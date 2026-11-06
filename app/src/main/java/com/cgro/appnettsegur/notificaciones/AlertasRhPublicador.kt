package com.cgro.appnettsegur.notificaciones

import android.content.Context
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.data.model.Empleado
import com.cgro.appnettsegur.data.model.EstadoCumplimiento
import com.cgro.appnettsegur.data.model.TipoCumplimiento
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Publica como notificaciones LOCALES del sistema las alertas de vencimiento de RH (M3, fase 4).
 *
 * Lo usan tanto la pantalla de inicio de RH (al entrar) como el worker diario. Se evita repetir la
 * misma alerta el mismo día con una clave que incluye la fecha de hoy: al día siguiente la clave
 * cambia y la alerta vuelve a salir. Estas alertas NO se escriben en Firebase.
 */
object AlertasRhPublicador {

    private val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /**
     * Revisa el cumplimiento y publica las alertas nuevas de hoy. Devuelve el resumen (para la
     * tarjeta del inicio de RH). [store] evita repetir avisos el mismo día.
     */
    suspend fun revisarYPublicar(
        context: Context,
        empleados: List<Empleado>,
        hoy: LocalDate,
        store: NotificacionesStore,
    ): ResumenVencimientos {
        val resumen = VencimientosRh.revisar(empleados, hoy)
        val yaMostradas = store.vencimientosRhMostrados()
        val nuevas = mutableListOf<String>()

        for (alerta in resumen.alertas) {
            val claveDia = "${alerta.clave}_$hoy" // incluye hoy → se puede repetir día a día, no en el mismo día
            if (claveDia in yaMostradas) continue
            PublicadorNotificaciones.publicarLocal(
                context = context,
                idAndroid = alerta.clave.hashCode(), // estable por empleado+tipo+fecha: reemplaza, no apila
                titulo = Mensajes.VENC_TITULO,
                mensaje = Mensajes.vencMensaje(alerta.empleadoNombre, tipoLegible(alerta.tipo), estadoTexto(alerta)),
            )
            nuevas += claveDia
        }
        store.marcarVencimientosRh(nuevas)
        return resumen
    }

    private fun tipoLegible(tipo: TipoCumplimiento): String = when (tipo) {
        TipoCumplimiento.ARL -> "ARL"
        TipoCumplimiento.SEGURIDAD_SOCIAL -> "Seguridad social"
        TipoCumplimiento.ALTURAS -> "Certificado de alturas"
    }

    private fun estadoTexto(alerta: AlertaVencimiento): String = when {
        alerta.fechaVencimiento == null -> "sin registrar"
        alerta.estado == EstadoCumplimiento.VENCIDO -> "vencido el ${alerta.fechaVencimiento.format(formato)}"
        else -> "por vencer (vence el ${alerta.fechaVencimiento.format(formato)})"
    }
}
