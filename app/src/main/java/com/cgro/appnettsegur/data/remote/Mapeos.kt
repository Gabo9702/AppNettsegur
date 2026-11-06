package com.cgro.appnettsegur.data.remote

import com.cgro.appnettsegur.data.model.Carta
import com.cgro.appnettsegur.data.model.Documento
import com.cgro.appnettsegur.data.model.DocumentoCumplimiento
import com.cgro.appnettsegur.data.model.Empleado
import com.cgro.appnettsegur.data.model.EstadoCumplimiento
import com.cgro.appnettsegur.data.model.EstadoProyecto
import com.cgro.appnettsegur.data.model.EvidenciaRemota
import com.cgro.appnettsegur.data.model.Hito
import com.cgro.appnettsegur.data.model.NovedadRemota
import com.cgro.appnettsegur.data.model.PersonalAsignado
import com.cgro.appnettsegur.data.model.Proyecto
import com.cgro.appnettsegur.data.model.TipoCumplimiento
import com.cgro.appnettsegur.data.model.TipoDocumento
import com.google.firebase.database.DataSnapshot
import java.time.LocalDate

/**
 * Conversión de DataSnapshot a los modelos de dominio (M1, fase C).
 *
 * Se hace a mano (no con getValue(Clase)) porque los modelos tienen enums y LocalDate, que el
 * mapeo automático de Firebase no entiende. Los números enteros llegan como Long desde RTDB.
 */

// ---- Lectores seguros de campos ----
internal fun DataSnapshot.texto(clave: String): String = child(clave).getValue(String::class.java).orEmpty()
internal fun DataSnapshot.textoONulo(clave: String): String? =
    child(clave).getValue(String::class.java)?.takeIf { it.isNotBlank() }
internal fun DataSnapshot.entero(clave: String): Int = (child(clave).getValue(Long::class.java) ?: 0L).toInt()
internal fun DataSnapshot.largo(clave: String): Long = child(clave).getValue(Long::class.java) ?: 0L
internal fun DataSnapshot.largoONulo(clave: String): Long? = child(clave).getValue(Long::class.java)
internal fun DataSnapshot.booleano(clave: String): Boolean = child(clave).getValue(Boolean::class.java) ?: false

// ---- Proyecto ----
fun DataSnapshot.aProyecto(): Proyecto? {
    val id = key ?: return null
    if (!exists()) return null
    val hitos = child("hitos").children.mapNotNull { it.aHito() }.sortedBy { it.orden }
    return Proyecto(
        proyectoId = id,
        nombreProyecto = texto("nombreProyecto"),
        nombreCliente = texto("nombreCliente"),
        clienteUid = texto("clienteUid"),
        ingenieroUid = texto("ingenieroUid"),
        estado = runCatching { EstadoProyecto.valueOf(texto("estado")) }.getOrDefault(EstadoProyecto.EN_EJECUCION),
        avancePorcentaje = entero("avancePorcentaje"),
        hitos = hitos,
    )
}

fun DataSnapshot.aHito(): Hito? {
    val id = key ?: return null
    return Hito(
        hitoId = id,
        nombre = texto("nombre"),
        fechaInicio = largo("fechaInicio"),
        fechaFin = largo("fechaFin"),
        completado = booleano("completado"),
        orden = entero("orden"),
    )
}

fun DataSnapshot.aPersonalAsignado(): PersonalAsignado? {
    val id = key ?: return null
    return PersonalAsignado(
        empleadoId = id,
        nombre = texto("nombre"),
        cedula = texto("cedula"),
        estadoCumplimiento = runCatching { EstadoCumplimiento.valueOf(texto("estadoCumplimiento")) }
            .getOrDefault(EstadoCumplimiento.VENCIDO),
    )
}

// ---- Documento ----
fun DataSnapshot.aDocumento(proyectoId: String): Documento? {
    val id = key ?: return null
    val tipo = runCatching { TipoDocumento.valueOf(texto("tipo")) }.getOrNull() ?: return null
    return Documento(
        documentoId = id,
        proyectoId = proyectoId,
        tipo = tipo,
        nombre = texto("nombre"),
        archivoId = texto("archivoId"),
        mime = texto("mime"),
        tamanoBytes = largo("tamanoBytes"),
        subidoPor = texto("subidoPor"),
        fecha = largo("fecha"),
    )
}

// ---- Empleado ----
fun DataSnapshot.aEmpleado(): Empleado? {
    val id = key ?: return null
    val cumplimiento = TipoCumplimiento.entries.mapNotNull { tipo ->
        val nodo = child("cumplimiento").child(tipo.name)
        if (!nodo.exists()) return@mapNotNull null
        val expedicion = runCatching { LocalDate.parse(nodo.texto("fechaExpedicion")) }.getOrNull()
        val vencimiento = runCatching { LocalDate.parse(nodo.texto("fechaVencimiento")) }.getOrNull()
        if (expedicion == null || vencimiento == null) return@mapNotNull null
        tipo to DocumentoCumplimiento(tipo, expedicion, vencimiento, nodo.texto("archivoId"))
    }.toMap()
    return Empleado(
        empleadoId = id,
        nombre = texto("nombre"),
        cedula = texto("cedula"),
        cargo = texto("cargo"),
        uid = textoONulo("uid"),
        cumplimiento = cumplimiento,
        carneArchivoId = child("carne").textoONulo("archivoId"),
        carneFecha = child("carne").largoONulo("fecha"),
    )
}

// ---- Carta ----
fun DataSnapshot.aCarta(): Carta? {
    val id = key ?: return null
    val empleados = child("empleados").children.mapNotNull { if (it.getValue(Boolean::class.java) == true) it.key else null }
    return Carta(
        cartaId = id,
        proyectoId = texto("proyectoId"),
        empleadoIds = empleados,
        archivoId = texto("archivoId"),
        generadaPor = texto("generadaPor"),
        fecha = largo("fecha"),
    )
}

// ---- Novedad vista por cliente/ingeniero ----
fun DataSnapshot.aNovedadRemota(): NovedadRemota? {
    val id = textoONulo("idRegistro") ?: key ?: return null
    val evidencias = child("evidencias").children.map {
        EvidenciaRemota(
            base64 = it.texto("base64"),
            ancho = it.entero("ancho"),
            alto = it.entero("alto"),
            hashSha256 = it.texto("hashSha256"),
        )
    }
    return NovedadRemota(
        idRegistro = id,
        proyectoId = texto("proyectoId"),
        nombreCliente = texto("nombreCliente"),
        nombreTecnico = texto("nombreTecnico"),
        uidTecnico = texto("uidTecnico"),
        fechaServicio = largo("fechaServicio"),
        horaInicio = texto("horaInicio"),
        horaFin = texto("horaFin"),
        tipoServicio = texto("tipoServicio"),
        tipoActividad = texto("tipoActividad"),
        serialEquipo = texto("serialEquipo"),
        descServAsignado = texto("descServAsignado"),
        estadoAtencion = texto("estadoAtencion"),
        descServRealizado = texto("descServRealizado"),
        motivoNoResuelta = textoONulo("motivoNoResuelta"),
        observacionesRecomendaciones = textoONulo("observacionesRecomendaciones"),
        nombreRepresentanteCliente = texto("nombreRepresentanteCliente"),
        cedulaRepresentanteCliente = texto("cedulaRepresentanteCliente"),
        rolRepresentanteCliente = texto("rolRepresentanteCliente"),
        fechaCreacion = largo("fechaCreacion"),
        evidencias = evidencias,
        firmaTecnicoBase64 = child("firmas").textoONulo("tecnico"),
        firmaRepresentanteBase64 = child("firmas").textoONulo("representante"),
        fechaSincronizacionServidor = largoONulo("fechaSincronizacionServidor"),
    )
}
