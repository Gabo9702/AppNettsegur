package com.cgro.appnettsegur.ui.compartido

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cgro.appnettsegur.AppNettsegur
import com.cgro.appnettsegur.data.model.NovedadRemota
import com.cgro.appnettsegur.data.repository.ProyectosRepository
import com.cgro.appnettsegur.navigation.Rutas
import com.cgro.appnettsegur.util.ConectividadMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

data class NovedadRemotaUi(
    val cargando: Boolean = true,
    val novedad: NovedadRemota? = null,
)

/**
 * Detalle de una novedad sincronizada (M2, fase 1), vista por el cliente o el ingeniero y abierta
 * también desde la Bandeja de M3. Solo lectura y en línea; muestra datos, fotos (miniatura) y firmas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NovedadRemotaViewModel(
    proyectos: ProyectosRepository,
    conectividad: ConectividadMonitor,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val id: String = checkNotNull(savedStateHandle[Rutas.ARG_ID])

    val hayInternet: StateFlow<Boolean> = conectividad.hayInternet

    private val reintentos = MutableStateFlow(0)

    val estado: StateFlow<NovedadRemotaUi> = reintentos
        .flatMapLatest {
            proyectos.novedad(id)
                .map { NovedadRemotaUi(cargando = false, novedad = it) }
                .onStart { emit(NovedadRemotaUi(cargando = true)) }
        }
        .catch { emit(NovedadRemotaUi(cargando = false)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NovedadRemotaUi())

    fun reintentar() {
        reintentos.value++
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppNettsegur
                NovedadRemotaViewModel(
                    app.container.proyectosRepository,
                    app.container.conectividad,
                    createSavedStateHandle(),
                )
            }
        }
    }
}
