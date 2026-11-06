package com.cgro.appnettsegur.notificaciones

import com.cgro.appnettsegur.data.model.DocumentoCumplimiento
import com.cgro.appnettsegur.data.model.Empleado
import com.cgro.appnettsegur.data.model.EstadoCumplimiento
import com.cgro.appnettsegur.data.model.TipoCumplimiento
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Pruebas de la revisión de vencimientos de RH (M3, fase 5, flujo alterno de CU-07).
 * El estado se calcula por documento con una fecha fija.
 */
class VencimientosRhTest {

    private val hoy = LocalDate.of(2026, 11, 5)

    private fun doc(tipo: TipoCumplimiento, vencimiento: LocalDate) =
        DocumentoCumplimiento(tipo, fechaExpedicion = hoy.minusYears(1), fechaVencimiento = vencimiento, archivoId = "a")

    private fun empleado(id: String, nombre: String, vararg docs: DocumentoCumplimiento) =
        Empleado(id, nombre, "1234567", "Técnico", cumplimiento = docs.associateBy { it.tipo })

    @Test
    fun `empleado al dia no genera alertas`() {
        val e = empleado(
            "E1", "Al Día",
            doc(TipoCumplimiento.ARL, hoy.plusMonths(6)),
            doc(TipoCumplimiento.SEGURIDAD_SOCIAL, hoy.plusMonths(6)),
            doc(TipoCumplimiento.ALTURAS, hoy.plusMonths(6)),
        )
        val r = VencimientosRh.revisar(listOf(e), hoy)
        assertEquals(0, r.vencidos)
        assertEquals(0, r.porVencer)
        assertEquals(emptyList<AlertaVencimiento>(), r.alertas)
    }

    @Test
    fun `cuenta vencidos y por vencer por documento`() {
        val e = empleado(
            "E1", "Mixto",
            doc(TipoCumplimiento.ARL, hoy.minusDays(1)),     // vencido
            doc(TipoCumplimiento.SEGURIDAD_SOCIAL, hoy.plusDays(10)), // por vencer
            doc(TipoCumplimiento.ALTURAS, hoy.plusMonths(6)), // vigente
        )
        val r = VencimientosRh.revisar(listOf(e), hoy)
        assertEquals(1, r.vencidos)
        assertEquals(1, r.porVencer)
        assertEquals(2, r.alertas.size)
    }

    @Test
    fun `documento faltante cuenta como vencido y la alerta no tiene fecha`() {
        val e = empleado(
            "E1", "Falta Alturas",
            doc(TipoCumplimiento.ARL, hoy.plusMonths(6)),
            doc(TipoCumplimiento.SEGURIDAD_SOCIAL, hoy.plusMonths(6)),
            // falta ALTURAS
        )
        val r = VencimientosRh.revisar(listOf(e), hoy)
        assertEquals(1, r.vencidos)
        val alerta = r.alertas.single()
        assertEquals(TipoCumplimiento.ALTURAS, alerta.tipo)
        assertEquals(EstadoCumplimiento.VENCIDO, alerta.estado)
        assertEquals(null, alerta.fechaVencimiento)
        // La clave es determinista por empleado+tipo+fecha (aquí "FALTA").
        assertEquals("venc_E1_ALTURAS_FALTA", alerta.clave)
    }

    @Test
    fun `el limite de 15 dias es por vencer`() {
        val e = empleado(
            "E1", "Limite",
            doc(TipoCumplimiento.ARL, hoy.plusDays(15)),
            doc(TipoCumplimiento.SEGURIDAD_SOCIAL, hoy.plusMonths(6)),
            doc(TipoCumplimiento.ALTURAS, hoy.plusMonths(6)),
        )
        val r = VencimientosRh.revisar(listOf(e), hoy)
        assertEquals(0, r.vencidos)
        assertEquals(1, r.porVencer)
    }
}
