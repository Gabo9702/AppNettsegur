package com.cgro.appnettsegur.data.repository

import android.content.Context
import com.cgro.appnettsegur.config.RutasFirebase
import com.cgro.appnettsegur.data.model.Empleado
import com.cgro.appnettsegur.data.model.NovedadRemota
import com.cgro.appnettsegur.data.model.PersonalAsignado
import com.cgro.appnettsegur.data.model.Proyecto
import com.cgro.appnettsegur.data.remote.FirebaseFlows
import com.cgro.appnettsegur.data.remote.aNovedadRemota
import com.cgro.appnettsegur.data.remote.aPersonalAsignado
import com.cgro.appnettsegur.data.remote.aProyecto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.cgro.appnettsegur.config.Mensajes
import java.time.LocalDate

/**
 * Proyectos leídos en línea de RTDB (M1, fase C).
 *
 * Las listas se obtienen con el patrón de ÍNDICE (no con un query sobre /proyectos), porque las
 * reglas protegen cada proyecto por nodo: leer el árbol completo dejaría que un cliente viera
 * proyectos ajenos. Por eso M1 agrega indices/proyectosPorCliente y .../proyectosPorIngeniero.
 */
class ProyectosRepositoryImpl(private val context: Context) : ProyectosRepository {

    private val configurado get() = FirebaseFlows.configurado(context)

    override fun proyectosDelCliente(uid: String): Flow<List<Proyecto>> =
        proyectosDesdeIndice(RutasFirebase.proyectosPorCliente(uid))

    override fun proyectosDelIngeniero(uid: String): Flow<List<Proyecto>> =
        proyectosDesdeIndice(RutasFirebase.proyectosPorIngeniero(uid))

    override fun proyecto(id: String): Flow<Proyecto?> {
        if (!configurado) return flowOf(null)
        return FirebaseFlows.flujoValor(FirebaseFlows.ref(RutasFirebase.proyecto(id))) { it.aProyecto() }
    }

    override fun novedadesDelProyecto(id: String): Flow<List<NovedadRemota>> {
        if (!configurado) return flowOf(emptyList())
        // El índice dice qué novedades pertenecen al proyecto; cada una se lee por su nodo.
        return FirebaseFlows.flujoClaves(RutasFirebase.novedadesPorProyecto(id)).map { ids ->
            ids.mapNotNull { leerNovedad(it) }
        }
    }

    override fun proyectosParaAnalista(): Flow<List<Proyecto>> {
        if (!configurado) return flowOf(emptyList())
        // El analista sí puede leer la colección /proyectos (regla aditiva de M1), así que se lee
        // completa y se mapea cada hijo. No se usa un índice por rol porque no existe uno de analista.
        return FirebaseFlows.flujoValor(FirebaseFlows.ref(RutasFirebase.PROYECTOS)) { snap ->
            snap.children.mapNotNull { it.aProyecto() }.sortedBy { it.nombreProyecto.lowercase() }
        }
    }

    override fun novedad(idRegistro: String): Flow<NovedadRemota?> {
        if (!configurado) return flowOf(null)
        // Se lee el nodo novedades/{id} y se convierte con el mapeo que ya usa M1 (aNovedadRemota).
        return FirebaseFlows.flujoValor(FirebaseFlows.ref(RutasFirebase.novedad(idRegistro))) {
            if (it.exists()) it.aNovedadRemota() else null
        }
    }

    override fun personalDelProyecto(id: String): Flow<List<PersonalAsignado>> {
        if (!configurado) return flowOf(emptyList())
        return FirebaseFlows.flujoValor(FirebaseFlows.ref(RutasFirebase.personalProyecto(id))) { snap ->
            snap.children.mapNotNull { it.aPersonalAsignado() }
        }
    }

    /** Marca/desmarca un hito y recalcula el avance en UNA escritura multi-ruta (CU-05, HU-12). */
    override suspend fun marcarHito(proyectoId: String, hitoId: String, completado: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireConfigurado()
                val hitosSnap = FirebaseFlows.ref(RutasFirebase.hitos(proyectoId)).get().await()
                val total = hitosSnap.childrenCount.toInt().coerceAtLeast(1)
                // Avance = porcentaje de hitos completados (con el cambio aplicado).
                val completados = hitosSnap.children.count { hijo ->
                    if (hijo.key == hitoId) completado else hijo.child("completado").getValue(Boolean::class.java) == true
                }
                val avance = (completados * 100) / total
                val updates = mapOf(
                    "${RutasFirebase.hito(proyectoId, hitoId)}/completado" to completado,
                    RutasFirebase.avanceProyecto(proyectoId) to avance,
                )
                FirebaseFlows.raiz().updateChildren(updates).await()
                Unit
            }
        }

    /**
     * Asigna/quita un técnico actualizando en UNA escritura multi-ruta: el nodo tecnicos del
     * proyecto, la vista reducida de personal (que el cliente lee) y el índice proyectosPorTecnico.
     */
    override suspend fun asignarTecnico(proyectoId: String, empleado: Empleado, asignar: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireConfigurado()
                val uidTecnico = empleado.uid ?: throw IllegalStateException(Mensajes.EMPLEADO_SIN_USUARIO)
                val personal: Any? = if (asignar) {
                    mapOf(
                        "nombre" to empleado.nombre,
                        "cedula" to empleado.cedula,
                        "estadoCumplimiento" to empleado.estadoCumplimiento(LocalDate.now()).name,
                    )
                } else null
                val updates = mapOf(
                    RutasFirebase.tecnicoProyecto(proyectoId, uidTecnico) to (if (asignar) true else null),
                    RutasFirebase.personalProyectoEmpleado(proyectoId, empleado.empleadoId) to personal,
                    RutasFirebase.proyectoPorTecnico(uidTecnico, proyectoId) to (if (asignar) true else null),
                )
                FirebaseFlows.raiz().updateChildren(updates).await()
                Unit
            }
        }

    // ---- Apoyos ----

    private fun proyectosDesdeIndice(indicePath: String): Flow<List<Proyecto>> {
        if (!configurado) return flowOf(emptyList())
        return FirebaseFlows.flujoClaves(indicePath).map { ids -> ids.mapNotNull { leerProyecto(it) } }
    }

    private suspend fun leerProyecto(id: String): Proyecto? =
        runCatching { FirebaseFlows.ref(RutasFirebase.proyecto(id)).get().await().aProyecto() }.getOrNull()

    private suspend fun leerNovedad(id: String): NovedadRemota? =
        runCatching { FirebaseFlows.ref(RutasFirebase.novedad(id)).get().await().aNovedadRemota() }.getOrNull()

    private fun requireConfigurado() {
        if (!configurado) throw IllegalStateException(Mensajes.FIREBASE_NO_CONFIGURADO)
    }
}
