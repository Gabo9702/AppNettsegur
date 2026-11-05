package com.cgro.appnettsegur.data.repository

import android.content.Context
import com.cgro.appnettsegur.config.Limites
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.config.RutasFirebase
import com.cgro.appnettsegur.data.model.Carta
import com.cgro.appnettsegur.data.model.Empleado
import com.cgro.appnettsegur.data.remote.FirebaseFlows
import com.cgro.appnettsegur.data.remote.aCarta
import com.cgro.appnettsegur.data.remote.texto
import com.cgro.appnettsegur.notificaciones.NotificacionFactory
import com.cgro.appnettsegur.util.ArchivoUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Cartas de presentación (M1, fase C). El PDF lo genera el analista en el dispositivo (M2) y se
 * sube en Base64. Se listan por el índice cartasPorProyecto (las reglas protegen cada carta por nodo).
 */
class CartasRepositoryImpl(private val context: Context) : CartasRepository {

    private val configurado get() = FirebaseFlows.configurado(context)

    override fun cartasDelProyecto(proyectoId: String): Flow<List<Carta>> {
        if (!configurado) return flowOf(emptyList())
        return FirebaseFlows.flujoClaves(RutasFirebase.cartasPorProyecto(proyectoId)).map { ids ->
            ids.mapNotNull { leerCarta(it) }
        }
    }

    /** Guarda la carta (PDF) y notifica al cliente del proyecto en UNA escritura multi-ruta (CU-09). */
    override suspend fun guardarCarta(proyectoId: String, empleados: List<Empleado>, pdf: File): Result<Carta> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!configurado) throw IllegalStateException(Mensajes.FIREBASE_NO_CONFIGURADO)
                val generadaPor = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw IllegalStateException(Mensajes.FIREBASE_NO_CONFIGURADO)

                val bytes = pdf.readBytes()
                require(bytes.size <= Limites.MAX_ARCHIVO_BYTES) { Mensajes.ARCHIVO_MUY_GRANDE }

                val cartaId = UUID.randomUUID().toString()
                val archivoId = UUID.randomUUID().toString()
                val ahora = System.currentTimeMillis()
                val empleadoIds = empleados.map { it.empleadoId }

                val carta = Carta(
                    cartaId = cartaId,
                    proyectoId = proyectoId,
                    empleadoIds = empleadoIds,
                    archivoId = archivoId,
                    generadaPor = generadaPor,
                    fecha = ahora,
                )

                val clienteUid = FirebaseFlows.ref(RutasFirebase.proyecto(proyectoId)).get().await()
                    .texto("clienteUid")

                val updates = buildMap<String, Any?> {
                    put(RutasFirebase.carta(cartaId), mapOf(
                        "proyectoId" to proyectoId,
                        "empleados" to empleadoIds.associateWith { true },
                        "archivoId" to archivoId,
                        "generadaPor" to generadaPor,
                        "fecha" to ahora,
                    ))
                    put(RutasFirebase.archivo(archivoId), mapOf(
                        "base64" to ArchivoUtils.aBase64(bytes),
                        "mime" to "application/pdf",
                        "nombre" to (pdf.name),
                        "tamanoBytes" to bytes.size.toLong(),
                        "subidoPor" to generadaPor,
                        "fecha" to ahora,
                    ))
                    put(RutasFirebase.cartaPorProyecto(proyectoId, cartaId), true)
                    if (clienteUid.isNotBlank()) {
                        putAll(NotificacionFactory.cartaNueva(clienteUid, proyectoId, carta))
                    }
                }
                FirebaseFlows.raiz().updateChildren(updates).await()
                carta
            }
        }

    private suspend fun leerCarta(id: String): Carta? =
        runCatching { FirebaseFlows.ref(RutasFirebase.carta(id)).get().await().aCarta() }.getOrNull()
}
