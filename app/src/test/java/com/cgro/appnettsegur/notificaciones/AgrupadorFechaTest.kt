package com.cgro.appnettsegur.notificaciones

import com.cgro.appnettsegur.data.model.Notificacion
import com.cgro.appnettsegur.data.model.TipoNotificacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pruebas de la agrupación por fecha de la bandeja (M3, fase 5). Se usa un reloj FIJO y una zona
 * fija, así el resultado no depende de la hora real del sistema.
 */
class AgrupadorFechaTest {

    private val zona = ZoneId.of("UTC")
    private val hoy = LocalDate.of(2026, 11, 5)
    private val ahora = hoy.atTime(12, 0).atZone(zona).toInstant().toEpochMilli()

    /** Notificación cuyo instante es el inicio del día [fecha] (o 0 para "sin fecha del servidor"). */
    private fun notif(id: String, fecha: LocalDate?): Notificacion = Notificacion(
        notificacionId = id,
        tipo = TipoNotificacion.NOVEDAD_SINCRONIZADA,
        titulo = "t",
        mensaje = "m",
        proyectoId = "PRY",
        refId = "REF",
        fecha = fecha?.atStartOfDay(zona)?.toInstant()?.toEpochMilli() ?: 0L,
        leida = false,
    )

    @Test
    fun `agrupa en Hoy, Esta semana y Anteriores en ese orden`() {
        val items = listOf(
            notif("vieja", hoy.minusDays(10)),
            notif("hoy", hoy),
            notif("ayer", hoy.minusDays(1)),
            notif("sinFecha", null),
        )
        val grupos = AgrupadorFecha.agrupar(items, ahora, zona)

        // Orden fijo de los grupos.
        assertEquals(listOf(GrupoBandeja.HOY, GrupoBandeja.ESTA_SEMANA, GrupoBandeja.ANTERIORES), grupos.map { it.first })
        assertEquals(listOf("hoy"), grupos[0].second.map { it.notificacionId })
        assertEquals(listOf("ayer"), grupos[1].second.map { it.notificacionId })
        // Sin fecha (epoch 0) también cae en Anteriores.
        assertTrue(grupos[2].second.map { it.notificacionId }.containsAll(listOf("vieja", "sinFecha")))
    }

    @Test
    fun `el limite de la semana son 6 dias atras y 7 dias ya es Anterior`() {
        val items = listOf(
            notif("seis", hoy.minusDays(6)),
            notif("siete", hoy.minusDays(7)),
        )
        val grupos = AgrupadorFecha.agrupar(items, ahora, zona).toMap()
        assertEquals(listOf("seis"), grupos[GrupoBandeja.ESTA_SEMANA]?.map { it.notificacionId })
        assertEquals(listOf("siete"), grupos[GrupoBandeja.ANTERIORES]?.map { it.notificacionId })
    }

    @Test
    fun `dentro de un grupo van de la mas reciente a la mas antigua`() {
        val items = listOf(
            notif("ayer", hoy.minusDays(1)),
            notif("hace3", hoy.minusDays(3)),
            notif("hace2", hoy.minusDays(2)),
        )
        val semana = AgrupadorFecha.agrupar(items, ahora, zona).toMap()[GrupoBandeja.ESTA_SEMANA]!!
        assertEquals(listOf("ayer", "hace2", "hace3"), semana.map { it.notificacionId })
    }

    @Test
    fun `lista vacia no produce grupos`() {
        assertTrue(AgrupadorFecha.agrupar(emptyList(), ahora, zona).isEmpty())
    }
}
