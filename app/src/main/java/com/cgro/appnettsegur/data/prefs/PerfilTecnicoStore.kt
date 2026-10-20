package com.cgro.appnettsegur.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.perfilDataStore by preferencesDataStore(name = "perfil_tecnico")

/** Perfil del técnico guardado en DataStore. Hoy solo guarda el nombre. */
class PerfilTecnicoStore(private val context: Context) {

    private val claveNombre = stringPreferencesKey("nombre_tecnico")

    /** Nombre del técnico, o cadena vacía si todavía no se ha configurado. */
    val nombre: Flow<String> = context.perfilDataStore.data.map { it[claveNombre].orEmpty() }

    suspend fun obtenerNombre(): String = nombre.first()

    suspend fun guardarNombre(nombre: String) {
        context.perfilDataStore.edit { it[claveNombre] = nombre.trim() }
    }
}
