package com.cgro.appnettsegur.notificaciones

import com.cgro.appnettsegur.data.model.Notificacion
import java.time.Instant
import java.time.ZoneId

/**
 * Agrupa las notificaciones de la bandeja por antigüedad (MODULOS.md, sección 6/7, fase 1 de M3).
 *
 * Es lógica PURA (sin Android ni Firebase) para poder probarla con un reloj fijo: se le pasa el
 * "ahora" en milisegundos, así la prueba no depende de la hora real del sistema (fase 5).
 */
enum class GrupoBandeja(val titulo: String) {
    HOY("Hoy"),
    ESTA_SEMANA("Esta semana"),
    ANTERIORES("Anteriores"),
}

object AgrupadorFecha {

    /**
     * Devuelve los grupos en orden fijo (Hoy, Esta semana, Anteriores), omitiendo los vacíos.
     * Dentro de cada grupo, las notificaciones quedan de la más reciente a la más antigua.
     *
     * - HOY: la notificación es del mismo día calendario que [ahoraMs].
     * - ESTA_SEMANA: no es de hoy, pero cae dentro de los últimos 7 días.
     * - ANTERIORES: el resto (incluye las que aún no tienen fecha del servidor, fecha = 0).
     */
    fun agrupar(
        items: List<Notificacion>,
        ahoraMs: Long,
        zona: ZoneId = ZoneId.systemDefault(),
    ): List<Pair<GrupoBandeja, List<Notificacion>>> {
        val hoy = Instant.ofEpochMilli(ahoraMs).atZone(zona).toLocalDate()
        val agrupadas = items
            .sortedByDescending { it.fecha }
            .groupBy { notif ->
                val dia = Instant.ofEpochMilli(notif.fecha).atZone(zona).toLocalDate()
                when {
                    dia.isEqual(hoy) -> GrupoBandeja.HOY
                    dia.isAfter(hoy.minusDays(7)) && dia.isBefore(hoy) -> GrupoBandeja.ESTA_SEMANA
                    else -> GrupoBandeja.ANTERIORES
                }
            }
        // Orden fijo de los grupos; se omiten los que no tienen notificaciones.
        return GrupoBandeja.entries.mapNotNull { grupo ->
            agrupadas[grupo]?.let { grupo to it }
        }
    }
}
