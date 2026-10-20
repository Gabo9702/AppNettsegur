package com.cgro.appnettsegur.ui.captura

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.cgro.appnettsegur.config.Catalogos
import com.cgro.appnettsegur.config.Limites
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.data.repository.EvidenciaFormulario
import com.cgro.appnettsegur.ui.componentes.CampoTexto
import com.cgro.appnettsegur.ui.componentes.DatoLectura
import com.cgro.appnettsegur.ui.componentes.DialogoConfirmacion
import com.cgro.appnettsegur.ui.componentes.FirmaPad
import com.cgro.appnettsegur.ui.componentes.GROSOR_FIRMA
import com.cgro.appnettsegur.ui.componentes.LocalMensajero
import com.cgro.appnettsegur.ui.componentes.SelectorLista
import com.cgro.appnettsegur.ui.componentes.TarjetaSeccion
import com.cgro.appnettsegur.ui.componentes.TextoError
import com.cgro.appnettsegur.util.Formatos
import com.cgro.appnettsegur.util.ImagenUtils
import com.cgro.appnettsegur.validacion.Campo
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CapturaScreen(
    onVolver: () -> Unit,
    onGuardado: () -> Unit,
    viewModel: CapturaViewModel = viewModel(factory = CapturaViewModel.Factory),
) {
    val s by viewModel.estado.collectAsStateWithLifecycle()
    val mensajero = LocalMensajero.current
    val contexto = LocalContext.current
    val grosorFirmaPx = with(LocalDensity.current) { GROSOR_FIRMA.toPx() }
    val scroll = rememberScrollState()
    val alcance = rememberCoroutineScope()
    // Un "ancla" por campo para poder hacer scroll al primer campo con error.
    val anclas = remember { Campo.entries.associateWith { BringIntoViewRequester() } }
    var confirmarSalida by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                EventoCaptura.Guardado -> {
                    mensajero(Mensajes.GUARDADO)
                    onGuardado()
                }
                is EventoCaptura.Mensaje -> mensajero(evento.texto)
                is EventoCaptura.IrACampo -> alcance.launch { anclas.getValue(evento.campo).bringIntoView() }
            }
        }
    }

    val salir = { if (s.hayCambios) confirmarSalida = true else onVolver() }
    BackHandler(enabled = s.hayCambios) { confirmarSalida = true }

    // Cámara: la app de cámara escribe en un archivo de cacheDir expuesto con FileProvider.
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        val uri = viewModel.uriCamaraPendiente
        if (exito && uri != null) viewModel.agregarFoto(uri) else viewModel.limpiarFotoCamara()
    }
    // Galería: Photo Picker, no requiere permisos de almacenamiento.
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.agregarFoto(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (s.esEdicion) "Editar registro" else "Nuevo registro") },
                navigationIcon = {
                    IconButton(onClick = salir) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        when {
            s.cargando -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            s.bloqueado -> Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(Mensajes.SINCRONIZADO_NO_EDITABLE, style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = onVolver) { Text("Volver") }
                }
            }
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(scroll)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ---------- Datos automáticos ----------
                TarjetaSeccion(titulo = null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        DatoLectura("Técnico", s.nombreTecnico, Modifier.weight(1f))
                        DatoLectura("Fecha", Formatos.fecha(s.fechaServicio), Modifier.weight(1f))
                    }
                    DatoLectura("ID del registro", Formatos.idCorto(s.idRegistro, Limites.CARACTERES_ID_CORTO))
                }

                // ---------- Proyecto y servicio ----------
                TarjetaSeccion("Proyecto y servicio", icono = Icons.Outlined.WorkOutline) {
                    SelectorLista(
                        etiqueta = "Proyecto *",
                        opciones = s.proyectos,
                        seleccion = s.proyectos.firstOrNull { it.proyectoId == s.proyectoId },
                        textoDe = { it.nombreProyecto },
                        onSeleccion = viewModel::onProyecto,
                        error = s.errores[Campo.PROYECTO],
                        modifier = Modifier.bringIntoViewRequester(anclas.getValue(Campo.PROYECTO)),
                    )
                    CampoTexto(
                        valor = s.nombreCliente,
                        onCambio = {},
                        etiqueta = "Cliente",
                        soloLectura = true,
                        ayuda = "Se llena con el proyecto seleccionado",
                    )
                    SelectorLista(
                        etiqueta = "Tipo de servicio *",
                        opciones = Catalogos.tiposServicio,
                        seleccion = s.tipoServicio.ifEmpty { null },
                        textoDe = { it },
                        onSeleccion = viewModel::onTipoServicio,
                        error = s.errores[Campo.TIPO_SERVICIO],
                        modifier = Modifier.bringIntoViewRequester(anclas.getValue(Campo.TIPO_SERVICIO)),
                    )
                    SelectorLista(
                        etiqueta = "Tipo de actividad *",
                        opciones = Catalogos.tiposActividad,
                        seleccion = s.tipoActividad.ifEmpty { null },
                        textoDe = { it },
                        onSeleccion = viewModel::onTipoActividad,
                        error = s.errores[Campo.TIPO_ACTIVIDAD],
                        modifier = Modifier.bringIntoViewRequester(anclas.getValue(Campo.TIPO_ACTIVIDAD)),
                    )
                    CampoTexto(
                        valor = s.serialEquipo,
                        onCambio = viewModel::onSerial,
                        etiqueta = "Serial del equipo *",
                        error = s.errores[Campo.SERIAL],
                        ayuda = "Letras y números, sin espacios (4 a 20)",
                        teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.bringIntoViewRequester(anclas.getValue(Campo.SERIAL)),
                    )
                }

                // ---------- Atención ----------
                TarjetaSeccion("Atención", icono = Icons.Outlined.Build) {
                    SelectorHora(
                        hora = s.horaInicio,
                        onHora = viewModel::onHoraInicio,
                        error = s.errores[Campo.HORA_INICIO],
                        modifier = Modifier.bringIntoViewRequester(anclas.getValue(Campo.HORA_INICIO)),
                    )
                    CampoTexto(
                        valor = s.descServAsignado,
                        onCambio = viewModel::onDescAsignado,
                        etiqueta = "Servicio asignado *",
                        ayuda = "Lo que se pidió hacer (diagnóstico o asignación)",
                        lineas = 3,
                        error = s.errores[Campo.DESC_ASIGNADO],
                        modifier = Modifier.bringIntoViewRequester(anclas.getValue(Campo.DESC_ASIGNADO)),
                    )
                    Column(
                        Modifier
                            .selectableGroup()
                            .bringIntoViewRequester(anclas.getValue(Campo.ESTADO_ATENCION))
                    ) {
                        Text("Estado de la atención *", style = MaterialTheme.typography.labelLarge)
                        Catalogos.estadosAtencion.forEach { (valor, texto) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = s.estadoAtencion == valor,
                                        onClick = { viewModel.onEstadoAtencion(valor) },
                                        role = Role.RadioButton,
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = s.estadoAtencion == valor, onClick = null)
                                Text(texto, Modifier.padding(start = 8.dp))
                            }
                        }
                        TextoError(s.errores[Campo.ESTADO_ATENCION])
                    }
                    CampoTexto(
                        valor = s.descServRealizado,
                        onCambio = viewModel::onDescRealizado,
                        etiqueta = "Servicio realizado *",
                        ayuda = "Describa la solución aplicada (mínimo ${Limites.MIN_DESC_REALIZADO} caracteres si quedó resuelta)",
                        lineas = 3,
                        error = s.errores[Campo.DESC_REALIZADO],
                        modifier = Modifier.bringIntoViewRequester(anclas.getValue(Campo.DESC_REALIZADO)),
                    )
                    if (s.estadoAtencion == Catalogos.NO_RESUELTA) {
                        CampoTexto(
                            valor = s.motivoNoResuelta,
                            onCambio = viewModel::onMotivo,
                            etiqueta = "Motivo por el que no quedó resuelta *",
                            ayuda = "Mínimo ${Limites.MIN_MOTIVO_NO_RESUELTA} caracteres",
                            lineas = 3,
                            error = s.errores[Campo.MOTIVO],
                            modifier = Modifier.bringIntoViewRequester(anclas.getValue(Campo.MOTIVO)),
                        )
                    }
                }

                // ---------- Observaciones ----------
                TarjetaSeccion("Observaciones", icono = Icons.AutoMirrored.Outlined.Notes) {
                    CampoTexto(
                        valor = s.observaciones,
                        onCambio = viewModel::onObservaciones,
                        etiqueta = "Observaciones y recomendaciones (opcional)",
                        lineas = 3,
                    )
                }

                // ---------- Evidencia fotográfica ----------
                TarjetaSeccion(
                    "Evidencia fotográfica (${s.evidencias.size}/${Limites.MAX_EVIDENCIAS})",
                    icono = Icons.Outlined.PhotoCamera,
                    modifier = Modifier.bringIntoViewRequester(anclas.getValue(Campo.EVIDENCIAS)),
                ) {
                    if (s.evidencias.isEmpty() && !s.procesandoImagen) {
                        Text(
                            "Agregue entre ${Limites.MIN_EVIDENCIAS} y ${Limites.MAX_EVIDENCIAS} fotos del trabajo realizado.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        s.evidencias.forEachIndexed { i, evidencia ->
                            Miniatura(evidencia, numero = i + 1, onQuitar = { viewModel.quitarFoto(evidencia) })
                        }
                        if (s.procesandoImagen) {
                            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        }
                    }
                    TextoError(s.errores[Campo.EVIDENCIAS])
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val uri = ImagenUtils.crearArchivoCamara(contexto)
                                viewModel.uriCamaraPendiente = uri
                                camara.launch(uri)
                            },
                            enabled = s.puedeAgregarFoto,
                        ) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                            Text(" Cámara")
                        }
                        OutlinedButton(
                            onClick = {
                                galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            enabled = s.puedeAgregarFoto,
                        ) {
                            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                            Text(" Galería")
                        }
                    }
                }

                // ---------- Representante del cliente ----------
                TarjetaSeccion("Representante del cliente", icono = Icons.Outlined.Badge) {
                    CampoTexto(
                        valor = s.nombreRepresentante,
                        onCambio = viewModel::onNombreRepresentante,
                        etiqueta = "Nombre *",
                        error = s.errores[Campo.NOMBRE_REPRESENTANTE],
                        teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.bringIntoViewRequester(anclas.getValue(Campo.NOMBRE_REPRESENTANTE)),
                    )
                    CampoTexto(
                        valor = s.cedulaRepresentante,
                        onCambio = viewModel::onCedula,
                        etiqueta = "Cédula *",
                        error = s.errores[Campo.CEDULA],
                        ayuda = "Solo números, entre ${Limites.MIN_CEDULA} y ${Limites.MAX_CEDULA} dígitos",
                        teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.bringIntoViewRequester(anclas.getValue(Campo.CEDULA)),
                    )
                    CampoTexto(
                        valor = s.rolRepresentante,
                        onCambio = viewModel::onRol,
                        etiqueta = "Rol *",
                        ayuda = "Ej.: administrador, supervisor, portero",
                        error = s.errores[Campo.ROL],
                        teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.bringIntoViewRequester(anclas.getValue(Campo.ROL)),
                    )
                }

                // ---------- Firmas ----------
                TarjetaSeccion("Firmas", icono = Icons.Outlined.Draw) {
                    FirmaPad(
                        titulo = "Firma del técnico *",
                        firma = s.firmaTecnico,
                        onCambio = viewModel::onFirmaTecnico,
                        error = s.errores[Campo.FIRMA_TECNICO],
                        modifier = Modifier.bringIntoViewRequester(anclas.getValue(Campo.FIRMA_TECNICO)),
                    )
                    FirmaPad(
                        titulo = "Firma del representante *",
                        firma = s.firmaRepresentante,
                        onCambio = viewModel::onFirmaRepresentante,
                        error = s.errores[Campo.FIRMA_REPRESENTANTE],
                        modifier = Modifier.bringIntoViewRequester(anclas.getValue(Campo.FIRMA_REPRESENTANTE)),
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        " Se guarda solo en este dispositivo. Para enviarlo, use SINCRONIZAR en Inicio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = { viewModel.guardar(grosorFirmaPx) },
                    enabled = !s.guardando && !s.procesandoImagen,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    if (s.guardando) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Save, contentDescription = null)
                    }
                    Spacer(Modifier.size(8.dp))
                    Text("GUARDAR EN EL DISPOSITIVO")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (confirmarSalida) {
        DialogoConfirmacion(
            titulo = Mensajes.SALIR_SIN_GUARDAR_TITULO,
            texto = Mensajes.SALIR_SIN_GUARDAR_TEXTO,
            textoConfirmar = "Salir",
            destructivo = true,
            onConfirmar = {
                confirmarSalida = false
                onVolver()
            },
            onCancelar = { confirmarSalida = false },
        )
    }
}

@Composable
private fun Miniatura(evidencia: EvidenciaFormulario, numero: Int, onQuitar: () -> Unit) {
    Box(Modifier.size(96.dp)) {
        Surface(shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = File(evidencia.rutaArchivo),
                contentDescription = "Foto de evidencia $numero",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.small),
            )
        }
        FilledTonalIconButton(
            onClick = onQuitar,
            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(32.dp),
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Quitar foto $numero", modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorHora(hora: String, onHora: (String) -> Unit, error: String?, modifier: Modifier = Modifier) {
    var abierto by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = hora,
            onValueChange = {},
            readOnly = true,
            label = { Text("Hora de inicio *") },
            trailingIcon = { Icon(Icons.Outlined.AccessTime, contentDescription = null) },
            isError = error != null,
            supportingText = { Text(error ?: "La hora de fin se registra al guardar") },
            modifier = Modifier.fillMaxWidth(),
        )
        // Capa transparente encima del campo para abrir el selector al tocarlo.
        Surface(
            onClick = { abierto = true },
            color = androidx.compose.ui.graphics.Color.Transparent,
            modifier = Modifier
                .matchParentSize()
                .semantics { contentDescription = "Elegir hora de inicio. Actual: ${hora.ifEmpty { "sin definir" }}" },
        ) {}
    }
    if (abierto) {
        val partes = hora.split(":").mapNotNull { it.toIntOrNull() }
        val ahora = java.time.LocalTime.now()
        val estado = rememberTimePickerState(
            initialHour = partes.getOrNull(0) ?: ahora.hour,
            initialMinute = partes.getOrNull(1) ?: ahora.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { abierto = false },
            title = { Text("Hora de inicio") },
            text = { TimePicker(state = estado) },
            confirmButton = {
                TextButton(onClick = {
                    onHora("%02d:%02d".format(estado.hour, estado.minute))
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } },
        )
    }
}
