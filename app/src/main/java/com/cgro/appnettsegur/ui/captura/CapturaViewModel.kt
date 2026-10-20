package com.cgro.appnettsegur.ui.captura

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cgro.appnettsegur.AppNettsegur
import com.cgro.appnettsegur.config.Catalogos
import com.cgro.appnettsegur.config.Limites
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.data.local.EstadoSync
import com.cgro.appnettsegur.data.local.NovedadEntity
import com.cgro.appnettsegur.data.local.ProyectoEntity
import com.cgro.appnettsegur.data.prefs.PerfilTecnicoStore
import com.cgro.appnettsegur.data.repository.EvidenciaFormulario
import com.cgro.appnettsegur.data.repository.NovedadRepository
import com.cgro.appnettsegur.navigation.Rutas
import com.cgro.appnettsegur.util.FirmaFormulario
import com.cgro.appnettsegur.util.ImagenUtils
import com.cgro.appnettsegur.validacion.Campo
import com.cgro.appnettsegur.validacion.DatosFormulario
import com.cgro.appnettsegur.validacion.ReglasValidacion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

data class CapturaUiState(
    val cargando: Boolean = true,
    val esEdicion: Boolean = false,
    /** true si se intentó editar un registro SINCRONIZADO (solo lectura). */
    val bloqueado: Boolean = false,
    val idRegistro: String = "",
    val nombreTecnico: String = "",
    val fechaServicio: Long = System.currentTimeMillis(),
    val proyectos: List<ProyectoEntity> = emptyList(),
    // Proyecto y servicio
    val proyectoId: String = "",
    val nombreCliente: String = "",
    val tipoServicio: String = "",
    val tipoActividad: String = "",
    val serialEquipo: String = "",
    // Atención
    val horaInicio: String = "",
    val descServAsignado: String = "",
    val estadoAtencion: String = "",
    val descServRealizado: String = "",
    val motivoNoResuelta: String = "",
    // Observaciones
    val observaciones: String = "",
    // Evidencia
    val evidencias: List<EvidenciaFormulario> = emptyList(),
    val procesandoImagen: Boolean = false,
    // Representante
    val nombreRepresentante: String = "",
    val cedulaRepresentante: String = "",
    val rolRepresentante: String = "",
    // Firmas
    val firmaTecnico: FirmaFormulario = FirmaFormulario(),
    val firmaRepresentante: FirmaFormulario = FirmaFormulario(),
    // Control
    val errores: Map<Campo, String> = emptyMap(),
    val hayCambios: Boolean = false,
    val guardando: Boolean = false,
) {
    val puedeAgregarFoto: Boolean get() = evidencias.size < Limites.MAX_EVIDENCIAS && !procesandoImagen
}

sealed interface EventoCaptura {
    data object Guardado : EventoCaptura
    data class Mensaje(val texto: String) : EventoCaptura
    data class IrACampo(val campo: Campo) : EventoCaptura
}

class CapturaViewModel(
    private val app: Application,
    private val repositorio: NovedadRepository,
    private val perfil: PerfilTecnicoStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val idEdicion: String? = savedStateHandle[Rutas.ARG_ID]
    private var original: NovedadEntity? = null
    private var guardadoConExito = false

    private val _estado = MutableStateFlow(CapturaUiState())
    val estado: StateFlow<CapturaUiState> = _estado.asStateFlow()

    private val _eventos = Channel<EventoCaptura>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    /** URI donde la cámara está escribiendo la foto (sobrevive a recrear la Activity). */
    var uriCamaraPendiente: Uri? = null

    init {
        viewModelScope.launch {
            repositorio.observarProyectos().collect { lista -> _estado.update { it.copy(proyectos = lista) } }
        }
        viewModelScope.launch { cargar() }
    }

    private suspend fun cargar() {
        if (idEdicion == null) {
            _estado.update {
                it.copy(cargando = false, idRegistro = UUID.randomUUID().toString(), nombreTecnico = perfil.obtenerNombre())
            }
            return
        }
        val novedad = repositorio.obtenerNovedad(idEdicion)
        if (novedad == null || novedad.estadoSync == EstadoSync.SINCRONIZADO) {
            // Un registro SINCRONIZADO es de solo lectura.
            _estado.update { it.copy(cargando = false, esEdicion = true, bloqueado = true) }
            return
        }
        original = novedad
        val evidencias = repositorio.obtenerEvidencias(novedad.idRegistro).map {
            EvidenciaFormulario(it.idEvidencia, it.rutaArchivo, it.tamanoBytes, it.hashSha256, it.ancho, it.alto, it.fechaCaptura, esTemporal = false)
        }
        _estado.update {
            it.copy(
                cargando = false,
                esEdicion = true,
                idRegistro = novedad.idRegistro,
                nombreTecnico = novedad.nombreTecnico,
                fechaServicio = novedad.fechaServicio,
                proyectoId = novedad.proyectoId,
                nombreCliente = novedad.nombreCliente,
                tipoServicio = novedad.tipoServicio,
                tipoActividad = novedad.tipoActividad,
                serialEquipo = novedad.serialEquipo,
                horaInicio = novedad.horaInicio,
                descServAsignado = novedad.descServAsignado,
                estadoAtencion = novedad.estadoAtencion,
                descServRealizado = novedad.descServRealizado,
                motivoNoResuelta = novedad.motivoNoResuelta.orEmpty(),
                observaciones = novedad.observacionesRecomendaciones.orEmpty(),
                evidencias = evidencias,
                nombreRepresentante = novedad.nombreRepresentanteCliente,
                cedulaRepresentante = novedad.cedulaRepresentanteCliente,
                rolRepresentante = novedad.rolRepresentanteCliente,
                firmaTecnico = FirmaFormulario(rutaExistente = novedad.rutaFirmaTecnico),
                firmaRepresentante = FirmaFormulario(rutaExistente = novedad.rutaFirmaRepresentante),
            )
        }
    }

    /** Aplica un cambio del usuario, limpia el error de ese campo y marca que hay cambios sin guardar. */
    private fun editar(campo: Campo?, cambio: (CapturaUiState) -> CapturaUiState) {
        _estado.update { actual ->
            val nuevo = cambio(actual)
            nuevo.copy(hayCambios = true, errores = if (campo == null) nuevo.errores else nuevo.errores - campo)
        }
    }

    fun onProyecto(proyecto: ProyectoEntity) = editar(Campo.PROYECTO) {
        it.copy(proyectoId = proyecto.proyectoId, nombreCliente = proyecto.nombreCliente)
    }
    fun onTipoServicio(valor: String) = editar(Campo.TIPO_SERVICIO) { it.copy(tipoServicio = valor) }
    fun onTipoActividad(valor: String) = editar(Campo.TIPO_ACTIVIDAD) { it.copy(tipoActividad = valor) }
    fun onSerial(valor: String) = editar(Campo.SERIAL) { it.copy(serialEquipo = valor) }
    fun onHoraInicio(valor: String) = editar(Campo.HORA_INICIO) { it.copy(horaInicio = valor) }
    fun onDescAsignado(valor: String) = editar(Campo.DESC_ASIGNADO) { it.copy(descServAsignado = valor) }
    fun onEstadoAtencion(valor: String) = editar(Campo.ESTADO_ATENCION) {
        it.copy(estadoAtencion = valor, errores = it.errores - Campo.MOTIVO - Campo.DESC_REALIZADO)
    }
    fun onDescRealizado(valor: String) = editar(Campo.DESC_REALIZADO) { it.copy(descServRealizado = valor) }
    fun onMotivo(valor: String) = editar(Campo.MOTIVO) { it.copy(motivoNoResuelta = valor) }
    fun onObservaciones(valor: String) = editar(null) { it.copy(observaciones = valor) }
    fun onNombreRepresentante(valor: String) = editar(Campo.NOMBRE_REPRESENTANTE) { it.copy(nombreRepresentante = valor) }
    fun onCedula(valor: String) = editar(Campo.CEDULA) { it.copy(cedulaRepresentante = valor) }
    fun onRol(valor: String) = editar(Campo.ROL) { it.copy(rolRepresentante = valor) }

    fun onFirmaTecnico(firma: FirmaFormulario) = actualizarFirma(Campo.FIRMA_TECNICO, firma) { s, f -> s.copy(firmaTecnico = f) }
    fun onFirmaRepresentante(firma: FirmaFormulario) =
        actualizarFirma(Campo.FIRMA_REPRESENTANTE, firma) { s, f -> s.copy(firmaRepresentante = f) }

    private fun actualizarFirma(
        campo: Campo,
        firma: FirmaFormulario,
        aplicar: (CapturaUiState, FirmaFormulario) -> CapturaUiState,
    ) {
        val anterior = if (campo == Campo.FIRMA_TECNICO) _estado.value.firmaTecnico else _estado.value.firmaRepresentante
        // Medir el tamaño del lienzo no es un cambio del usuario.
        val soloTamano = anterior.copy(anchoPx = firma.anchoPx, altoPx = firma.altoPx) == firma
        if (soloTamano) _estado.update { aplicar(it, firma) } else editar(campo) { aplicar(it, firma) }
    }

    /** Procesa una foto de la cámara o la galería y guarda una copia propia en la carpeta temporal. */
    fun agregarFoto(uri: Uri) {
        if (!_estado.value.puedeAgregarFoto) return
        _estado.update { it.copy(procesandoImagen = true) }
        viewModelScope.launch {
            try {
                val imagen = ImagenUtils.procesarEvidencia(app, uri)
                val evidencia = EvidenciaFormulario(
                    idEvidencia = UUID.randomUUID().toString(),
                    rutaArchivo = imagen.rutaArchivo,
                    tamanoBytes = imagen.tamanoBytes,
                    hashSha256 = imagen.hashSha256,
                    ancho = imagen.ancho,
                    alto = imagen.alto,
                    fechaCaptura = System.currentTimeMillis(),
                    esTemporal = true,
                )
                editar(Campo.EVIDENCIAS) { it.copy(evidencias = it.evidencias + evidencia) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _eventos.send(EventoCaptura.Mensaje(Mensajes.ERROR_IMAGEN))
            } finally {
                _estado.update { it.copy(procesandoImagen = false) }
                // La copia propia ya existe (o falló): el archivo de la cámara en cacheDir sobra.
                if (uri == uriCamaraPendiente) limpiarFotoCamara()
            }
        }
    }

    /** La foto de la cámara ya se copió (o se canceló): se borra el archivo de cacheDir. */
    fun limpiarFotoCamara() {
        uriCamaraPendiente?.lastPathSegment?.let { File(File(app.cacheDir, "camara"), it).delete() }
        uriCamaraPendiente = null
    }

    fun quitarFoto(evidencia: EvidenciaFormulario) {
        // Las temporales se borran ya; las guardadas se borran al GUARDAR (si se cancela, siguen ahí).
        if (evidencia.esTemporal) File(evidencia.rutaArchivo).delete()
        editar(Campo.EVIDENCIAS) { it.copy(evidencias = it.evidencias - evidencia) }
    }

    /** GUARDAR: valida R1–R6 y, solo si todo es válido, escribe en Room. Nunca toca Firebase. */
    fun guardar(grosorFirmaPx: Float) {
        val s = _estado.value
        if (s.guardando || s.bloqueado || s.cargando) return

        val ahora = System.currentTimeMillis()
        // horaFin = hora del PRIMER guardar; al editar se conserva la original.
        val horaFin = original?.horaFin ?: Instant.ofEpochMilli(ahora).atZone(ZoneId.systemDefault()).format(formatoHora)
        val fechaServicio = original?.fechaServicio ?: ahora

        val resultado = ReglasValidacion.validar(
            DatosFormulario(
                proyectoId = s.proyectoId,
                tipoServicio = s.tipoServicio,
                tipoActividad = s.tipoActividad,
                serialEquipo = s.serialEquipo,
                horaInicio = s.horaInicio,
                horaFin = horaFin,
                fechaServicio = fechaServicio,
                descServAsignado = s.descServAsignado,
                estadoAtencion = s.estadoAtencion,
                descServRealizado = s.descServRealizado,
                motivoNoResuelta = s.motivoNoResuelta,
                nombreRepresentante = s.nombreRepresentante,
                cedulaRepresentante = s.cedulaRepresentante,
                rolRepresentante = s.rolRepresentante,
                evidencias = s.evidencias.map { File(it.rutaArchivo) },
                firmaTecnicoValida = s.firmaTecnico.esValida,
                firmaRepresentanteValida = s.firmaRepresentante.esValida,
                ahora = ahora,
            )
        )

        if (!resultado.esValido) {
            // No se escribe nada y no se borra lo digitado: solo se marcan los campos.
            _estado.update { it.copy(errores = resultado.errores) }
            viewModelScope.launch {
                resultado.primerCampoConError?.let { _eventos.send(EventoCaptura.IrACampo(it)) }
                _eventos.send(EventoCaptura.Mensaje(Mensajes.HAY_ERRORES))
            }
            return
        }

        val novedad = NovedadEntity(
            idRegistro = s.idRegistro,
            proyectoId = s.proyectoId,
            nombreCliente = s.nombreCliente,
            nombreTecnico = s.nombreTecnico,
            fechaServicio = fechaServicio,
            horaInicio = s.horaInicio,
            horaFin = horaFin,
            tipoServicio = s.tipoServicio,
            tipoActividad = s.tipoActividad,
            serialEquipo = s.serialEquipo.trim().uppercase(), // R4: se guarda en mayúsculas
            descServAsignado = s.descServAsignado.trim(),
            estadoAtencion = s.estadoAtencion,
            descServRealizado = s.descServRealizado.trim(),
            motivoNoResuelta = s.motivoNoResuelta.trim()
                .takeIf { s.estadoAtencion == Catalogos.NO_RESUELTA && it.isNotEmpty() },
            observacionesRecomendaciones = s.observaciones.trim().ifEmpty { null },
            nombreRepresentanteCliente = s.nombreRepresentante.trim(),
            cedulaRepresentanteCliente = s.cedulaRepresentante.trim(),
            rolRepresentanteCliente = s.rolRepresentante.trim(),
            rutaFirmaTecnico = "", // el repositorio asigna la ruta del PNG
            rutaFirmaRepresentante = "",
            // Un registro editado (aunque estuviera en ERROR) vuelve a quedar PENDIENTE de enviar.
            estadoSync = EstadoSync.PENDIENTE,
            fechaCreacion = original?.fechaCreacion ?: ahora,
            fechaModificacion = ahora,
            fechaSincronizacion = null,
            mensajeError = null,
        )

        _estado.update { it.copy(guardando = true) }
        viewModelScope.launch {
            try {
                repositorio.guardar(novedad, s.evidencias, s.firmaTecnico, s.firmaRepresentante, grosorFirmaPx)
                guardadoConExito = true
                _estado.update { it.copy(guardando = false, hayCambios = false) }
                _eventos.send(EventoCaptura.Guardado)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _estado.update { it.copy(guardando = false) }
                _eventos.send(EventoCaptura.Mensaje(Mensajes.ERROR_GUARDANDO))
            }
        }
    }

    /** Si el usuario sale sin guardar, se borran las fotos temporales de este formulario. */
    override fun onCleared() {
        if (!guardadoConExito) {
            _estado.value.evidencias.filter { it.esTemporal }.forEach { File(it.rutaArchivo).delete() }
        }
        limpiarFotoCamara()
    }

    companion object {
        private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppNettsegur
                CapturaViewModel(app, app.container.novedadRepository, app.container.perfilStore, createSavedStateHandle())
            }
        }
    }
}
