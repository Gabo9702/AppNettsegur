package com.cgro.appnettsegur.ui.cliente

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

data class InicioClienteUi(
    val cargando: Boolean = true,
    val proyectos: List<Proyecto> = emptyList(),
)

/**
 * Inicio del cliente (M2, fase 1): "Mis proyectos".
 *
 * Trabaja en línea (MODULOS.md, sección 3). Los proyectos se leen con el índice seguro
 * proyectosPorCliente, así el cliente nunca ve proyectos ajenos. "Reintentar" vuelve a suscribirse.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InicioClienteViewModel(
    sesion: SesionRepository,
    proyectos: ProyectosRepository,
    conectividad: ConectividadMonitor,
) : ViewModel() {

    val hayInternet: StateFlow<Boolean> = conectividad.hayInternet

    private val reintentos = MutableStateFlow(0)

    val estado: StateFlow<InicioClienteUi> = reintentos
        .flatMapLatest {
            sesion.usuario.flatMapLatest { usuario ->
                if (usuario == null) {
                    flowOf(InicioClienteUi(cargando = false))
                } else {
                    proyectos.proyectosDelCliente(usuario.uid)
                        .map { InicioClienteUi(cargando = false, proyectos = it) }
                        .onStart { emit(InicioClienteUi(cargando = true)) }
                }
            }
        }
        .catch { emit(InicioClienteUi(cargando = false)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioClienteUi())

    fun reintentar() {
        reintentos.value++
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppNettsegur
                InicioClienteViewModel(
                    app.container.sesionRepository,
                    app.container.proyectosRepository,
                    app.container.conectividad,
                )
            }
        }
    }
}
