package com.cgro.appnettsegur.data.model

import com.cgro.appnettsegur.config.Limites
import com.cgro.appnettsegur.config.Rol
import java.time.LocalDate

/**
 * Modelos de dominio de los módulos nuevos (MODULOS.md, secciones 4 y 6.2).
 *
 * Estos datos viven EN LÍNEA (Firebase RTDB) y no se guardan en Room (sección 5.4):
 * solo el flujo del técnico usa Room. Las fechas de día (expedición/vencimiento) se modelan
 * con LocalDate; las marcas de tiempo (fecha de creación, de notificación) con Long epoch ms.
 */

// ---------------------------------------------------------------------------
// Usuario y sesión
// ---------------------------------------------------------------------------

/** usuarios/{uid}. El empleadoId solo lo tienen los técnicos. */
data class Usuario(
    val uid: String,
    val nombre: String,
    val correo: String,
    val rol: Rol,
    val empleadoId: String? = null,
)

// ---------------------------------------------------------------------------
// Proyectos
// ---------------------------------------------------------------------------

enum class EstadoProyecto { EN_EJECUCION, FINALIZADO }

/** proyectos/{proyectoId}. El avance se calcula a partir de los hitos completados. */
data class Proyecto(
    val proyectoId: String,
    val nombreProyecto: String,
    val nombreCliente: String,
    val clienteUid: String,
    val ingenieroUid: String,
    val estado: EstadoProyecto = EstadoProyecto.EN_EJECUCION,
    val avancePorcentaje: Int = 0,
    val hitos: List<Hito> = emptyList(),
)

data class Hito(
    val hitoId: String,
    val nombre: String,
    val fechaInicio: Long,
    val fechaFin: Long,
    val completado: Boolean,
    val orden: Int,
)

/** Vista reducida del personal que el cliente sí puede leer (sección 4, nota). */
data class PersonalAsignado(
    val empleadoId: String,
    val nombre: String,
    val cedula: String,
    val estadoCumplimiento: EstadoCumplimiento,
)

// ---------------------------------------------------------------------------
// Documentos y archivos
// ---------------------------------------------------------------------------

enum class TipoDocumento { COTIZACION, CRONOGRAMA, INFORME_AVANCE, ACTA_ENTREGA, INFORME_FINAL }

/** documentos/{proyectoId}/{documentoId}: solo metadatos (el contenido va en archivos/). */
data class Documento(
    val documentoId: String,
    val proyectoId: String,
    val tipo: TipoDocumento,
    val nombre: String,
    val archivoId: String,
    val mime: String,
    val tamanoBytes: Long,
    val subidoPor: String,
    val fecha: Long,
)

/** archivos/{archivoId}: contenido pesado (Base64) separado de los metadatos. */
data class Archivo(
    val archivoId: String,
    val base64: String,
    val mime: String,
    val nombre: String,
    val tamanoBytes: Long,
    val subidoPor: String,
    val fecha: Long,
)

// ---------------------------------------------------------------------------
// Empleados y cumplimiento
// ---------------------------------------------------------------------------

enum class TipoCumplimiento { ARL, SEGURIDAD_SOCIAL, ALTURAS }

enum class EstadoCumplimiento { VIGENTE, POR_VENCER, VENCIDO }

/** Un documento de cumplimiento con su vigencia. */
data class DocumentoCumplimiento(
    val tipo: TipoCumplimiento,
    val fechaExpedicion: LocalDate,
    val fechaVencimiento: LocalDate,
    val archivoId: String,
)

/** empleados/{empleadoId}. El estado y la habilitación NO se guardan: se calculan (sección 4). */
data class Empleado(
    val empleadoId: String,
    val nombre: String,
    val cedula: String,
    val cargo: String,
    val uid: String? = null,
    val cumplimiento: Map<TipoCumplimiento, DocumentoCumplimiento> = emptyMap(),
    val carneArchivoId: String? = null,
    val carneFecha: Long? = null,
) {
    /**
     * Estado de cumplimiento del empleado en la fecha dada: devuelve el PEOR de sus tres
     * documentos obligatorios. Si falta alguno, se considera VENCIDO.
     */
    fun estadoCumplimiento(hoy: LocalDate): EstadoCumplimiento {
        var resultado = EstadoCumplimiento.VIGENTE
        for (tipo in TipoCumplimiento.entries) {
            val doc = cumplimiento[tipo]
            val estado = when {
                doc == null -> EstadoCumplimiento.VENCIDO
                doc.fechaVencimiento.isBefore(hoy) -> EstadoCumplimiento.VENCIDO
                !doc.fechaVencimiento.isAfter(hoy.plusDays(Limites.DIAS_POR_VENCER)) -> EstadoCumplimiento.POR_VENCER
                else -> EstadoCumplimiento.VIGENTE
            }
            // El peor estado manda (VENCIDO > POR_VENCER > VIGENTE).
            if (estado.ordinal > resultado.ordinal) resultado = estado
        }
        return resultado
    }

    /** Habilitado para obra: sus tres documentos existen y ninguno está vencido (sección 4). */
    fun habilitado(hoy: LocalDate): Boolean = estadoCumplimiento(hoy) != EstadoCumplimiento.VENCIDO
}

// ---------------------------------------------------------------------------
// Cartas de presentación
// ---------------------------------------------------------------------------

/** cartas/{cartaId}: la carta en PDF (en archivos/) y los empleados que incluye. */
data class Carta(
    val cartaId: String,
    val proyectoId: String,
    val empleadoIds: List<String>,
    val archivoId: String,
    val generadaPor: String,
    val fecha: Long,
)

// ---------------------------------------------------------------------------
// Notificaciones
// ---------------------------------------------------------------------------

enum class TipoNotificacion { NOVEDAD_SINCRONIZADA, DOCUMENTO_NUEVO, CARTA_NUEVA }

/** notificaciones/{uidDestinatario}/{notificacionId}. Los destinatarios solo cambian `leida`. */
data class Notificacion(
    val notificacionId: String,
    val tipo: TipoNotificacion,
    val titulo: String,
    val mensaje: String,
    val proyectoId: String,
    val refId: String,
    val fecha: Long,
    val leida: Boolean = false,
)

// ---------------------------------------------------------------------------
// Novedad vista por cliente e ingeniero (lectura de novedades/{idRegistro})
// ---------------------------------------------------------------------------

/** Una evidencia tal como viaja a Firebase: miniatura Base64 + metadatos. */
data class EvidenciaRemota(
    val base64: String,
    val ancho: Int,
    val alto: Int,
    val hashSha256: String,
)

/** novedades/{idRegistro} leída por el cliente o el ingeniero del proyecto. */
data class NovedadRemota(
    val idRegistro: String,
    val proyectoId: String,
    val nombreCliente: String,
    val nombreTecnico: String,
    val uidTecnico: String,
    val fechaServicio: Long,
    val horaInicio: String,
    val horaFin: String,
    val tipoServicio: String,
    val tipoActividad: String,
    val serialEquipo: String,
    val descServAsignado: String,
    val estadoAtencion: String,
    val descServRealizado: String,
    val motivoNoResuelta: String? = null,
    val observacionesRecomendaciones: String? = null,
    val nombreRepresentanteCliente: String,
    val cedulaRepresentanteCliente: String,
    val rolRepresentanteCliente: String,
    val fechaCreacion: Long,
    val evidencias: List<EvidenciaRemota> = emptyList(),
    val firmaTecnicoBase64: String? = null,
    val firmaRepresentanteBase64: String? = null,
    val fechaSincronizacionServidor: Long? = null,
)
