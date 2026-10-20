package com.cgro.appnettsegur.data.remote

import android.content.Context
import com.cgro.appnettsegur.config.Limites
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.tasks.await

/**
 * Única clase que escribe en Firebase Realtime Database.
 * Solo se usa desde SyncRepository, es decir, solo cuando el usuario pulsa SINCRONIZAR.
 *
 * NO se activa setPersistenceEnabled(true): no queremos una caché offline de Firebase,
 * la fuente de verdad local es Room.
 */
class FirebaseNovedadesDataSource(private val context: Context) {

    /** false si falta app/google-services.json (Firebase no se inicializó). */
    fun estaConfigurado(): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    /**
     * Escribe el registro en novedades/{idRegistro} con setValue (NUNCA push()).
     * Como la llave es el idRegistro, reintentar sobrescribe el mismo nodo y no crea duplicados.
     *
     * await() se completa cuando el servidor confirma la escritura. Ojo: si no hay conexión,
     * el SDK de RTDB NO devuelve error, sino que ENCOLA la escritura y la tarea queda esperando.
     * Por eso SyncRepository verifica la conexión antes y envuelve esta llamada en un timeout.
     */
    suspend fun escribir(idRegistro: String, datos: Map<String, Any?>) {
        val mapa = datos + ("fechaSincronizacionServidor" to ServerValue.TIMESTAMP)
        FirebaseDatabase.getInstance().reference
            .child(Limites.NODO_FIREBASE)
            .child(idRegistro)
            .setValue(mapa)
            .await()
    }
}
