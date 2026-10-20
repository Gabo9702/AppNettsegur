package com.cgro.appnettsegur.ui.listado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cgro.appnettsegur.AppNettsegur
import com.cgro.appnettsegur.data.local.EstadoSync
import com.cgro.appnettsegur.data.local.NovedadEntity
import com.cgro.appnettsegur.data.repository.NovedadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ListadoUiState(
    val cargando: Boolean = true,
    val filtro: EstadoSync? = null, // null = todos
    val registros: List<NovedadEntity> = emptyList(),
    val totalSinFiltro: Int = 0,
)

class ListadoViewModel(repositorio: NovedadRepository) : ViewModel() {

    private val filtro = MutableStateFlow<EstadoSync?>(null)

    // El Flow de Room emite de nuevo cada vez que cambia la tabla: la lista se actualiza sola.
    val estado: StateFlow<ListadoUiState> =
        combine(repositorio.observarNovedades(), filtro) { todas, f ->
            ListadoUiState(
                cargando = false,
                filtro = f,
                registros = if (f == null) todas else todas.filter { it.estadoSync == f },
                totalSinFiltro = todas.size,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListadoUiState())

    fun filtrar(estado: EstadoSync?) {
        filtro.value = estado
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppNettsegur
                ListadoViewModel(app.container.novedadRepository)
            }
        }
    }
}
