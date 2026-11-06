package com.cgro.appnettsegur.ui.notificaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cgro.appnettsegur.AppNettsegur
import com.cgro.appnettsegur.data.repository.NotificacionesRepository
import com.cgro.appnettsegur.data.repository.SesionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * Contador de notificaciones sin leer para la campana (M3, fase 1).
 *
 * Sigue al usuario de la sesión: si cambia (o cierra sesión), se reengancha al nodo del nuevo uid.
 */
class CampanaViewModel(
    private val notificaciones: NotificacionesRepository,
    sesion: SesionRepository,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val noLeidas: StateFlow<Int> = sesion.usuario
        .flatMapLatest { usuario ->
            if (usuario == null) flowOf(0) else notificaciones.noLeidas(usuario.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppNettsegur
                CampanaViewModel(
                    app.container.notificacionesRepository,
                    app.container.sesionRepository,
                )
            }
        }
    }
}
