package com.cgro.appnettsegur.notificaciones

import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.config.RutasFirebase
import com.cgro.appnettsegur.data.local.NovedadEntity
import com.cgro.appnettsegur.data.model.Carta
import com.cgro.appnettsegur.data.model.Documento
import com.cgro.appnettsegur.data.model.TipoNotificacion
import com.google.firebase.database.ServerValue

/**
 * Crea las notificaciones que deben ir DENTRO de una escritura multi-ruta (updateChildren),
 * en la misma operación atómica que el dato que las origina (MODULOS.md, secciones 5.3 y 7).
 *
 * Lo crea M1 y lo mantiene M3. Cada función devuelve un mapa de rutas RELATIVAS a la raíz
 * (listas para `putAll` en el mapa de updateChildren).
 *
 * CLAVE: los ID de notificación son DETERMINISTAS (nov_{id}, doc_{id}, carta_{id}). Así, un
 * reintento de la sincronización sobrescribe la misma notificación y NO la duplica. Limitación
 * conocida (sección 5.3): si un reintento sobrescribe una notificación ya leída, vuelve a
 * aparecer como no leída. Es aceptable y está documentado.
 */
object NotificacionFactory {

    /** Aviso al cliente y al ingeniero cuando el técnico sincroniza una novedad (CU-02). */
    fun novedadSincronizada(uidDestino: String, r: NovedadEntity): Map<String, Any?> =
        notificacion(
            uidDestino = uidDestino,
            notificacionId = "nov_${r.idRegistro}",
            tipo = TipoNotificacion.NOVEDAD_SINCRONIZADA,
            titulo = Mensajes.NOTIF_NOVEDAD_TITULO,
            mensaje = Mensajes.notifNovedadMensaje(r.nombreCliente),
            proyectoId = r.proyectoId,
            refId = r.idRegistro,
        )

    /** Aviso al cliente cuando el ingeniero sube un documento al proyecto (CU-05). */
    fun documentoNuevo(uidDestino: String, proyectoId: String, d: Documento): Map<String, Any?> =
        notificacion(
            uidDestino = uidDestino,
            notificacionId = "doc_${d.documentoId}",
            tipo = TipoNotificacion.DOCUMENTO_NUEVO,
            titulo = Mensajes.NOTIF_DOCUMENTO_TITULO,
            mensaje = Mensajes.notifDocumentoMensaje(d.nombre),
            proyectoId = proyectoId,
            refId = d.documentoId,
        )

    /** Aviso al cliente cuando el analista publica una carta de presentación (CU-09). */
    fun cartaNueva(uidDestino: String, proyectoId: String, c: Carta): Map<String, Any?> =
        notificacion(
            uidDestino = uidDestino,
            notificacionId = "carta_${c.cartaId}",
            tipo = TipoNotificacion.CARTA_NUEVA,
            titulo = Mensajes.NOTIF_CARTA_TITULO,
            mensaje = Mensajes.NOTIF_CARTA_MENSAJE,
            proyectoId = proyectoId,
            refId = c.cartaId,
        )

    private fun notificacion(
        uidDestino: String,
        notificacionId: String,
        tipo: TipoNotificacion,
        titulo: String,
        mensaje: String,
        proyectoId: String,
        refId: String,
    ): Map<String, Any?> = mapOf(
        RutasFirebase.notificacion(uidDestino, notificacionId) to mapOf(
            "tipo" to tipo.name,
            "titulo" to titulo,
            "mensaje" to mensaje,
            "proyectoId" to proyectoId,
            "refId" to refId,
            // La hora la pone el servidor cuando confirma la escritura.
            "fecha" to ServerValue.TIMESTAMP,
            "leida" to false,
        ),
    )
}
