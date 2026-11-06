package com.cgro.appnettsegur.ui.notificaciones

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cgro.appnettsegur.AppNettsegur
import com.cgro.appnettsegur.data.repository.EmpleadosRepository
import com.cgro.appnettsegur.notificaciones.AlertasRhPublicador
import com.cgro.appnettsegur.notificaciones.NotificacionesStore
import com.cgro.appnettsegur.notificaciones.ResumenVencimientos
import com.cgro.appnettsegur.notificaciones.VencimientosRh
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Resumen de vencimientos para la tarjeta del inicio de RH y disparo del aviso al entrar (M3, fase 4).
 */
class RhVencimientosViewModel(
    private val empleados: EmpleadosRepository,
    private val store: NotificacionesStore,
    private val appContext: Context,
) : ViewModel() {

    /** Alimenta la tarjeta "N vencidos · M por vencer"; se recalcula si cambian los empleados. */
    val resumen: StateFlow<ResumenVencimientos?> = empleados.empleados()
        .map { VencimientosRh.revisar(it, LocalDate.now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var yaReviso = false

    /** Al entrar RH a su inicio: publica las alertas locales de hoy (una sola vez por sesión abierta). */
    fun revisarYAvisar() {
        if (yaReviso) return
        yaReviso = true
        viewModelScope.launch {
            val lista = empleados.empleados().first()
            AlertasRhPublicador.revisarYPublicar(appContext, lista, LocalDate.now(), store)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppNettsegur
                RhVencimientosViewModel(
                    app.container.empleadosRepository,
                    app.container.notificacionesStore,
                    app.applicationContext,
                )
            }
        }
    }
}
