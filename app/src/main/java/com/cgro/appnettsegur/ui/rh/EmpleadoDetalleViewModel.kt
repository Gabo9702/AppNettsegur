package com.cgro.appnettsegur.ui.rh

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cgro.appnettsegur.AppNettsegur
import com.cgro.appnettsegur.data.model.Empleado
import com.cgro.appnettsegur.data.model.TipoCumplimiento
import com.cgro.appnettsegur.data.repository.EmpleadosRepository
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
import java.time.LocalDate

data class EmpleadoDetalleUi(
    val cargando: Boolean = true,
    val empleado: Empleado? = null,
)

/**
 * Detalle de un empleado (M2, fase 3, CU-07): cumplimiento (ARL, seguridad social y alturas) y carné.
 * El estado y la habilitación se calculan en el modelo. Trabaja en línea (sección 3).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EmpleadoDetalleViewModel(
    private val empleados: EmpleadosRepository,
    conectividad: ConectividadMonitor,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val id: String = checkNotNull(savedStateHandle[Rutas.ARG_EMPLEADO])

    val hayInternet: StateFlow<Boolean> = conectividad.hayInternet

    private val reintentos = MutableStateFlow(0)

    val estado: StateFlow<EmpleadoDetalleUi> = reintentos
        .flatMapLatest {
            empleados.empleado(id)
                .map { EmpleadoDetalleUi(cargando = false, empleado = it) }
                .onStart { emit(EmpleadoDetalleUi(cargando = true)) }
        }
        .catch { emit(EmpleadoDetalleUi(cargando = false)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EmpleadoDetalleUi())

    fun reintentar() { reintentos.value++ }

    suspend fun guardar(empleado: Empleado): Result<Unit> = empleados.guardar(empleado)

    suspend fun subirCumplimiento(
        tipo: TipoCumplimiento,
        expedicion: LocalDate,
        vencimiento: LocalDate,
        uri: Uri,
    ): Result<Unit> = empleados.subirCumplimiento(id, tipo, expedicion, vencimiento, uri)

    suspend fun subirCarne(uri: Uri): Result<Unit> = empleados.subirCarne(id, uri)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppNettsegur
                EmpleadoDetalleViewModel(
                    app.container.empleadosRepository,
                    app.container.conectividad,
                    createSavedStateHandle(),
                )
            }
        }
    }
}
