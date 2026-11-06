package com.cgro.appnettsegur.notificaciones

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cgro.appnettsegur.config.Rol
import com.cgro.appnettsegur.data.prefs.SesionStore
import com.cgro.appnettsegur.data.repository.EmpleadosRepositoryImpl
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Revisa una vez al día los documentos de cumplimiento y avisa a RH de los vencidos o por vencer
 * (M3, fase 4, flujo alterno de CU-07).
 *
 * Solo corre para el rol RH. Lee empleados en línea (con red), calcula las alertas con VencimientosRh
 * y las publica como notificaciones LOCALES (nunca en Firebase). No sincroniza ni escribe nada.
 */
class VencimientosRhWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (FirebaseApp.getApps(applicationContext).isEmpty()) return Result.success()
        if (FirebaseAuth.getInstance().currentUser == null) return Result.success()

        // Solo RH recibe estas alertas; si el usuario en caché no es RH, no se hace nada.
        val rol = SesionStore(applicationContext).leer()?.rol
        if (rol != Rol.RH) return Result.success()

        val empleados = EmpleadosRepositoryImpl(applicationContext).empleados().first()
        AlertasRhPublicador.revisarYPublicar(
            context = applicationContext,
            empleados = empleados,
            hoy = LocalDate.now(),
            store = NotificacionesStore(applicationContext),
        )
        return Result.success()
    }
}
