package com.cgro.appnettsegur.validacion

import com.cgro.appnettsegur.config.Catalogos
import com.cgro.appnettsegur.config.Mensajes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId

class ReglasValidacionTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private val zona = ZoneId.of("America/Bogota")
    private fun ms(fecha: LocalDateTime) = fecha.atZone(zona).toInstant().toEpochMilli()

    // ---------- R1 ----------
    @Test
    fun r1_noResueltaSinMotivo_falla() {
        assertEquals(Mensajes.R1_MOTIVO, ReglasValidacion.r1MotivoNoResuelta(Catalogos.NO_RESUELTA, ""))
    }

    @Test
    fun r1_motivoConEspaciosEnExtremosNoCuenta() {
        val motivo = "   " + "a".repeat(19) + "   "
        assertEquals(Mensajes.R1_MOTIVO, ReglasValidacion.r1MotivoNoResuelta(Catalogos.NO_RESUELTA, motivo))
    }

    @Test
    fun r1_motivoDe20Caracteres_pasa() {
        assertNull(ReglasValidacion.r1MotivoNoResuelta(Catalogos.NO_RESUELTA, "Falta repuesto DVR 1"))
    }

    @Test
    fun r1_resueltaNoExigeMotivo() {
        assertNull(ReglasValidacion.r1MotivoNoResuelta(Catalogos.RESUELTA, ""))
    }

    // ---------- R2 ----------
    @Test
    fun r2_inicioAnteriorAFin_pasa() {
        assertNull(ReglasValidacion.r2Horas("08:00", "08:01"))
    }

    @Test
    fun r2_inicioIgualOPosteriorAFin_falla() {
        assertEquals(Mensajes.R2_HORAS, ReglasValidacion.r2Horas("09:00", "09:00"))
        assertEquals(Mensajes.R2_HORAS, ReglasValidacion.r2Horas("10:30", "09:00"))
    }

    @Test
    fun r2_horaMalFormada_falla() {
        assertEquals(Mensajes.R2_HORAS, ReglasValidacion.r2Horas("8h", "09:00"))
    }

    @Test
    fun r2_fechaDeHoyOAnterior_pasa() {
        val ahora = ms(LocalDateTime.of(2026, 10, 2, 15, 0))
        assertNull(ReglasValidacion.r2Fecha(ms(LocalDateTime.of(2026, 10, 2, 23, 59)), ahora, zona))
        assertNull(ReglasValidacion.r2Fecha(ms(LocalDateTime.of(2026, 9, 30, 8, 0)), ahora, zona))
    }

    @Test
    fun r2_fechaFutura_falla() {
        val ahora = ms(LocalDateTime.of(2026, 10, 2, 15, 0))
        val manana = ms(LocalDateTime.of(2026, 10, 3, 0, 1))
        assertEquals(Mensajes.R2_FECHA_FUTURA, ReglasValidacion.r2Fecha(manana, ahora, zona))
    }

    // ---------- R3 ----------
    @Test
    fun r3_cedulasValidas() {
        assertNull(ReglasValidacion.r3Cedula("123456"))
        assertNull(ReglasValidacion.r3Cedula("1234567890"))
    }

    @Test
    fun r3_cedulasInvalidas() {
        listOf("12345", "12345678901", "12.345.678", "12345a", "").forEach {
            assertEquals("Cédula '$it'", Mensajes.R3_CEDULA, ReglasValidacion.r3Cedula(it))
        }
    }

    // ---------- R4 ----------
    @Test
    fun r4_serialesValidos() {
        assertNull(ReglasValidacion.r4Serial("AB12"))
        assertNull(ReglasValidacion.r4Serial("hk2023dvr0012345678x"))
    }

    @Test
    fun r4_serialesInvalidos() {
        listOf("AB1", "AB 123", "AB-123", "A".repeat(21), "ÑANDU1").forEach {
            assertEquals("Serial '$it'", Mensajes.R4_SERIAL, ReglasValidacion.r4Serial(it))
        }
    }

    // ---------- R5 ----------
    @Test
    fun r5_resueltaConDescripcionSuficiente_pasa() {
        assertNull(
            ReglasValidacion.r5DescRealizado(
                Catalogos.RESUELTA,
                "Se cambió la fuente de poder y se probó la cámara",
                "Cámara sin imagen"
            )
        )
    }

    @Test
    fun r5_descripcionCorta_falla() {
        assertEquals(
            Mensajes.R5_DESC_REALIZADO,
            ReglasValidacion.r5DescRealizado(Catalogos.RESUELTA, "Se cambió la fuente", "Cámara sin imagen")
        )
    }

    @Test
    fun r5_igualAlAsignadoIgnorandoMayusculasYEspacios_falla() {
        val asignado = "Revisar cámara de la portería principal"
        val realizado = "  REVISAR  cámara de la portería   PRINCIPAL "
        assertEquals(
            Mensajes.R5_DESC_REALIZADO,
            ReglasValidacion.r5DescRealizado(Catalogos.RESUELTA, realizado, asignado)
        )
    }

    @Test
    fun r5_noResueltaNoAplica() {
        assertNull(ReglasValidacion.r5DescRealizado(Catalogos.NO_RESUELTA, "corto", "corto"))
    }

    // ---------- R6 ----------
    @Test
    fun r6_sinFotos_falla() {
        assertEquals(Mensajes.R6_SIN_FOTO, ReglasValidacion.r6Evidencias(emptyList()))
    }

    @Test
    fun r6_fotoQueNoExisteOVacia_falla() {
        val vacia = carpeta.newFile("vacia.jpg")
        val inexistente = File(carpeta.root, "no_existe.jpg")
        assertEquals(Mensajes.R6_FOTO_NO_DISPONIBLE, ReglasValidacion.r6Evidencias(listOf(vacia)))
        assertEquals(Mensajes.R6_FOTO_NO_DISPONIBLE, ReglasValidacion.r6Evidencias(listOf(inexistente)))
    }

    @Test
    fun r6_masDeTresFotos_falla() {
        val fotos = (1..4).map { foto("f$it.jpg") }
        assertEquals(Mensajes.R6_MAX_FOTOS, ReglasValidacion.r6Evidencias(fotos))
    }

    @Test
    fun r6_fotoValida_pasa() {
        assertNull(ReglasValidacion.r6Evidencias(listOf(foto("ok.jpg"))))
    }

    @Test
    fun r6_firmas() {
        assertEquals(Mensajes.R6_FIRMA_TECNICO, ReglasValidacion.r6FirmaTecnico(false))
        assertEquals(Mensajes.R6_FIRMA_REPRESENTANTE, ReglasValidacion.r6FirmaRepresentante(false))
        assertNull(ReglasValidacion.r6FirmaTecnico(true))
        assertNull(ReglasValidacion.r6FirmaRepresentante(true))
    }

    // ---------- Validación completa ----------
    @Test
    fun formularioCompleto_esValido() {
        val resultado = ReglasValidacion.validar(datosValidos())
        assertTrue(resultado.errores.toString(), resultado.esValido)
    }

    @Test
    fun camposVacios_sonObligatoriosYElPrimeroEsProyecto() {
        val resultado = ReglasValidacion.validar(
            datosValidos().copy(proyectoId = "", rolRepresentante = " ", serialEquipo = "")
        )
        assertFalse(resultado.esValido)
        assertEquals(Mensajes.CAMPO_OBLIGATORIO, resultado.errores[Campo.PROYECTO])
        assertEquals(Mensajes.CAMPO_OBLIGATORIO, resultado.errores[Campo.SERIAL])
        assertEquals(Mensajes.CAMPO_OBLIGATORIO, resultado.errores[Campo.ROL])
        assertEquals(Campo.PROYECTO, resultado.primerCampoConError)
    }

    @Test
    fun noResueltaSinMotivo_marcaCampoMotivo() {
        val resultado = ReglasValidacion.validar(
            datosValidos().copy(estadoAtencion = Catalogos.NO_RESUELTA, motivoNoResuelta = "")
        )
        assertEquals(Mensajes.R1_MOTIVO, resultado.errores[Campo.MOTIVO])
    }

    private fun foto(nombre: String): File = carpeta.newFile(nombre).apply { writeBytes(byteArrayOf(1, 2, 3)) }

    private fun datosValidos() = DatosFormulario(
        proyectoId = "PRY-001",
        tipoServicio = "Instalación",
        tipoActividad = "CCTV / cámaras",
        serialEquipo = "DVR2026A",
        horaInicio = "08:00",
        horaFin = "10:00",
        fechaServicio = System.currentTimeMillis(),
        descServAsignado = "Instalar cámara en portería",
        estadoAtencion = Catalogos.RESUELTA,
        descServRealizado = "Se instaló la cámara, se cableó y se configuró el DVR",
        motivoNoResuelta = "",
        nombreRepresentante = "Ana Pérez",
        cedulaRepresentante = "1020304050",
        rolRepresentante = "Administradora",
        evidencias = listOf(foto("evidencia.jpg")),
        firmaTecnicoValida = true,
        firmaRepresentanteValida = true,
        ahora = System.currentTimeMillis(),
    )
}
