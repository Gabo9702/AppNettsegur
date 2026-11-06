package com.cgro.appnettsegur.notificaciones

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Programa y cancela los trabajos periódicos de notificaciones (M3, fases 3 y 4).
 *
 * El trabajo de recepción (cliente/ingeniero) se programa al iniciar sesión y se cancela al
 * cerrarla. Se usa enqueueUniquePeriodicWork con política KEEP para no apilar copias si se vuelve
 * a programar (p. ej. si la app se reabre sin que el trabajo anterior se cancelara).
 */
object ProgramadorTrabajos {

    private const val TRABAJO_RECEPCION = "notificaciones_recepcion"
    const val TRABAJO_VENCIMIENTOS_RH = "vencimientos_rh" // fase 4

    /** 15 min es el período mínimo que permite Android para un PeriodicWorkRequest. */
    private const val PERIODO_MIN = 15L

    private val soloConRed = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Recepción de notificaciones con la app cerrada (fase 3). */
    fun programarRecepcion(context: Context) {
        val solicitud = PeriodicWorkRequestBuilder<NotificacionesWorker>(PERIODO_MIN, TimeUnit.MINUTES)
            .setConstraints(soloConRed)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TRABAJO_RECEPCION,
            ExistingPeriodicWorkPolicy.KEEP,
            solicitud,
        )
    }

    fun cancelarRecepcion(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(TRABAJO_RECEPCION)
    }

    /** Revisión diaria de vencimientos de cumplimiento para RH (fase 4). */
    fun programarVencimientosRh(context: Context) {
        val solicitud = PeriodicWorkRequestBuilder<VencimientosRhWorker>(1, TimeUnit.DAYS)
            .setConstraints(soloConRed)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TRABAJO_VENCIMIENTOS_RH,
            ExistingPeriodicWorkPolicy.KEEP,
            solicitud,
        )
    }

    fun cancelarVencimientosRh(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(TRABAJO_VENCIMIENTOS_RH)
    }
}
