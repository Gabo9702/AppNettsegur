package com.cgro.appnettsegur.notificaciones

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cgro.appnettsegur.MainActivity
import com.cgro.appnettsegur.R
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.data.model.Notificacion
import com.cgro.appnettsegur.data.model.TipoNotificacion
import com.cgro.appnettsegur.navigation.Rutas

/**
 * Crea el canal y publica las notificaciones del sistema Android (M3, fase 2).
 *
 * Sin un servidor (Cloud Functions + FCM requieren el plan Blaze, sección 2), estos avisos los
 * dispara la propia app: el listener en primer plano/segundo plano (fase 2) o el worker periódico
 * (fase 3). El PendingIntent reabre la app en el destino correcto de cada notificación.
 */
object PublicadorNotificaciones {

    /** Canal de Android. Se crea en Application.onCreate (es idempotente). */
    fun crearCanal(context: Context) {
        val canal = NotificationChannel(
            Mensajes.CANAL_ID,
            Mensajes.CANAL_NOMBRE,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = Mensajes.CANAL_DESC }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(canal)
    }

    /** Publica el aviso de una notificación concreta, abriendo su destino al tocarla. */
    fun publicar(context: Context, notif: Notificacion) {
        if (!puedePublicar(context)) return
        val id = notif.notificacionId.hashCode()
        val aviso = NotificationCompat.Builder(context, Mensajes.CANAL_ID)
            .setSmallIcon(R.drawable.ic_stat_notificacion)
            .setContentTitle(notif.titulo)
            .setContentText(notif.mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notif.mensaje))
            .setContentIntent(intentDestino(context, rutaDe(notif), id))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(id, aviso)
    }

    /** Aviso resumido cuando hay varias a la vez (fase 3): abre la bandeja. */
    fun publicarResumen(context: Context, cantidad: Int) {
        if (!puedePublicar(context)) return
        val aviso = NotificationCompat.Builder(context, Mensajes.CANAL_ID)
            .setSmallIcon(R.drawable.ic_stat_notificacion)
            .setContentTitle(Mensajes.BANDEJA_TITULO)
            .setContentText(Mensajes.notifResumen(cantidad))
            .setContentIntent(intentDestino(context, Rutas.BANDEJA, ID_RESUMEN))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(ID_RESUMEN, aviso)
    }

    /** Publica una alerta LOCAL (RH, fase 4) con un ID de Android estable por empleado+tipo. */
    fun publicarLocal(context: Context, idAndroid: Int, titulo: String, mensaje: String) {
        if (!puedePublicar(context)) return
        val aviso = NotificationCompat.Builder(context, Mensajes.CANAL_ID)
            .setSmallIcon(R.drawable.ic_stat_notificacion)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setContentIntent(intentDestino(context, Rutas.INICIO_RH, idAndroid))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(idAndroid, aviso)
    }

    /** En Android 13+ no se puede publicar sin el permiso POST_NOTIFICATIONS concedido. */
    private fun puedePublicar(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** PendingIntent que reabre MainActivity y le pasa la ruta de navegación destino. */
    private fun intentDestino(context: Context, ruta: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_RUTA, ruta)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Ruta de navegación según el tipo (igual que el toque en la bandeja). */
    private fun rutaDe(notif: Notificacion): String = when (notif.tipo) {
        TipoNotificacion.NOVEDAD_SINCRONIZADA -> Rutas.novedadRemota(notif.refId)
        TipoNotificacion.DOCUMENTO_NUEVO, TipoNotificacion.CARTA_NUEVA -> Rutas.proyectoDetalle(notif.proyectoId)
    }

    private const val ID_RESUMEN = 100_000
}
