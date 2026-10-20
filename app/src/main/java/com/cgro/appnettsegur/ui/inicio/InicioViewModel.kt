package com.cgro.appnettsegur.ui.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cgro.appnettsegur.AppNettsegur
import com.cgro.appnettsegur.data.local.Conteos
import com.cgro.appnettsegur.data.prefs.PerfilTecnicoStore
import com.cgro.appnettsegur.data.repository.NovedadRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class InicioUiState(
    val cargando: Boolean = true,
    val nombreTecnico: String = "",
    val conteos: Conteos = Conteos(0, 0, 0, 0),
)

class InicioViewModel(repositorio: NovedadRepository, perfil: PerfilTecnicoStore) : ViewModel() {

    // Los contadores salen de un Flow de Room: cambian solos al guardar o sincronizar.
    val estado: StateFlow<InicioUiState> =
        combine(repositorio.observarConteos(), perfil.nombre) { conteos, nombre ->
            InicioUiState(cargando = false, nombreTecnico = nombre, conteos = conteos)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppNettsegur
                InicioViewModel(app.container.novedadRepository, app.container.perfilStore)
            }
        }
    }
}
