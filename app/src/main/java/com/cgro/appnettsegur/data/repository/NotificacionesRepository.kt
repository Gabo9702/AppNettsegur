package com.cgro.appnettsegur.data.repository

import android.content.Context
import com.cgro.appnettsegur.config.Limites
import com.cgro.appnettsegur.config.RutasFirebase
import com.cgro.appnettsegur.data.model.Notificacion
import com.cgro.appnettsegur.data.model.TipoNotificacion
import com.cgro.appnettsegur.data.remote.FirebaseFlows
import com.cgro.appnettsegur.notificaciones.RegistroMostradas
import com.google.firebase.database.DataSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Lectura de las notificaciones del destinatario (M3). La ESCRITURA de notificaciones la hace M1
 * (NotificacionFactory + escrituras multi-ruta): aquí solo se RECIBEN y se marca `leida`
 * (MODULOS.md, secciones 4.1 y 7).
 *
 * Las reglas de seguridad (sección 4.1) dejan al destinatario leer notificaciones/{uid} y cambiar
 * SOLO el campo `leida`. Por eso marcar como leída escribe únicamente .../{id}/leida.
 */
interface NotificacionesRepository {
    /** Últimas notificaciones del usuario, de la más reciente a la más antigua (máx. 50). */
    fun observar(uid: String): Flow<List<Notificacion>>

    /** Cantidad de notificaciones sin leer (para el contador de la campana). */
    fun noLeidas(uid: String): Flow<Int>

    /** Lectura puntual (sin listener): la usa el worker periódico con la app cerrada (fase 3). */
    suspend fun leerUnaVez(uid: String): List<Notificacion>

    /** Marca una notificación como leída (solo escribe el campo `leida`). */
    suspend fun marcarLeida(uid: String, notificacionId: String): Result<Unit>

    /** Marca como leídas todas las no leídas, en una sola escritura multi-ruta. */
    suspend fun marcarTodasLeidas(uid: String): Result<Unit>
}

class NotificacionesRepositoryImpl(private val context: Context) : NotificacionesRepository {

    /** false si falta google-services.json (Firebase no se inicializó): la app no debe reventar. */
    private val configurado get() = FirebaseFlows.configurado(context)

    override fun observar(uid: String): Flow<List<Notificacion>> {
        if (!configurado) return flowOf(emptyList())
        // Se ordena por `fecha` en el servidor (.indexOn: ["fecha"]) y se traen solo las últimas 50.
        val query = FirebaseFlows.ref(RutasFirebase.notificaciones(uid))
            .orderByChild("fecha")
            .limitToLast(Limites.MAX_NOTIFICACIONES)
        return FirebaseFlows.flujoValor(query) { snap ->
            snap.children.mapNotNull { it.aNotificacion() }.sortedByDescending { it.fecha }
        }
    }

    override fun noLeidas(uid: String): Flow<Int> {
        if (!configurado) return flowOf(0)
        // Se cuenta sobre el nodo completo: el contador debe reflejar todas las no leídas, no solo 50.
        // Se usa la misma función pura que la prueba unitaria (RegistroMostradas.conteoNoLeidas).
        return FirebaseFlows.flujoValor(FirebaseFlows.ref(RutasFirebase.notificaciones(uid))) { snap ->
            RegistroMostradas.conteoNoLeidas(snap.children.mapNotNull { it.aNotificacion() })
        }
    }

    override suspend fun leerUnaVez(uid: String): List<Notificacion> =
        withContext(Dispatchers.IO) {
            if (!configurado) return@withContext emptyList()
            runCatching {
                val snap = FirebaseFlows.ref(RutasFirebase.notificaciones(uid)).get().await()
                snap.children.mapNotNull { it.aNotificacion() }.sortedByDescending { it.fecha }
            }.getOrDefault(emptyList())
        }

    override suspend fun marcarLeida(uid: String, notificacionId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireConfigurado()
                FirebaseFlows.ref(RutasFirebase.notificacion(uid, notificacionId))
                    .child("leida").setValue(true).await()
                Unit
            }
        }

    override suspend fun marcarTodasLeidas(uid: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireConfigurado()
                val nodo = FirebaseFlows.ref(RutasFirebase.notificaciones(uid))
                val snap = nodo.get().await()
                // Una sola escritura multi-ruta: {id}/leida = true solo para las que estaban sin leer.
                val updates = snap.children
                    .filter { it.child("leida").getValue(Boolean::class.java) != true }
                    .mapNotNull { it.key }
                    .associate { "$it/leida" to true as Any? }
                if (updates.isNotEmpty()) nodo.updateChildren(updates).await()
                Unit
            }
        }

    private fun requireConfigurado() {
        if (!configurado) error("Firebase no está configurado")
    }

    /** DataSnapshot → Notificacion. A mano, porque TipoNotificacion es un enum. */
    private fun DataSnapshot.aNotificacion(): Notificacion? {
        val id = key ?: return null
        val tipo = runCatching {
            TipoNotificacion.valueOf(child("tipo").getValue(String::class.java).orEmpty())
        }.getOrNull() ?: return null
        return Notificacion(
            notificacionId = id,
            tipo = tipo,
            titulo = child("titulo").getValue(String::class.java).orEmpty(),
            mensaje = child("mensaje").getValue(String::class.java).orEmpty(),
            proyectoId = child("proyectoId").getValue(String::class.java).orEmpty(),
            refId = child("refId").getValue(String::class.java).orEmpty(),
            fecha = child("fecha").getValue(Long::class.java) ?: 0L,
            leida = child("leida").getValue(Boolean::class.java) ?: false,
        )
    }
}
