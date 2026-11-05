package com.cgro.appnettsegur.ui.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cgro.appnettsegur.AppNettsegur
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.data.local.Conteos
import com.cgro.appnettsegur.data.repository.NovedadRepository
import com.cgro.appnettsegur.data.repository.SesionRepository
import com.cgro.appnettsegur.util.ConectividadMonitor
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InicioUiState(
    val cargando: Boolean = true,
    val nombreTecnico: String = "",
    val conteos: Conteos = Conteos(0, 0, 0, 0),
)

class InicioViewModel(
    private val repositorio: NovedadRepository,
    private val sesion: SesionRepository,
    private val conectividad: ConectividadMonitor,
) : ViewModel() {

    // El nombre del técnico sale de la sesión (sección 5.1), ya no de la pantalla de perfil.
    // Los contadores salen de un Flow de Room: cambian solos al guardar o sincronizar.
    private val base: StateFlow<InicioUiState> =
        combine(repositorio.observarConteos(), sesion.usuario) { conteos, usuario ->
            InicioUiState(cargando = false, nombreTecnico = usuario?.nombre.orEmpty(), conteos = conteos)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioUiState())

    val estado: StateFlow<InicioUiState> = base

    private val _mensajes = Channel<String>(Channel.BUFFERED)
    val mensajes = _mensajes.receiveAsFlow()

    /** Botón "Actualizar proyectos": descarga del índice los proyectos asignados (sección 5.2). */
    fun actualizarProyectos() {
        viewModelScope.launch {
            if (!conectividad.hayInternet.value) {
                _mensajes.send(Mensajes.PROYECTOS_SIN_CONEXION)
                return@launch
            }
            val uid = sesion.usuario.value?.uid ?: return@launch
            repositorio.actualizarProyectosDelTecnico(uid).fold(
                onSuccess = { n -> _mensajes.send(Mensajes.proyectosActualizados(n)) },
                onFailure = { _mensajes.send(Mensajes.PROYECTOS_ERROR) },
            )
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppNettsegur
                InicioViewModel(
                    app.container.novedadRepository,
                    app.container.sesionRepository,
                    app.container.conectividad,
                )
            }
        }
    }
}
