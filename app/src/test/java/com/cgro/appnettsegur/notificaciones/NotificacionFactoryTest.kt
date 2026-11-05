package com.cgro.appnettsegur.notificaciones

import com.cgro.appnettsegur.data.local.EstadoSync
import com.cgro.appnettsegur.data.local.NovedadEntity
import com.cgro.appnettsegur.data.model.Carta
import com.cgro.appnettsegur.data.model.Documento
import com.cgro.appnettsegur.data.model.TipoDocumento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de NotificacionFactory (MODULOS.md, secciones 5.3 y 7): IDs DETERMINISTAS para que un
 * reintento sobrescriba la misma notificación y no la duplique.
 */
class NotificacionFactoryTest {

    private fun novedad(id: String) = NovedadEntity(
        idRegistro = id, proyectoId = "PRY-001", nombreCliente = "Cliente", nombreTecnico = "Téc",
        fechaServicio = 0, horaInicio = "08:00", horaFin = "09:00", tipoServicio = "Instalación",
        tipoActividad = "CCTV / cámaras", serialEquipo = "ABC123", descServAsignado = "x",
        estadoAtencion = "RESUELTA", descServRealizado = "y", motivoNoResuelta = null,
        observacionesRecomendaciones = null, nombreRepresentanteCliente = "Rep",
        cedulaRepresentanteCliente = "1234567", rolRepresentanteCliente = "Admin",
        rutaFirmaTecnico = "/a", rutaFirmaRepresentante = "/b", estadoSync = EstadoSync.PENDIENTE,
        fechaCreacion = 0, fechaModificacion = 0, fechaSincronizacion = null, mensajeError = null,
    )

    @Test
    fun `novedadSincronizada usa id determinista y un solo nodo`() {
        val mapa = NotificacionFactory.novedadSincronizada("UID_CLI", novedad("NOV-1"))
        assertEquals(1, mapa.size)
        val (ruta, valor) = mapa.entries.first()
        assertEquals("notificaciones/UID_CLI/nov_NOV-1", ruta)
        val v = valor as Map<*, *>
        assertEquals("NOVEDAD_SINCRONIZADA", v["tipo"])
        assertEquals("NOV-1", v["refId"])
        assertEquals("PRY-001", v["proyectoId"])
        assertEquals(false, v["leida"])
    }

    @Test
    fun `dos llamadas con la misma novedad dan la misma ruta (no duplica)`() {
        val a = NotificacionFactory.novedadSincronizada("UID_CLI", novedad("NOV-1"))
        val b = NotificacionFactory.novedadSincronizada("UID_CLI", novedad("NOV-1"))
        assertEquals(a.keys, b.keys)
    }

    @Test
    fun `documentoNuevo usa doc_ y apunta al documento`() {
        val doc = Documento("DOC-9", "PRY-001", TipoDocumento.INFORME_AVANCE, "Informe.pdf", "ARCH-1", "application/pdf", 10, "ing", 0)
        val mapa = NotificacionFactory.documentoNuevo("UID_CLI", "PRY-001", doc)
        val (ruta, valor) = mapa.entries.first()
        assertEquals("notificaciones/UID_CLI/doc_DOC-9", ruta)
        val v = valor as Map<*, *>
        assertEquals("DOCUMENTO_NUEVO", v["tipo"])
        assertEquals("DOC-9", v["refId"])
    }

    @Test
    fun `cartaNueva usa carta_ y apunta a la carta`() {
        val carta = Carta("CARTA-7", "PRY-001", listOf("EMP-1"), "ARCH-2", "analista", 0)
        val mapa = NotificacionFactory.cartaNueva("UID_CLI", "PRY-001", carta)
        val (ruta, valor) = mapa.entries.first()
        assertEquals("notificaciones/UID_CLI/carta_CARTA-7", ruta)
        val v = valor as Map<*, *>
        assertEquals("CARTA_NUEVA", v["tipo"])
        assertEquals("CARTA-7", v["refId"])
        assertTrue(v.containsKey("fecha"))
    }
}
