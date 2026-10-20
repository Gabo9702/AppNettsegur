package com.cgro.appnettsegur.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Estado de envío del registro. No es un Boolean: hay tres estados posibles. */
enum class EstadoSync { PENDIENTE, SINCRONIZADO, ERROR }

/** Constancia de servicio técnico (tabla `novedades`, CLAUDE.md sección 5.1). */
@Entity(tableName = "novedades")
data class NovedadEntity(
    @PrimaryKey val idRegistro: String,
    val proyectoId: String,
    val nombreCliente: String,
    val nombreTecnico: String,
    val fechaServicio: Long,
    val horaInicio: String,
    val horaFin: String,
    val tipoServicio: String,
    val tipoActividad: String,
    val serialEquipo: String,
    val descServAsignado: String,
    val estadoAtencion: String,
    val descServRealizado: String,
    val motivoNoResuelta: String?,
    val observacionesRecomendaciones: String?,
    val nombreRepresentanteCliente: String,
    val cedulaRepresentanteCliente: String,
    val rolRepresentanteCliente: String,
    val rutaFirmaTecnico: String,
    val rutaFirmaRepresentante: String,
    val estadoSync: EstadoSync,
    val fechaCreacion: Long,
    val fechaModificacion: Long,
    val fechaSincronizacion: Long?,
    val mensajeError: String?,
)

/** Foto de evidencia (tabla `evidencias`, sección 5.2). Se borra en cascada con su novedad. */
@Entity(
    tableName = "evidencias",
    foreignKeys = [
        ForeignKey(
            entity = NovedadEntity::class,
            parentColumns = ["idRegistro"],
            childColumns = ["idRegistro"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("idRegistro")],
)
data class EvidenciaEntity(
    @PrimaryKey val idEvidencia: String,
    val idRegistro: String,
    val rutaArchivo: String,
    val tamanoBytes: Long,
    val hashSha256: String,
    val ancho: Int,
    val alto: Int,
    val fechaCaptura: Long,
)

/** Catálogo local de proyectos (sección 5.3), cargado desde assets/proyectos.json. */
@Entity(tableName = "proyectos")
data class ProyectoEntity(
    @PrimaryKey val proyectoId: String,
    val nombreProyecto: String,
    val nombreCliente: String,
)

/** Contadores para la pantalla de inicio. */
data class Conteos(
    val total: Int,
    val pendientes: Int,
    val sincronizados: Int,
    val conError: Int,
) {
    val porSincronizar: Int get() = pendientes + conError
}
