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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.io.File
import java.time.LocalDate

/**
 * Implementaciones de RELLENO de la fase A (MODULOS.md, sección 6.2).
 *
 * Existen para que el proyecto compile y para que M2 y M3 puedan programar contra las interfaces
 * desde ya. La FASE C las reemplaza por implementaciones reales contra Firebase RTDB.
 */

private fun <T> pendiente(): Result<T> =
    Result.failure(Exception("Repositorio aún no implementado (fase C)."))

class ProyectosRepositoryRelleno : ProyectosRepository {
    override fun proyectosDelCliente(uid: String): Flow<List<Proyecto>> = flowOf(emptyList())
    override fun proyectosDelIngeniero(uid: String): Flow<List<Proyecto>> = flowOf(emptyList())
    override fun proyecto(id: String): Flow<Proyecto?> = flowOf(null)
    override fun novedadesDelProyecto(id: String): Flow<List<NovedadRemota>> = flowOf(emptyList())
    override fun personalDelProyecto(id: String): Flow<List<PersonalAsignado>> = flowOf(emptyList())
    override suspend fun marcarHito(proyectoId: String, hitoId: String, completado: Boolean): Result<Unit> = pendiente()
    override suspend fun asignarTecnico(proyectoId: String, empleado: Empleado, asignar: Boolean): Result<Unit> = pendiente()
}

class DocumentosRepositoryRelleno : DocumentosRepository {
    override fun documentos(proyectoId: String): Flow<List<Documento>> = flowOf(emptyList())
    override suspend fun subir(proyectoId: String, tipo: TipoDocumento, uri: Uri): Result<Documento> = pendiente()
}

class ArchivosRepositoryRelleno : ArchivosRepository {
    override suspend fun descargarAArchivoLocal(archivoId: String): Result<File> = pendiente()
}

class EmpleadosRepositoryRelleno : EmpleadosRepository {
    override fun empleados(): Flow<List<Empleado>> = flowOf(emptyList())
    override fun empleado(id: String): Flow<Empleado?> = flowOf(null)
    override suspend fun guardar(empleado: Empleado): Result<Unit> = pendiente()
    override suspend fun subirCumplimiento(
        empleadoId: String,
        tipo: TipoCumplimiento,
        expedicion: LocalDate,
        vencimiento: LocalDate,
        uri: Uri,
    ): Result<Unit> = pendiente()
    override suspend fun subirCarne(empleadoId: String, uri: Uri): Result<Unit> = pendiente()
}

class CartasRepositoryRelleno : CartasRepository {
    override fun cartasDelProyecto(proyectoId: String): Flow<List<Carta>> = flowOf(emptyList())
    override suspend fun guardarCarta(proyectoId: String, empleados: List<Empleado>, pdf: File): Result<Carta> = pendiente()
}
