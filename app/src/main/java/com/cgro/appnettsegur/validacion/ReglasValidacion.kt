package com.cgro.appnettsegur.validacion

import com.cgro.appnettsegur.config.Catalogos
import com.cgro.appnettsegur.config.Limites
import com.cgro.appnettsegur.config.Mensajes
import java.io.File
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

/**
 * Campos del formulario que pueden marcarse en error.
 * El orden es el mismo de la pantalla: así se hace scroll al PRIMER campo con error.
 */
enum class Campo {
    PROYECTO, TIPO_SERVICIO, TIPO_ACTIVIDAD, SERIAL,
    HORA_INICIO, DESC_ASIGNADO, ESTADO_ATENCION, DESC_REALIZADO, MOTIVO,
    EVIDENCIAS,
    NOMBRE_REPRESENTANTE, CEDULA, ROL,
    FIRMA_TECNICO, FIRMA_REPRESENTANTE,
}

/** Datos del formulario en el momento de pulsar GUARDAR. */
data class DatosFormulario(
    val proyectoId: String,
    val tipoServicio: String,
    val tipoActividad: String,
    val serialEquipo: String,
    val horaInicio: String,
    val horaFin: String,
    val fechaServicio: Long,
    val descServAsignado: String,
    val estadoAtencion: String,
    val descServRealizado: String,
    val motivoNoResuelta: String,
    val nombreRepresentante: String,
    val cedulaRepresentante: String,
    val rolRepresentante: String,
    val evidencias: List<File>,
    val firmaTecnicoValida: Boolean,
    val firmaRepresentanteValida: Boolean,
    val ahora: Long,
)

data class ResultadoValidacion(val errores: Map<Campo, String>) {
    val esValido: Boolean get() = errores.isEmpty()
    val primerCampoConError: Campo? get() = errores.keys.minByOrNull { it.ordinal }
}

/**
 * Reglas R1–R6 (CLAUDE.md, sección 6). Cada regla devuelve el mensaje de error
 * o null si se cumple. Se ejecutan en el ViewModel al pulsar GUARDAR.
 */
object ReglasValidacion {

    private val regexSerial = Regex(Limites.REGEX_SERIAL)
    private val regexCedula = Regex("^\\d{${Limites.MIN_CEDULA},${Limites.MAX_CEDULA}}$")

    /** R1: si la atención NO quedó resuelta, el motivo es obligatorio y tiene ≥ 20 caracteres (sin espacios de los extremos). */
    fun r1MotivoNoResuelta(estadoAtencion: String, motivo: String): String? {
        if (estadoAtencion != Catalogos.NO_RESUELTA) return null
        return if (motivo.trim().length >= Limites.MIN_MOTIVO_NO_RESUELTA) null else Mensajes.R1_MOTIVO
    }

    /** R2 (horas): la hora de inicio debe ser estrictamente anterior a la de finalización. */
    fun r2Horas(horaInicio: String, horaFin: String): String? {
        val inicio = parsearHora(horaInicio) ?: return Mensajes.R2_HORAS
        val fin = parsearHora(horaFin) ?: return Mensajes.R2_HORAS
        return if (inicio.isBefore(fin)) null else Mensajes.R2_HORAS
    }

    /** R2 (fecha): la fecha del servicio no puede ser un día posterior a hoy. */
    fun r2Fecha(fechaServicio: Long, ahora: Long, zona: ZoneId = ZoneId.systemDefault()): String? {
        val diaServicio = Instant.ofEpochMilli(fechaServicio).atZone(zona).toLocalDate()
        val hoy = Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()
        return if (diaServicio.isAfter(hoy)) Mensajes.R2_FECHA_FUTURA else null
    }

    /** R3: la cédula tiene solo dígitos, entre 6 y 10. */
    fun r3Cedula(cedula: String): String? =
        if (regexCedula.matches(cedula.trim())) null else Mensajes.R3_CEDULA

    /** R4: serial alfanumérico de 4 a 20 caracteres, sin espacios internos. */
    fun r4Serial(serial: String): String? =
        if (regexSerial.matches(serial.trim())) null else Mensajes.R4_SERIAL

    /**
     * R5: si la atención quedó RESUELTA, la descripción de lo realizado tiene ≥ 30 caracteres
     * y es distinta de lo asignado (se compara sin mayúsculas ni espacios).
     */
    fun r5DescRealizado(estadoAtencion: String, realizado: String, asignado: String): String? {
        if (estadoAtencion != Catalogos.RESUELTA) return null
        val suficiente = realizado.trim().length >= Limites.MIN_DESC_REALIZADO
        val distinto = normalizar(realizado) != normalizar(asignado)
        return if (suficiente && distinto) null else Mensajes.R5_DESC_REALIZADO
    }

    /** R6 (fotos): entre 1 y 3 evidencias, y cada archivo existe y pesa más de 0 bytes. */
    fun r6Evidencias(evidencias: List<File>): String? = when {
        evidencias.size < Limites.MIN_EVIDENCIAS -> Mensajes.R6_SIN_FOTO
        evidencias.size > Limites.MAX_EVIDENCIAS -> Mensajes.R6_MAX_FOTOS
        evidencias.any { !it.exists() || it.length() <= 0L } -> Mensajes.R6_FOTO_NO_DISPONIBLE
        else -> null
    }

    /** R6 (firma del técnico): debe existir y no estar vacía. */
    fun r6FirmaTecnico(valida: Boolean): String? = if (valida) null else Mensajes.R6_FIRMA_TECNICO

    /** R6 (firma del representante): debe existir y no estar vacía. */
    fun r6FirmaRepresentante(valida: Boolean): String? = if (valida) null else Mensajes.R6_FIRMA_REPRESENTANTE

    /** Ejecuta los obligatorios y R1–R6. Si un campo obligatorio está vacío, ese mensaje tiene prioridad. */
    fun validar(d: DatosFormulario): ResultadoValidacion {
        val errores = linkedMapOf<Campo, String>()

        fun requerido(campo: Campo, valor: String, regla: () -> String? = { null }) {
            val error = if (valor.isBlank()) Mensajes.CAMPO_OBLIGATORIO else regla()
            if (error != null) errores[campo] = error
        }

        requerido(Campo.PROYECTO, d.proyectoId)
        requerido(Campo.TIPO_SERVICIO, d.tipoServicio)
        requerido(Campo.TIPO_ACTIVIDAD, d.tipoActividad)
        requerido(Campo.SERIAL, d.serialEquipo) { r4Serial(d.serialEquipo) }
        requerido(Campo.HORA_INICIO, d.horaInicio) {
            r2Fecha(d.fechaServicio, d.ahora) ?: r2Horas(d.horaInicio, d.horaFin)
        }
        requerido(Campo.DESC_ASIGNADO, d.descServAsignado)
        requerido(Campo.ESTADO_ATENCION, d.estadoAtencion)
        requerido(Campo.DESC_REALIZADO, d.descServRealizado) {
            r5DescRealizado(d.estadoAtencion, d.descServRealizado, d.descServAsignado)
        }
        r1MotivoNoResuelta(d.estadoAtencion, d.motivoNoResuelta)?.let { errores[Campo.MOTIVO] = it }
        r6Evidencias(d.evidencias)?.let { errores[Campo.EVIDENCIAS] = it }
        requerido(Campo.NOMBRE_REPRESENTANTE, d.nombreRepresentante)
        requerido(Campo.CEDULA, d.cedulaRepresentante) { r3Cedula(d.cedulaRepresentante) }
        requerido(Campo.ROL, d.rolRepresentante)
        r6FirmaTecnico(d.firmaTecnicoValida)?.let { errores[Campo.FIRMA_TECNICO] = it }
        r6FirmaRepresentante(d.firmaRepresentanteValida)?.let { errores[Campo.FIRMA_REPRESENTANTE] = it }

        return ResultadoValidacion(errores)
    }

    private fun parsearHora(hora: String): LocalTime? = try {
        LocalTime.parse(hora.trim())
    } catch (_: DateTimeParseException) {
        null
    }

    private fun normalizar(texto: String) = texto.lowercase().filterNot { it.isWhitespace() }
}
