package com.cgro.appnettsegur.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cgro.appnettsegur.AppNettsegur
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.config.Rol
import com.cgro.appnettsegur.data.repository.SesionRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(private val sesion: SesionRepository) : ViewModel() {

    data class UiState(
        val correo: String = "",
        val clave: String = "",
        val cargando: Boolean = false,
        val error: String? = null,
    )

    private val _estado = MutableStateFlow(UiState())
    val estado = _estado.asStateFlow()

    // Emite el rol del usuario cuando el inicio de sesión fue exitoso: la UI navega a su inicio.
    private val _logueado = Channel<Rol>(Channel.BUFFERED)
    val logueado = _logueado.receiveAsFlow()

    fun onCorreo(valor: String) = _estado.update { it.copy(correo = valor, error = null) }
    fun onClave(valor: String) = _estado.update { it.copy(clave = valor, error = null) }

    fun entrar() {
        val s = _estado.value
        if (s.cargando) return
        when {
            s.correo.isBlank() -> _estado.update { it.copy(error = Mensajes.LOGIN_CORREO_VACIO) }
            s.clave.isBlank() -> _estado.update { it.copy(error = Mensajes.LOGIN_CLAVE_VACIA) }
            else -> viewModelScope.launch {
                _estado.update { it.copy(cargando = true, error = null) }
                sesion.iniciarSesion(s.correo, s.clave).fold(
                    onSuccess = { usuario ->
                        _estado.update { it.copy(cargando = false) }
                        _logueado.send(usuario.rol)
                    },
                    onFailure = { e ->
                        _estado.update { it.copy(cargando = false, error = e.message ?: Mensajes.LOGIN_ERROR) }
                    },
                )
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppNettsegur
                LoginViewModel(app.container.sesionRepository)
            }
        }
    }
}
