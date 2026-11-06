package com.cgro.appnettsegur.notificaciones

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.cgro.appnettsegur.config.Rol
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.data.model.Notificacion
import com.cgro.appnettsegur.data.repository.NotificacionesRepository
import com.cgro.appnettsegur.data.repository.SesionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Observa las notificaciones del RECEPTOR con sesión iniciada mientras el proceso vive (M3, fase 2).
 *
 * Decide cómo avisar según el ciclo de vida del proceso (ProcessLifecycleOwner):
 * - app en PRIMER PLANO: solo actualiza el contador (automático, vía Flow) y lanza un snackbar;
 * - app en SEGUNDO PLANO: publica una notificación del sistema con PendingIntent al destino.
 *
 * Con la app CERRADA (proceso muerto) este observador no corre: de eso se encarga el worker
 * periódico (fase 3). El registro de "ya mostradas" evita que un mismo aviso se repita.
 *
 * Este componente NO sincroniza novedades ni escribe en Firebase (salvo marcar `leida` desde la
 * bandeja, que es otro flujo): solo LEE notificaciones. Así no rompe la regla del taller.
 */
class ObservadorNotificaciones(
    private val appContext: Context,
    private val sesion: SesionRepository,
    private val repo: NotificacionesRepository,
    private val store: NotificacionesStore,
    private val scope: CoroutineScope,
) {

    // Snackbars para el caso de primer plano; la UI (NavGraph) los recoge y los muestra.
    private val _snackbars = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbars: SharedFlow<String> = _snackbars.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun iniciar() {
        PublicadorNotificaciones.crearCanal(appContext)

        // 1) Al cerrar sesión se limpia el registro de "ya mostradas" (fase 2).
        scope.launch {
            sesion.usuario.map { it?.uid }.distinctUntilChanged().collect { uid ->
                if (uid == null) store.limpiarMostradas()
            }
        }

        // 2) Escucha las notificaciones del receptor actual (cliente o ingeniero).
        //    RH recibe alertas LOCALES (fase 4), no desde Firebase; el técnico no recibe nada.
        scope.launch {
            sesion.usuario
                .flatMapLatest { usuario ->
                    if (usuario != null && usuario.rol in RECEPTORES_FIREBASE) repo.observar(usuario.uid)
                    else flowOf(emptyList())
                }
                .collect { lista -> procesar(lista) }
        }
    }

    private suspend fun procesar(lista: List<Notificacion>) {
        val yaMostradas = store.yaMostradas()
        val nuevas = RegistroMostradas.nuevas(lista, yaMostradas)
        if (nuevas.isEmpty()) return

        if (enPrimerPlano()) {
            // Primer plano: el contador se actualiza solo (Flow); aquí solo avisamos con un snackbar.
            val texto = if (nuevas.size == 1) nuevas.first().titulo else Mensajes.notifResumen(nuevas.size)
            _snackbars.tryEmit(texto)
        } else {
            // Segundo plano: notificación del sistema con PendingIntent al destino.
            nuevas.forEach { PublicadorNotificaciones.publicar(appContext, it) }
        }

        // Se marcan como mostradas (y se podan las que ya no existen) para no repetir el aviso.
        store.marcarMostradas(nuevas.map { it.notificacionId }, vigentes = RegistroMostradas.vigentes(lista))
    }

    private fun enPrimerPlano(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private companion object {
        val RECEPTORES_FIREBASE = setOf(Rol.CLIENTE, Rol.INGENIERO)
    }
}
