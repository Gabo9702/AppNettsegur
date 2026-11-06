package com.cgro.appnettsegur.notificaciones

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.notificacionesDataStore by preferencesDataStore(name = "notificaciones")

/**
 * Preferencias locales de las notificaciones (M3, fase 2):
 *
 * - Registro de "ya mostradas": IDs de notificaciones que ya se avisaron (snackbar o aviso del
 *   sistema), para NO repetir el mismo aviso. Se limpia al cerrar sesión.
 * - Si ya se pidió el permiso POST_NOTIFICATIONS, para no insistir con la explicación.
 */
class NotificacionesStore(private val context: Context) {

    private val claveMostradas = stringSetPreferencesKey("ya_mostradas")
    private val claveVencimientosRh = stringSetPreferencesKey("vencimientos_rh_mostrados")
    private val clavePermisoPreguntado = booleanPreferencesKey("permiso_preguntado")

    suspend fun yaMostradas(): Set<String> =
        context.notificacionesDataStore.data.first()[claveMostradas].orEmpty()

    /** Agrega IDs al registro de ya mostradas (y conserva, opcionalmente, solo los vigentes). */
    suspend fun marcarMostradas(ids: Collection<String>, vigentes: Set<String>? = null) {
        if (ids.isEmpty() && vigentes == null) return
        context.notificacionesDataStore.edit { p ->
            val actual = p[claveMostradas].orEmpty()
            var nuevo = actual + ids
            // Poda: si se conoce el conjunto vigente, no guardamos IDs que ya no existen.
            if (vigentes != null) nuevo = nuevo.intersect(vigentes + ids)
            p[claveMostradas] = nuevo
        }
    }

    // ---- Alertas de vencimiento de RH (fase 4): se dedup por día con una clave que incluye hoy ----

    suspend fun vencimientosRhMostrados(): Set<String> =
        context.notificacionesDataStore.data.first()[claveVencimientosRh].orEmpty()

    suspend fun marcarVencimientosRh(claves: Collection<String>) {
        if (claves.isEmpty()) return
        context.notificacionesDataStore.edit { p ->
            // Solo se conservan las de hoy: las claves llevan la fecha, así el set no crece sin fin.
            p[claveVencimientosRh] = p[claveVencimientosRh].orEmpty() + claves
        }
    }

    /** Se limpia al cerrar sesión (MODULOS.md, fase 2 de M3): ambos registros locales. */
    suspend fun limpiarMostradas() {
        context.notificacionesDataStore.edit {
            it.remove(claveMostradas)
            it.remove(claveVencimientosRh)
        }
    }

    suspend fun permisoPreguntado(): Boolean =
        context.notificacionesDataStore.data.first()[clavePermisoPreguntado] ?: false

    suspend fun marcarPermisoPreguntado() {
        context.notificacionesDataStore.edit { it[clavePermisoPreguntado] = true }
    }
}
