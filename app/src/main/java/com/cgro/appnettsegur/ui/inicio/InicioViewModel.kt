package com.cgro.appnettsegur.ui.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cgro.appnettsegur.AppNettsegur
import com.cgro.appnettsegur.data.local.Conteos
import com.cgro.appnettsegur.data.repository.NovedadRepository
import com.cgro.appnettsegur.data.repository.SesionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class InicioUiState(
    val cargando: Boolean = true,
    val nombreTecnico: String = "",
    val conteos: Conteos = Conteos(0, 0, 0, 0),
)

class InicioViewModel(repositorio: NovedadRepository, sesion: SesionRepository) : ViewModel() {

    // El nombre del técnico sale de la sesión (sección 5.1), ya no de la pantalla de perfil.
    // Los contadores salen de un Flow de Room: cambian solos al guardar o sincronizar.
    val estado: StateFlow<InicioUiState> =
        combine(repositorio.observarConteos(), sesion.usuario) { conteos, usuario ->
            InicioUiState(cargando = false, nombreTecnico = usuario?.nombre.orEmpty(), conteos = conteos)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppNettsegur
                InicioViewModel(app.container.novedadRepository, app.container.sesionRepository)
            }
        }
    }
}
