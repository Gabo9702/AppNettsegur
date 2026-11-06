package com.cgro.appnettsegur.notificaciones

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cgro.appnettsegur.data.repository.NotificacionesRepositoryImpl
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

/**
 * Revisa las notificaciones del usuario con la app CERRADA (M3, fase 3).
 *
 * LIMITACIÓN CONOCIDA (trabajo futuro): sin un servidor (Cloud Functions + FCM requieren el plan
 * Blaze, sección 2), no hay push real. Este worker se ejecuta cada 15 minutos (el mínimo que
 * permite Android) y SOLO cuando hay red, así que el aviso con la app cerrada puede llegar con
 * 15 minutos de retraso o más (Android puede diferir los trabajos en segundo plano).
 *
 * IMPORTANTE: este worker solo LEE notificaciones y publica avisos del sistema. NO sincroniza
 * novedades ni escribe nada en Firebase; lo único que guarda es el registro local de "ya mostradas".
 * El taller prohíbe la sincronización automática de novedades.
 */
class NotificacionesWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Si Firebase no está configurado (falta google-services.json), no hay nada que revisar.
        if (FirebaseApp.getApps(applicationContext).isEmpty()) return Result.success()

        // Solo tiene sentido si hay una sesión persistida en el dispositivo.
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        val repo = NotificacionesRepositoryImpl(applicationContext)
        val store = NotificacionesStore(applicationContext)

        val lista = repo.leerUnaVez(uid)
        val nuevas = RegistroMostradas.nuevas(lista, store.yaMostradas())
        if (nuevas.isEmpty()) return Result.success()

        // Si hay más de 3, un solo aviso resumido para no saturar la barra de notificaciones.
        if (nuevas.size > 3) {
            PublicadorNotificaciones.publicarResumen(applicationContext, nuevas.size)
        } else {
            nuevas.forEach { PublicadorNotificaciones.publicar(applicationContext, it) }
        }

        // Se marcan como mostradas para que un listener en primer plano no las repita luego.
        store.marcarMostradas(nuevas.map { it.notificacionId }, vigentes = RegistroMostradas.vigentes(lista))
        return Result.success()
    }
}
