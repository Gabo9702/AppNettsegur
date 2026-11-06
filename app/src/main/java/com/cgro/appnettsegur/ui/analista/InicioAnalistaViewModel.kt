package com.cgro.appnettsegur.ui.analista

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cgro.appnettsegur.AppNettsegur
import com.cgro.appnettsegur.data.model.Carta
import com.cgro.appnettsegur.data.model.Empleado
import com.cgro.appnettsegur.data.model.Proyecto
import com.cgro.appnettsegur.data.repository.CartasRepository
import com.cgro.appnettsegur.data.repository.EmpleadosRepository
import com.cgro.appnettsegur.data.repository.ProyectosRepository
import com.cgro.appnettsegur.util.ConectividadMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.io.File

/**
 * Inicio del analista administrativo (M2, fase 4, CU-09): empleados (para cargar carnés) y proyectos
 * (para generar cartas de presentación). Trabaja en línea (sección 3).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InicioAnalistaViewModel(
    private val empleadosRepo: EmpleadosRepository,
    private val proyectosRepo: ProyectosRepository,
    private val cartasRepo: CartasRepository,
    conectividad: ConectividadMonitor,
) : ViewModel() {

    val hayInternet: StateFlow<Boolean> = conectividad.hayInternet

    private val reintentos = MutableStateFlow(0)

    val empleados: StateFlow<List<Empleado>> = reintentos
        .flatMapLatest { empleadosRepo.empleados() }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val proyectos: StateFlow<List<Proyecto>> = reintentos
        .flatMapLatest { proyectosRepo.proyectosParaAnalista() }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Proyecto elegido para la carta. */
    val proyectoCarta = MutableStateFlow<Proyecto?>(null)

    /** Técnicos asignados al proyecto elegido (con sus datos reales de empleado). */
    val tecnicosDelProyecto: StateFlow<List<Empleado>> = proyectoCarta
        .flatMapLatest { proyecto ->
            if (proyecto == null) flowOf(emptyList())
            else combine(proyectosRepo.personalDelProyecto(proyecto.proyectoId), empleados) { personal, emps ->
                val ids = personal.map { it.empleadoId }.toSet()
                emps.filter { it.empleadoId in ids }
            }
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun elegirProyecto(proyecto: Proyecto?) { proyectoCarta.value = proyecto }

    fun reintentar() { reintentos.value++ }

    suspend fun subirCarne(empleadoId: String, uri: Uri): Result<Unit> =
        empleadosRepo.subirCarne(empleadoId, uri)

    /** Guarda la carta (PDF) y notifica al cliente del proyecto (CartasRepository). */
    suspend fun guardarCarta(proyectoId: String, tecnicos: List<Empleado>, pdf: File): Result<Carta> =
        cartasRepo.guardarCarta(proyectoId, tecnicos, pdf)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppNettsegur
                InicioAnalistaViewModel(
                    app.container.empleadosRepository,
                    app.container.proyectosRepository,
                    app.container.cartasRepository,
                    app.container.conectividad,
                )
            }
        }
    }
}
