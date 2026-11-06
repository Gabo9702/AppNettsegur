package com.cgro.appnettsegur.data.remote

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/**
 * Única clase que escribe la sincronización del técnico en Firebase Realtime Database.
 * Solo se usa desde SyncRepository, es decir, solo cuando el usuario pulsa SINCRONIZAR.
 *
 * NO se activa setPersistenceEnabled(true): no queremos una caché offline de Firebase,
 * la fuente de verdad local es Room.
 */
class FirebaseNovedadesDataSource(private val context: Context) {

    /** false si falta app/google-services.json (Firebase no se inicializó). */
    fun estaConfigurado(): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    /**
     * Escribe el registro con updateChildren multi-ruta (MODULOS.md, sección 5.3): en UNA sola
     * operación ATÓMICA van la novedad, su índice y las notificaciones. Como todas las llaves son
     * fijas (idRegistro, nov_{id}), reintentar sobrescribe los mismos nodos y NO crea duplicados.
     *
     * await() se completa cuando el servidor confirma la escritura. Ojo: si no hay conexión, el
     * SDK de RTDB NO devuelve error, sino que ENCOLA la escritura y la tarea queda esperando. Por
     * eso SyncRepository verifica la conexión antes y envuelve esta llamada en un timeout.
     */
    suspend fun escribirMultiRuta(updates: Map<String, Any?>) {
        FirebaseDatabase.getInstance().reference.updateChildren(updates).await()
    }
}
