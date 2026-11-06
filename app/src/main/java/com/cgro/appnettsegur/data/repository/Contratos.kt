package com.cgro.appnettsegur.data.repository

import android.net.Uri
import com.cgro.appnettsegur.data.model.Carta
import com.cgro.appnettsegur.data.model.Documento
import com.cgro.appnettsegur.data.model.Empleado
import com.cgro.appnettsegur.data.model.NovedadRemota
import com.cgro.appnettsegur.data.model.PersonalAsignado
import com.cgro.appnettsegur.data.model.Proyecto
import com.cgro.appnettsegur.data.model.TipoCumplimiento
import com.cgro.appnettsegur.data.model.TipoDocumento
import com.cgro.appnettsegur.data.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.time.LocalDate

/**
 * Interfaces que M1 entrega primero (MODULOS.md, sección 6.2). M2 y M3 programan contra ellas.
 * M1 puede AGREGAR métodos, pero NO cambiar estas firmas sin avisar al equipo.
 */

interface SesionRepository {
    /** Usuario con sesión iniciada, o null si no hay sesión. */
    val usuario: StateFlow<Usuario?>
    suspend fun iniciarSesion(correo: String, clave: String): Result<Usuario>
    suspend fun cerrarSesion()
}

interface ProyectosRepository {
    fun proyectosDelCliente(uid: String): Flow<List<Proyecto>>
    fun proyectosDelIngeniero(uid: String): Flow<List<Proyecto>>
    fun proyecto(id: String): Flow<Proyecto?>
    fun novedadesDelProyecto(id: String): Flow<List<NovedadRemota>>
    /**
     * Una novedad sincronizada por su idRegistro (M2: pantalla NovedadRemota y Bandeja de M3).
     * Método ADITIVO solicitado por M2 y confirmado por el equipo (docs/m2_resumen.md).
     */
    fun novedad(idRegistro: String): Flow<NovedadRemota?>
    fun personalDelProyecto(id: String): Flow<List<PersonalAsignado>>
    suspend fun marcarHito(proyectoId: String, hitoId: String, completado: Boolean): Result<Unit>
    suspend fun asignarTecnico(proyectoId: String, empleado: Empleado, asignar: Boolean): Result<Unit>
}

interface DocumentosRepository {
    fun documentos(proyectoId: String): Flow<List<Documento>>
    /** Escribe documento + archivo + notificación al cliente en una sola operación multi-ruta. */
    suspend fun subir(proyectoId: String, tipo: TipoDocumento, uri: Uri): Result<Documento>
}

interface ArchivosRepository {
    /** Decodifica el Base64 de archivos/{archivoId} a un archivo en cacheDir. */
    suspend fun descargarAArchivoLocal(archivoId: String): Result<File>
}

interface EmpleadosRepository {
    fun empleados(): Flow<List<Empleado>>
    fun empleado(id: String): Flow<Empleado?>
    suspend fun guardar(empleado: Empleado): Result<Unit>
    suspend fun subirCumplimiento(
        empleadoId: String,
        tipo: TipoCumplimiento,
        expedicion: LocalDate,
        vencimiento: LocalDate,
        uri: Uri,
    ): Result<Unit>
    suspend fun subirCarne(empleadoId: String, uri: Uri): Result<Unit>
}

interface CartasRepository {
    fun cartasDelProyecto(proyectoId: String): Flow<List<Carta>>
    /** Guarda la carta (PDF) y notifica al cliente del proyecto en una sola operación multi-ruta. */
    suspend fun guardarCarta(proyectoId: String, empleados: List<Empleado>, pdf: File): Result<Carta>
}
