package com.cgro.appnettsegur.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cgro.appnettsegur.config.Rol
import com.cgro.appnettsegur.data.model.Usuario
import kotlinx.coroutines.flow.first

private val Context.sesionDataStore by preferencesDataStore(name = "sesion")

/**
 * Caché local de la sesión (MODULOS.md, sección 5.1): uid, nombre, correo, rol y empleadoId.
 *
 * Permite abrir la app SIN Internet cuando ya se inició sesión antes: si FirebaseAuth tiene
 * usuario y aquí hay datos del mismo uid, se entra directo al inicio del rol.
 */
class SesionStore(private val context: Context) {

    private val claveUid = stringPreferencesKey("uid")
    private val claveNombre = stringPreferencesKey("nombre")
    private val claveCorreo = stringPreferencesKey("correo")
    private val claveRol = stringPreferencesKey("rol")
    private val claveEmpleadoId = stringPreferencesKey("empleadoId")

    /** Usuario guardado en caché, o null si no hay sesión guardada o le falta el rol. */
    suspend fun leer(): Usuario? {
        val p = context.sesionDataStore.data.first()
        val uid = p[claveUid]?.takeIf { it.isNotBlank() } ?: return null
        val rol = Rol.desde(p[claveRol]) ?: return null
        return Usuario(
            uid = uid,
            nombre = p[claveNombre].orEmpty(),
            correo = p[claveCorreo].orEmpty(),
            rol = rol,
            empleadoId = p[claveEmpleadoId]?.takeIf { it.isNotBlank() },
        )
    }

    suspend fun guardar(usuario: Usuario) {
        context.sesionDataStore.edit { p ->
            p[claveUid] = usuario.uid
            p[claveNombre] = usuario.nombre
            p[claveCorreo] = usuario.correo
            p[claveRol] = usuario.rol.name
            if (usuario.empleadoId != null) p[claveEmpleadoId] = usuario.empleadoId else p.remove(claveEmpleadoId)
        }
    }

    suspend fun limpiar() {
        context.sesionDataStore.edit { it.clear() }
    }
}
