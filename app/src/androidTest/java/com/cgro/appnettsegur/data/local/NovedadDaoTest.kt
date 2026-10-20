package com.cgro.appnettsegur.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NovedadDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var novedadDao: NovedadDao
    private lateinit var evidenciaDao: EvidenciaDao

    @Before
    fun crearBase() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        novedadDao = db.novedadDao()
        evidenciaDao = db.evidenciaDao()
    }

    @After
    fun cerrarBase() = db.close()

    @Test
    fun guardarYLeerConEstadoPendiente() = runBlocking {
        novedadDao.guardar(novedad("a", creada = 1))

        val leida = novedadDao.obtenerPorId("a")
        assertEquals(EstadoSync.PENDIENTE, leida?.estadoSync)
    }

    @Test
    fun guardarDosVecesNoDuplica() = runBlocking {
        novedadDao.guardar(novedad("a", creada = 1))
        novedadDao.guardar(novedad("a", creada = 1).copy(serialEquipo = "CAMBIO"))

        val todas = novedadDao.observarTodas().first()
        assertEquals(1, todas.size)
        assertEquals("CAMBIO", todas.first().serialEquipo)
    }

    @Test
    fun noSincronizadosIncluyePendienteYErrorEnOrden() = runBlocking {
        novedadDao.guardar(novedad("tercero", creada = 3))
        novedadDao.guardar(novedad("primero", creada = 1, estado = EstadoSync.ERROR))
        novedadDao.guardar(novedad("enviado", creada = 2, estado = EstadoSync.SINCRONIZADO))

        val lista = novedadDao.obtenerNoSincronizados().map { it.idRegistro }
        assertEquals(listOf("primero", "tercero"), lista)
    }

    @Test
    fun marcarSincronizadoYErrorActualizanEstado() = runBlocking {
        novedadDao.guardar(novedad("a", creada = 1))

        novedadDao.marcarError("a", "timeout")
        assertEquals(EstadoSync.ERROR, novedadDao.obtenerPorId("a")?.estadoSync)
        assertEquals("timeout", novedadDao.obtenerPorId("a")?.mensajeError)

        novedadDao.marcarSincronizado("a", 99)
        val leida = novedadDao.obtenerPorId("a")!!
        assertEquals(EstadoSync.SINCRONIZADO, leida.estadoSync)
        assertEquals(99L, leida.fechaSincronizacion)
        assertNull(leida.mensajeError)
    }

    @Test
    fun conteosPorEstado() = runBlocking {
        novedadDao.guardar(novedad("a", creada = 1))
        novedadDao.guardar(novedad("b", creada = 2, estado = EstadoSync.ERROR))
        novedadDao.guardar(novedad("c", creada = 3, estado = EstadoSync.SINCRONIZADO))

        val conteos = novedadDao.observarConteos().first()
        assertEquals(Conteos(total = 3, pendientes = 1, sincronizados = 1, conError = 1), conteos)
    }

    @Test
    fun eliminarNovedadBorraEvidenciasEnCascada() = runBlocking {
        novedadDao.guardar(novedad("a", creada = 1))
        evidenciaDao.insertarTodas(
            listOf(EvidenciaEntity("e1", "a", "/ruta/a_0.jpg", 10, "hash", 100, 100, 1))
        )

        novedadDao.eliminar("a")
        assertTrue(evidenciaDao.obtenerDeRegistro("a").isEmpty())
    }

    private fun novedad(id: String, creada: Long, estado: EstadoSync = EstadoSync.PENDIENTE) =
        NovedadEntity(
            idRegistro = id,
            proyectoId = "PRY-001",
            nombreCliente = "Cliente",
            nombreTecnico = "Técnico",
            fechaServicio = creada,
            horaInicio = "08:00",
            horaFin = "09:00",
            tipoServicio = "Instalación",
            tipoActividad = "CCTV / cámaras",
            serialEquipo = "ABC123",
            descServAsignado = "Instalar cámara",
            estadoAtencion = "RESUELTA",
            descServRealizado = "Se instaló la cámara y se configuró el DVR",
            motivoNoResuelta = null,
            observacionesRecomendaciones = null,
            nombreRepresentanteCliente = "Ana",
            cedulaRepresentanteCliente = "12345678",
            rolRepresentanteCliente = "Administradora",
            rutaFirmaTecnico = "/firmas/t.png",
            rutaFirmaRepresentante = "/firmas/r.png",
            estadoSync = estado,
            fechaCreacion = creada,
            fechaModificacion = creada,
            fechaSincronizacion = null,
            mensajeError = null,
        )
}
