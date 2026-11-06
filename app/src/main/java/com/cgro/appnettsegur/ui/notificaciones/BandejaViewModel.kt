package com.cgro.appnettsegur.ui.notificaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cgro.appnettsegur.AppNettsegur
import com.cgro.appnettsegur.data.model.Notificacion
import com.cgro.appnettsegur.data.repository.NotificacionesRepository
import com.cgro.appnettsegur.data.repository.SesionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BandejaUiState(
    val cargando: Boolean = true,
    val notificaciones: List<Notificacion> = emptyList(),
)

/**
 * Bandeja de notificaciones del usuario (M3, fase 1).
 *
 * La lista llega por Flow desde Firebase (se refresca sola). La AGRUPACIÓN por fecha la hace la
 * pantalla con AgrupadorFecha, para poder probarla aparte con un reloj fijo (fase 5).
 */
class BandejaViewModel(
    private val notificaciones: NotificacionesRepository,
    private val sesion: SesionRepository,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<BandejaUiState> = sesion.usuario
        .flatMapLatest { usuario ->
            if (usuario == null) flowOf(BandejaUiState(cargando = false, notificaciones = emptyList()))
            else notificaciones.observar(usuario.uid).map { BandejaUiState(cargando = false, notificaciones = it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BandejaUiState())

    /** Al tocar una notificación se marca como leída (antes de navegar al destino). */
    fun marcarLeida(notificacionId: String) {
        val uid = sesion.usuario.value?.uid ?: return
        viewModelScope.launch { notificaciones.marcarLeida(uid, notificacionId) }
    }

    fun marcarTodasLeidas() {
        val uid = sesion.usuario.value?.uid ?: return
        viewModelScope.launch { notificaciones.marcarTodasLeidas(uid) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppNettsegur
                BandejaViewModel(
                    app.container.notificacionesRepository,
                    app.container.sesionRepository,
                )
            }
        }
    }
}
