package com.cgro.appnettsegur.ui.detalle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cgro.appnettsegur.AppNettsegur
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.data.local.EstadoSync
import com.cgro.appnettsegur.data.local.EvidenciaEntity
import com.cgro.appnettsegur.data.local.NovedadEntity
import com.cgro.appnettsegur.data.local.ProyectoEntity
import com.cgro.appnettsegur.data.repository.NovedadRepository
import com.cgro.appnettsegur.navigation.Rutas
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetalleUiState(
    val cargando: Boolean = true,
    val novedad: NovedadEntity? = null,
    val evidencias: List<EvidenciaEntity> = emptyList(),
    val nombreProyecto: String? = null,
) {
    val puedeEditar: Boolean get() = novedad != null && novedad.estadoSync != EstadoSync.SINCRONIZADO
    val puedeEliminar: Boolean get() = puedeEditar
    val puedeReintentar: Boolean get() = novedad?.estadoSync == EstadoSync.ERROR
}

sealed interface EventoDetalle {
    data object Eliminado : EventoDetalle
    data class Mensaje(val texto: String) : EventoDetalle
}

class DetalleViewModel(
    private val repositorio: NovedadRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val id: String = checkNotNull(savedStateHandle[Rutas.ARG_ID])

    private val _eventos = Channel<EventoDetalle>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    // Se observa Room: si la sincronización cambia el estado, el detalle se actualiza solo.
    val estado: StateFlow<DetalleUiState> = combine(
        repositorio.observarNovedad(id),
        repositorio.observarEvidencias(id),
        repositorio.observarProyectos(),
    ) { novedad, evidencias, proyectos: List<ProyectoEntity> ->
        DetalleUiState(
            cargando = false,
            novedad = novedad,
            evidencias = evidencias,
            nombreProyecto = proyectos.firstOrNull { it.proyectoId == novedad?.proyectoId }?.nombreProyecto,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUiState())

    fun eliminar() {
        viewModelScope.launch {
            if (repositorio.eliminar(id)) {
                _eventos.send(EventoDetalle.Eliminado)
            } else {
                _eventos.send(EventoDetalle.Mensaje(Mensajes.SINCRONIZADO_NO_EDITABLE))
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppNettsegur
                DetalleViewModel(app.container.novedadRepository, createSavedStateHandle())
            }
        }
    }
}
