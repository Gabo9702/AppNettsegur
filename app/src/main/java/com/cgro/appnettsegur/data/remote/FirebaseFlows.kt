package com.cgro.appnettsegur.data.remote

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Utilidades para leer Firebase Realtime Database como Flows (M1, fase C).
 *
 * NO se activa setPersistenceEnabled(true): la fuente de verdad local sigue siendo Room para el
 * técnico; los demás roles trabajan en línea (MODULOS.md, secciones 3 y 5.3).
 */
object FirebaseFlows {

    /** false si falta google-services.json (Firebase no se inicializó). */
    fun configurado(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    fun raiz(): DatabaseReference = FirebaseDatabase.getInstance().reference

    fun ref(path: String): DatabaseReference = raiz().child(path)

    /**
     * Flow genérico sobre un ValueEventListener: emite cada vez que cambian los datos y QUITA el
     * listener cuando el flujo se cierra (awaitClose), para no filtrar listeners.
     */
    fun <T> flujoValor(query: Query, transformar: (DataSnapshot) -> T): Flow<T> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(transformar(snapshot))
            }

            override fun onCancelled(error: DatabaseError) {
                // Al cerrar sesión, los listeners activos reciben PERMISSION_DENIED.
                // Cerramos el flujo SIN excepción: si el colector corre en el hilo
                // principal sin .catch, propagar el error lo tumbaría. La UI ya
                // gestiona los estados vacío / "sin conexión".
                close()
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    /** Flow con las llaves hijas de un nodo (p. ej. los ID de un índice). */
    fun flujoClaves(path: String): Flow<List<String>> =
        flujoValor(ref(path)) { snap -> snap.children.mapNotNull { it.key } }
}
