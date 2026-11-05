package com.cgro.appnettsegur.data.repository

import android.content.Context
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.config.Rol
import com.cgro.appnettsegur.config.RutasFirebase
import com.cgro.appnettsegur.data.model.Usuario
import com.cgro.appnettsegur.data.prefs.SesionStore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Sesión con Firebase Authentication (correo y contraseña) y caché local en DataStore
 * (MODULOS.md, sección 5.1).
 *
 * Todo acceso a Firebase va protegido por [configurado]: si falta google-services.json, la app
 * no inicializa Firebase, así que iniciar sesión devuelve un error legible en vez de reventar.
 */
class SesionRepositoryImpl(
    private val context: Context,
    private val store: SesionStore,
) : SesionRepository {

    private val _usuario = MutableStateFlow<Usuario?>(null)
    override val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    /** false si falta google-services.json (Firebase no se inicializó). */
    private val configurado: Boolean get() = FirebaseApp.getApps(context).isNotEmpty()

    /**
     * Restaura la sesión en caché al arrancar, SIN Internet: solo si FirebaseAuth ya tiene un
     * usuario (sesión persistida localmente por el SDK) y la caché corresponde a ese mismo uid.
     * Devuelve el usuario restaurado, o null si hay que mostrar el inicio de sesión.
     */
    suspend fun restaurarSesion(): Usuario? {
        if (!configurado) return null
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            _usuario.value = null
            return null
        }
        val enCache = store.leer()?.takeIf { it.uid == uid }
        _usuario.value = enCache
        return enCache
    }

    override suspend fun iniciarSesion(correo: String, clave: String): Result<Usuario> =
        withContext(Dispatchers.IO) {
            if (!configurado) return@withContext Result.failure(Exception(Mensajes.LOGIN_FIREBASE_NO_CONFIGURADO))
            try {
                val auth = FirebaseAuth.getInstance()
                val resultado = auth.signInWithEmailAndPassword(correo.trim(), clave).await()
                val uid = resultado.user?.uid
                    ?: return@withContext Result.failure(Exception(Mensajes.LOGIN_ERROR))

                // El rol vive en usuarios/{uid}/rol (no en custom claims, sección 2).
                val snapshot = FirebaseDatabase.getInstance().reference
                    .child(RutasFirebase.usuario(uid)).get().await()
                val usuario = aUsuario(uid, correo.trim(), snapshot)
                if (usuario == null) {
                    // Cuenta sin rol válido: no la dejamos iniciar sesión.
                    auth.signOut()
                    return@withContext Result.failure(Exception(Mensajes.LOGIN_SIN_ROL))
                }

                store.guardar(usuario)
                _usuario.value = usuario
                Result.success(usuario)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(Exception(mensajeLegible(e)))
            }
        }

    override suspend fun cerrarSesion() {
        if (configurado) runCatching { FirebaseAuth.getInstance().signOut() }
        store.limpiar()
        _usuario.value = null
    }

    /** Construye el usuario a partir de usuarios/{uid}; null si no tiene un rol válido. */
    private fun aUsuario(uid: String, correoAuth: String, snapshot: DataSnapshot): Usuario? {
        val rol = Rol.desde(snapshot.child("rol").getValue(String::class.java)) ?: return null
        return Usuario(
            uid = uid,
            nombre = snapshot.child("nombre").getValue(String::class.java).orEmpty(),
            correo = snapshot.child("correo").getValue(String::class.java) ?: correoAuth,
            rol = rol,
            empleadoId = snapshot.child("empleadoId").getValue(String::class.java)?.takeIf { it.isNotBlank() },
        )
    }

    /** Convierte la excepción de Firebase en un texto que el usuario entienda. */
    private fun mensajeLegible(e: Throwable): String = when (e) {
        is FirebaseAuthInvalidUserException, is FirebaseAuthInvalidCredentialsException -> Mensajes.LOGIN_CREDENCIALES
        is FirebaseNetworkException -> Mensajes.LOGIN_SIN_RED
        else -> e.message?.takeIf { it.isNotBlank() } ?: Mensajes.LOGIN_ERROR
    }
}
