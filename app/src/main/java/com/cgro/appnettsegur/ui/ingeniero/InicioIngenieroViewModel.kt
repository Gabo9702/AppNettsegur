package com.cgro.appnettsegur.ui.ingeniero

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cgro.appnettsegur.AppNettsegur
import com.cgro.appnettsegur.data.model.Proyecto
import com.cgro.appnettsegur.data.repository.ProyectosRepository
import com.cgro.appnettsegur.data.repository.SesionRepository
import com.cgro.appnettsegur.util.ConectividadMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

data class InicioIngenieroUi(
    val cargando: Boolean = true,
    val proyectos: List<Proyecto> = emptyList(),
)

/**
 * Inicio del ingeniero (M2, fase 2): los proyectos a su cargo, leídos con el índice seguro
 * proyectosPorIngeniero. Trabaja en línea (MODULOS.md, sección 3).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InicioIngenieroViewModel(
    sesion: SesionRepository,
    proyectos: ProyectosRepository,
    conectividad: ConectividadMonitor,
) : ViewModel() {

    val hayInternet: StateFlow<Boolean> = conectividad.hayInternet

    private val reintentos = MutableStateFlow(0)

    val estado: StateFlow<InicioIngenieroUi> = reintentos
        .flatMapLatest {
            sesion.usuario.flatMapLatest { usuario ->
                if (usuario == null) {
                    flowOf(InicioIngenieroUi(cargando = false))
                } else {
                    proyectos.proyectosDelIngeniero(usuario.uid)
                        .map { InicioIngenieroUi(cargando = false, proyectos = it) }
                        .onStart { emit(InicioIngenieroUi(cargando = true)) }
                }
            }
        }
        .catch { emit(InicioIngenieroUi(cargando = false)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioIngenieroUi())

    fun reintentar() {
        reintentos.value++
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppNettsegur
                InicioIngenieroViewModel(
                    app.container.sesionRepository,
                    app.container.proyectosRepository,
                    app.container.conectividad,
                )
            }
        }
    }
}
