package com.cgro.appnettsegur.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cgro.appnettsegur.AppNettsegur
import com.cgro.appnettsegur.data.repository.ProgresoSync
import com.cgro.appnettsegur.data.repository.SyncRepository
import com.cgro.appnettsegur.util.ConectividadMonitor
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Se crea a nivel de la Activity (no de una pantalla), así la sincronización no se
 * cancela si el usuario navega mientras se envían los registros.
 */
class SyncViewModel(
    private val sync: SyncRepository,
    conectividad: ConectividadMonitor,
) : ViewModel() {

    val progreso: StateFlow<ProgresoSync> = sync.progreso
    val hayInternet: StateFlow<Boolean> = conectividad.hayInternet
    val falloSimuladoEn: StateFlow<Int?> = sync.falloSimuladoEn

    private val _mensajes = Channel<String>(Channel.BUFFERED)
    val mensajes = _mensajes.receiveAsFlow()

    /** Botón SINCRONIZAR o "Reintentar". Si ya hay una sincronización en curso, se ignora. */
    fun sincronizar() {
        if (progreso.value.sincronizando) return
        viewModelScope.launch {
            sync.sincronizar()?.let { _mensajes.send(it) }
        }
    }

    /** Solo debug (Prueba E): número de registro que fallará a propósito, o null para desactivar. */
    fun simularFalloEn(posicion: Int?) {
        sync.falloSimuladoEn.value = posicion?.takeIf { it > 0 }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppNettsegur
                SyncViewModel(app.container.syncRepository, app.container.conectividad)
            }
        }
    }
}
