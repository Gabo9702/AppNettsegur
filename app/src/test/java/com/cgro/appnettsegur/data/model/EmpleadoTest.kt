package com.cgro.appnettsegur.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Pruebas de Empleado.estadoCumplimiento/habilitado (MODULOS.md, sección 6.2).
 * El estado se calcula, no se guarda: por eso se puede probar sin Firebase.
 */
class EmpleadoTest {

    private val hoy = LocalDate.of(2026, 11, 5)

    private fun doc(tipo: TipoCumplimiento, vencimiento: LocalDate) =
        DocumentoCumplimiento(tipo, fechaExpedicion = hoy.minusYears(1), fechaVencimiento = vencimiento, archivoId = "a")

    private fun empleadoCon(vararg docs: DocumentoCumplimiento) = Empleado(
        empleadoId = "EMP-1", nombre = "Test", cedula = "1234567", cargo = "Técnico",
        cumplimiento = docs.associateBy { it.tipo },
    )

    @Test
    fun `los tres documentos vigentes dan VIGENTE y habilitado`() {
        val e = empleadoCon(
            doc(TipoCumplimiento.ARL, hoy.plusMonths(6)),
            doc(TipoCumplimiento.SEGURIDAD_SOCIAL, hoy.plusMonths(3)),
            doc(TipoCumplimiento.ALTURAS, hoy.plusYears(1)),
        )
        assertEquals(EstadoCumplimiento.VIGENTE, e.estadoCumplimiento(hoy))
        assertTrue(e.habilitado(hoy))
    }

    @Test
    fun `un documento a 10 dias de vencer da POR_VENCER pero sigue habilitado`() {
        val e = empleadoCon(
            doc(TipoCumplimiento.ARL, hoy.plusDays(10)),
            doc(TipoCumplimiento.SEGURIDAD_SOCIAL, hoy.plusMonths(3)),
            doc(TipoCumplimiento.ALTURAS, hoy.plusYears(1)),
        )
        assertEquals(EstadoCumplimiento.POR_VENCER, e.estadoCumplimiento(hoy))
        assertTrue(e.habilitado(hoy))
    }

    @Test
    fun `justo en el limite de 15 dias es POR_VENCER`() {
        val e = empleadoCon(
            doc(TipoCumplimiento.ARL, hoy.plusDays(15)),
            doc(TipoCumplimiento.SEGURIDAD_SOCIAL, hoy.plusMonths(3)),
            doc(TipoCumplimiento.ALTURAS, hoy.plusYears(1)),
        )
        assertEquals(EstadoCumplimiento.POR_VENCER, e.estadoCumplimiento(hoy))
    }

    @Test
    fun `un documento vencido da VENCIDO y no habilitado`() {
        val e = empleadoCon(
            doc(TipoCumplimiento.ARL, hoy.minusDays(1)),
            doc(TipoCumplimiento.SEGURIDAD_SOCIAL, hoy.plusMonths(3)),
            doc(TipoCumplimiento.ALTURAS, hoy.plusYears(1)),
        )
        assertEquals(EstadoCumplimiento.VENCIDO, e.estadoCumplimiento(hoy))
        assertFalse(e.habilitado(hoy))
    }

    @Test
    fun `si falta un documento se considera VENCIDO y no habilitado`() {
        val e = empleadoCon(
            doc(TipoCumplimiento.ARL, hoy.plusMonths(6)),
            doc(TipoCumplimiento.SEGURIDAD_SOCIAL, hoy.plusMonths(3)),
            // falta ALTURAS
        )
        assertEquals(EstadoCumplimiento.VENCIDO, e.estadoCumplimiento(hoy))
        assertFalse(e.habilitado(hoy))
    }
}
