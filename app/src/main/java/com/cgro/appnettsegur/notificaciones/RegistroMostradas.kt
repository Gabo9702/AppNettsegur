package com.cgro.appnettsegur.notificaciones

import com.cgro.appnettsegur.data.model.Notificacion

/**
 * Lógica PURA de "ya mostradas" (M3, fase 2), separada para poder probarla sin Android (fase 5).
 *
 * A partir de la lista de notificaciones del usuario y del conjunto de IDs ya avisados, decide
 * cuáles son NUEVAS (no leídas y nunca avisadas). Así ni el listener en primer plano ni el worker
 * en segundo plano repiten un aviso.
 */
object RegistroMostradas {

    /** Notificaciones que hay que avisar ahora: no leídas y que no estaban en [yaMostradas]. */
    fun nuevas(notificaciones: List<Notificacion>, yaMostradas: Set<String>): List<Notificacion> =
        notificaciones.filter { !it.leida && it.notificacionId !in yaMostradas }

    /** IDs de todas las notificaciones vigentes (para poder podar las ya mostradas que ya no existen). */
    fun vigentes(notificaciones: List<Notificacion>): Set<String> =
        notificaciones.map { it.notificacionId }.toSet()
}
