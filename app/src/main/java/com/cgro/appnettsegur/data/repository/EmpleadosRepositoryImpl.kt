package com.cgro.appnettsegur.data.repository

import android.content.Context
import android.net.Uri
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.config.RutasFirebase
import com.cgro.appnettsegur.data.model.Empleado
import com.cgro.appnettsegur.data.model.TipoCumplimiento
import com.cgro.appnettsegur.data.remote.FirebaseFlows
import com.cgro.appnettsegur.data.remote.aEmpleado
import com.cgro.appnettsegur.util.ArchivoUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Empleados y cumplimiento (M1, fase C), para RH y analista. El estado y la habilitación NO se
 * guardan: los calcula el modelo [Empleado]. Los archivos (ARL, etc.) van a archivos/{id} en Base64.
 */
class EmpleadosRepositoryImpl(private val context: Context) : EmpleadosRepository {

    private val configurado get() = FirebaseFlows.configurado(context)

    override fun empleados(): Flow<List<Empleado>> {
        if (!configurado) return flowOf(emptyList())
        return FirebaseFlows.flujoValor(FirebaseFlows.ref(RutasFirebase.EMPLEADOS)) { snap ->
            snap.children.mapNotNull { it.aEmpleado() }.sortedBy { it.nombre }
        }
    }

    override fun empleado(id: String): Flow<Empleado?> {
        if (!configurado) return flowOf(null)
        return FirebaseFlows.flujoValor(FirebaseFlows.ref(RutasFirebase.empleado(id))) { it.aEmpleado() }
    }

    /** Crea o edita los datos básicos del empleado, sin tocar cumplimiento ni carné. */
    override suspend fun guardar(empleado: Empleado): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireConfigurado()
            val updates = mapOf(
                "${RutasFirebase.empleado(empleado.empleadoId)}/nombre" to empleado.nombre,
                "${RutasFirebase.empleado(empleado.empleadoId)}/cedula" to empleado.cedula,
                "${RutasFirebase.empleado(empleado.empleadoId)}/cargo" to empleado.cargo,
                "${RutasFirebase.empleado(empleado.empleadoId)}/uid" to empleado.uid,
            )
            FirebaseFlows.raiz().updateChildren(updates).await()
            Unit
        }
    }

    /** Sube un documento de cumplimiento (archivo + fechas) en UNA escritura multi-ruta. */
    override suspend fun subirCumplimiento(
        empleadoId: String,
        tipo: TipoCumplimiento,
        expedicion: LocalDate,
        vencimiento: LocalDate,
        uri: Uri,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val (archivoId, leido, subidoPor, ahora) = prepararArchivo(uri)
            val updates = mapOf(
                RutasFirebase.cumplimiento(empleadoId, tipo.name) to mapOf(
                    "fechaExpedicion" to expedicion.format(ISO),
                    "fechaVencimiento" to vencimiento.format(ISO),
                    "archivoId" to archivoId,
                ),
                RutasFirebase.archivo(archivoId) to archivoMapa(leido, subidoPor, ahora),
            )
            FirebaseFlows.raiz().updateChildren(updates).await()
            Unit
        }
    }

    /** Sube el carné del empleado (archivo) en UNA escritura multi-ruta. */
    override suspend fun subirCarne(empleadoId: String, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val (archivoId, leido, subidoPor, ahora) = prepararArchivo(uri)
            val updates = mapOf(
                RutasFirebase.carneEmpleado(empleadoId) to mapOf("archivoId" to archivoId, "fecha" to ahora),
                RutasFirebase.archivo(archivoId) to archivoMapa(leido, subidoPor, ahora),
            )
            FirebaseFlows.raiz().updateChildren(updates).await()
            Unit
        }
    }

    // ---- Apoyos ----

    private data class ArchivoPreparado(
        val archivoId: String,
        val leido: ArchivoUtils.ArchivoLeido,
        val subidoPor: String,
        val fecha: Long,
    )

    private suspend fun prepararArchivo(uri: Uri): ArchivoPreparado {
        requireConfigurado()
        val subidoPor = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException(Mensajes.FIREBASE_NO_CONFIGURADO)
        val leido = ArchivoUtils.leerYValidar(context, uri)
        return ArchivoPreparado(UUID.randomUUID().toString(), leido, subidoPor, System.currentTimeMillis())
    }

    private fun archivoMapa(leido: ArchivoUtils.ArchivoLeido, subidoPor: String, fecha: Long): Map<String, Any?> =
        mapOf(
            "base64" to ArchivoUtils.aBase64(leido.bytes),
            "mime" to leido.mime,
            "nombre" to leido.nombre,
            "tamanoBytes" to leido.bytes.size.toLong(),
            "subidoPor" to subidoPor,
            "fecha" to fecha,
        )

    private fun requireConfigurado() {
        if (!configurado) throw IllegalStateException(Mensajes.FIREBASE_NO_CONFIGURADO)
    }

    private companion object {
        val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
