package com.cgro.appnettsegur.ui.rh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cgro.appnettsegur.AppNettsegur
import com.cgro.appnettsegur.data.model.Empleado
import com.cgro.appnettsegur.data.repository.EmpleadosRepository
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
import java.time.LocalDate

data class InicioRhUi(
    val cargando: Boolean = true,
    val empleados: List<Empleado> = emptyList(),
)

/**
 * Inicio de HCQ y RH (M2, fase 3): lista de empleados con buscador. El estado de cumplimiento se
 * calcula (no se guarda) y la lista se ordena con los VENCIDOS primero. Trabaja en línea (sección 3).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InicioRhViewModel(
    private val empleados: EmpleadosRepository,
    conectividad: ConectividadMonitor,
) : ViewModel() {

    val hayInternet: StateFlow<Boolean> = conectividad.hayInternet

    private val reintentos = MutableStateFlow(0)
    val busqueda = MutableStateFlow("")

    private val todos: StateFlow<InicioRhUi> = reintentos
        .flatMapLatest {
            empleados.empleados()
                .map { lista ->
                    val hoy = LocalDate.now()
                    // Vencidos primero (peor estado), luego por nombre.
                    val ordenada = lista.sortedWith(
                        compareByDescending<Empleado> { it.estadoCumplimiento(hoy).ordinal }.thenBy { it.nombre.lowercase() }
                    )
                    InicioRhUi(cargando = false, empleados = ordenada)
                }
                .onStart { emit(InicioRhUi(cargando = true)) }
        }
        .catch { emit(InicioRhUi(cargando = false)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioRhUi())

    /** Lista visible según el buscador (nombre o cédula). */
    val estado: StateFlow<InicioRhUi> = combineBusqueda()

    private fun combineBusqueda(): StateFlow<InicioRhUi> =
        kotlinx.coroutines.flow.combine(todos, busqueda) { ui, q ->
            if (q.isBlank()) ui
            else ui.copy(
                empleados = ui.empleados.filter {
                    it.nombre.contains(q, ignoreCase = true) || it.cedula.contains(q.trim())
                },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioRhUi())

    fun buscar(texto: String) { busqueda.value = texto }

    fun reintentar() { reintentos.value++ }

    /** Crea un empleado nuevo (o guarda cambios de datos básicos) desde el inicio de RH. */
    suspend fun guardar(empleado: Empleado): Result<Unit> = empleados.guardar(empleado)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppNettsegur
                InicioRhViewModel(app.container.empleadosRepository, app.container.conectividad)
            }
        }
    }
}
