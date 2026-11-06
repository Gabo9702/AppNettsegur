package com.cgro.appnettsegur.ui.compartido

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cgro.appnettsegur.config.Catalogos
import com.cgro.appnettsegur.config.Rol
import com.cgro.appnettsegur.data.model.Carta
import com.cgro.appnettsegur.data.model.Documento
import com.cgro.appnettsegur.data.model.Hito
import com.cgro.appnettsegur.data.model.NovedadRemota
import com.cgro.appnettsegur.data.model.PersonalAsignado
import com.cgro.appnettsegur.data.model.TipoDocumento
import com.cgro.appnettsegur.ui.componentes.LocalMensajero
import com.cgro.appnettsegur.ui.componentes.TarjetaSeccion
import com.cgro.appnettsegur.ui.ingeniero.CargarDocumentoDialog
import com.cgro.appnettsegur.ui.ingeniero.TabTecnicos
import com.cgro.appnettsegur.util.Formatos
import kotlinx.coroutines.launch

/** Pestañas del detalle de proyecto. TECNICOS solo la ve el ingeniero (fase 2). */
private enum class TabProyecto(val titulo: String) {
    AVANCE(MensajesRoles.TAB_AVANCE),
    DOCUMENTOS(MensajesRoles.TAB_DOCUMENTOS),
    PERSONAL(MensajesRoles.TAB_PERSONAL),
    NOVEDADES(MensajesRoles.TAB_NOVEDADES),
    CARTAS(MensajesRoles.TAB_CARTAS),
    TECNICOS(MensajesRoles.TAB_TECNICOS),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProyectoDetalleScreen(
    onVolver: () -> Unit,
    onAbrirNovedad: (String) -> Unit,
    viewModel: ProyectoDetalleViewModel = viewModel(factory = ProyectoDetalleViewModel.Factory),
) {
    val s by viewModel.estado.collectAsStateWithLifecycle()
    val hayInternet by viewModel.hayInternet.collectAsStateWithLifecycle()
    val rol by viewModel.rol.collectAsStateWithLifecycle()
    val empleados by viewModel.empleadosDisponibles.collectAsStateWithLifecycle()
    val esIngeniero = rol == Rol.INGENIERO
    var tab by rememberSaveable { mutableStateOf(TabProyecto.AVANCE) }

    // El ingeniero ve además la pestaña Técnicos; el cliente solo las de lectura.
    val tabs = remember(esIngeniero) {
        TabProyecto.entries.filter { it != TabProyecto.TECNICOS || esIngeniero }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.proyecto?.nombreProyecto ?: "Proyecto") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = MensajesRoles.VOLVER)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when {
                s.cargando && s.proyecto == null && !hayInternet ->
                    SeccionNecesitaInternet(onReintentar = viewModel::reintentar)

                s.cargando && s.proyecto == null ->
                    PantallaCargando()

                else -> {
                    val indice = tabs.indexOf(tab).coerceAtLeast(0)
                    PrimaryScrollableTabRow(selectedTabIndex = indice, edgePadding = 12.dp) {
                        tabs.forEach { t ->
                            Tab(
                                selected = tab == t,
                                onClick = { tab = t },
                                text = { Text(t.titulo) },
                            )
                        }
                    }
                    when (tab) {
                        TabProyecto.AVANCE ->
                            TabAvance(s.proyecto?.avancePorcentaje ?: 0, s.proyecto?.hitos.orEmpty(), esIngeniero, viewModel)
                        TabProyecto.DOCUMENTOS -> TabDocumentos(s.documentos, viewModel, esIngeniero)
                        TabProyecto.PERSONAL -> TabPersonal(s.personal)
                        TabProyecto.NOVEDADES -> TabNovedades(s.novedades, onAbrirNovedad)
                        TabProyecto.CARTAS -> TabCartas(s.cartas, viewModel)
                        TabProyecto.TECNICOS -> TabTecnicos(empleados, s.personal, viewModel)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pestaña Avance: porcentaje + cronograma de hitos (solo lectura en la fase 1)
// ---------------------------------------------------------------------------
@Composable
private fun TabAvance(avance: Int, hitos: List<Hito>, esIngeniero: Boolean, viewModel: ProyectoDetalleViewModel) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TarjetaSeccion(titulo = MensajesRoles.TAB_AVANCE) {
                Text(MensajesRoles.avance(avance), style = MaterialTheme.typography.titleLarge)
                LinearProgressIndicator(
                    progress = { avance.coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                )
            }
        }
        item {
            Text(MensajesRoles.CRONOGRAMA_TITULO, style = MaterialTheme.typography.titleMedium)
            // El ingeniero puede marcar/desmarcar hitos; el avance se recalcula (HU-12).
            if (esIngeniero) Text(
                MensajesRoles.CRONOGRAMA_EDITAR,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (hitos.isEmpty()) {
            item { Text(MensajesRoles.SIN_HITOS, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(hitos, key = { it.hitoId }) { hito -> FilaHito(hito, esIngeniero, viewModel) }
        }
    }
}

@Composable
private fun FilaHito(hito: Hito, esIngeniero: Boolean, viewModel: ProyectoDetalleViewModel) {
    val mensajero = LocalMensajero.current
    val scope = rememberCoroutineScope()
    var trabajando by remember { mutableStateOf(false) }

    TarjetaSeccion(titulo = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (hito.completado) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (hito.completado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(hito.nombre, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${Formatos.fecha(hito.fechaInicio)} – ${Formatos.fecha(hito.fechaFin)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (esIngeniero) {
                Switch(
                    checked = hito.completado,
                    enabled = !trabajando,
                    onCheckedChange = { nuevo ->
                        trabajando = true
                        scope.launch {
                            val res = viewModel.marcarHito(hito.hitoId, nuevo)
                            trabajando = false
                            res.onFailure { mensajero(it.message ?: "No se pudo actualizar el hito.") }
                        }
                    },
                )
            } else {
                Text(
                    if (hito.completado) MensajesRoles.HITO_COMPLETADO else MensajesRoles.HITO_PENDIENTE,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (hito.completado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pestaña Documentos: agrupados por tipo; al tocar uno se descarga y se abre
// ---------------------------------------------------------------------------
@Composable
private fun TabDocumentos(documentos: List<Documento>, viewModel: ProyectoDetalleViewModel, esIngeniero: Boolean) {
    val mensajero = LocalMensajero.current
    val scope = rememberCoroutineScope()
    var mostrarDialogo by remember { mutableStateOf(false) }
    var subiendo by remember { mutableStateOf(false) }
    val porTipo = remember(documentos) { documentos.groupBy { it.tipo } }

    Column(Modifier.fillMaxSize()) {
        // El ingeniero puede cargar documentos (CU-05); el cliente solo los lee.
        if (esIngeniero) {
            Button(
                onClick = { mostrarDialogo = true },
                enabled = !subiendo,
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
            ) {
                Icon(Icons.Outlined.UploadFile, contentDescription = null)
                Text("  " + MensajesRoles.CARGAR_DOCUMENTO)
            }
            if (subiendo) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Text(MensajesRoles.DOC_SUBIENDO, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        androidx.compose.foundation.layout.Box(Modifier.weight(1f).fillMaxWidth()) {
            if (documentos.isEmpty()) {
                PantallaVacia(MensajesRoles.SIN_DOCUMENTOS)
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TipoDocumento.entries.forEach { tipo ->
                        val lista = porTipo[tipo].orEmpty()
                        if (lista.isNotEmpty()) {
                            item(key = "cab_${tipo.name}") {
                                Text(etiquetaTipoDocumento(tipo), style = MaterialTheme.typography.titleMedium)
                            }
                            items(lista, key = { it.documentoId }) { doc -> FilaDocumento(doc, viewModel) }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogo) {
        CargarDocumentoDialog(
            onDismiss = { mostrarDialogo = false },
            onPublicar = { tipo, uri ->
                mostrarDialogo = false
                subiendo = true
                scope.launch {
                    val res = viewModel.subirDocumento(tipo, uri)
                    subiendo = false
                    res.onSuccess { mensajero(MensajesRoles.DOC_PUBLICADO) }
                        .onFailure { mensajero(it.message ?: MensajesRoles.DESCARGA_ERROR) }
                }
            },
        )
    }
}

@Composable
private fun FilaDocumento(doc: Documento, viewModel: ProyectoDetalleViewModel) {
    val context = LocalContext.current
    val mensajero = LocalMensajero.current
    val scope = rememberCoroutineScope()
    var descargando by remember { mutableStateOf(false) }

    TarjetaSeccion(
        titulo = null,
        modifier = Modifier.clickable(enabled = !descargando) {
            descargando = true
            scope.launch {
                val res = viewModel.descargar(doc.archivoId)
                descargando = false
                res.onSuccess { archivo ->
                    val abierto = abrirArchivo(context, archivo, doc.mime)
                    if (!abierto) mensajero(MensajesRoles.SIN_APP_PARA_ABRIR)
                }.onFailure { mensajero(it.message ?: MensajesRoles.DESCARGA_ERROR) }
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(doc.nombre, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${tamanoLegible(doc.tamanoBytes)} · ${Formatos.fecha(doc.fecha)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (descargando) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pestaña Personal: vista reducida (nombre, cédula, estado de cumplimiento)
// ---------------------------------------------------------------------------
@Composable
private fun TabPersonal(personal: List<PersonalAsignado>) {
    if (personal.isEmpty()) {
        PantallaVacia(MensajesRoles.SIN_PERSONAL)
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(personal, key = { it.empleadoId }) { p ->
            TarjetaSeccion(titulo = null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.nombre, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "C.C. ${p.cedula}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    ChipCumplimiento(p.estadoCumplimiento)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pestaña Novedades: sincronizadas del proyecto, filtrables por estado de la atención
// ---------------------------------------------------------------------------
@Composable
private fun TabNovedades(novedades: List<NovedadRemota>, onAbrir: (String) -> Unit) {
    var filtro by rememberSaveable { mutableStateOf<String?>(null) } // null = todas

    val ordenadas = remember(novedades) { novedades.sortedByDescending { it.fechaServicio } }
    val visibles = remember(ordenadas, filtro) {
        if (filtro == null) ordenadas else ordenadas.filter { it.estadoAtencion == filtro }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(filtro == null, onClick = { filtro = null }, label = { Text(MensajesRoles.FILTRO_TODAS) })
            FilterChip(filtro == Catalogos.RESUELTA, onClick = { filtro = Catalogos.RESUELTA }, label = { Text(MensajesRoles.FILTRO_RESUELTAS) })
            FilterChip(filtro == Catalogos.NO_RESUELTA, onClick = { filtro = Catalogos.NO_RESUELTA }, label = { Text(MensajesRoles.FILTRO_NO_RESUELTAS) })
        }
        if (visibles.isEmpty()) {
            PantallaVacia(MensajesRoles.SIN_NOVEDADES)
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(visibles, key = { it.idRegistro }) { nov -> FilaNovedad(nov, onAbrir) }
            }
        }
    }
}

@Composable
private fun FilaNovedad(nov: NovedadRemota, onAbrir: (String) -> Unit) {
    TarjetaSeccion(titulo = null, modifier = Modifier.clickable { onAbrir(nov.idRegistro) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(nov.tipoServicio, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${nov.tipoActividad} · ${Formatos.fecha(nov.fechaServicio)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                textoEstadoAtencion(nov.estadoAtencion),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (nov.estadoAtencion == Catalogos.RESUELTA)
                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Pestaña Cartas: lista y descarga
// ---------------------------------------------------------------------------
@Composable
private fun TabCartas(cartas: List<Carta>, viewModel: ProyectoDetalleViewModel) {
    if (cartas.isEmpty()) {
        PantallaVacia(MensajesRoles.SIN_CARTAS)
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(cartas, key = { it.cartaId }) { carta -> FilaCarta(carta, viewModel) }
    }
}

@Composable
private fun FilaCarta(carta: Carta, viewModel: ProyectoDetalleViewModel) {
    val context = LocalContext.current
    val mensajero = LocalMensajero.current
    val scope = rememberCoroutineScope()
    var descargando by remember { mutableStateOf(false) }

    TarjetaSeccion(
        titulo = null,
        modifier = Modifier.clickable(enabled = !descargando) {
            descargando = true
            scope.launch {
                val res = viewModel.descargar(carta.archivoId)
                descargando = false
                res.onSuccess { archivo ->
                    val abierto = abrirArchivo(context, archivo, "application/pdf")
                    if (!abierto) mensajero(MensajesRoles.SIN_APP_PARA_ABRIR)
                }.onFailure { mensajero(it.message ?: MensajesRoles.DESCARGA_ERROR) }
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(
                MensajesRoles.cartaEtiqueta(Formatos.fecha(carta.fecha)),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            if (descargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}

// ---------------------------------------------------------------------------
// Apoyos
// ---------------------------------------------------------------------------
fun etiquetaTipoDocumento(tipo: TipoDocumento): String = when (tipo) {
    TipoDocumento.COTIZACION -> "Cotización"
    TipoDocumento.CRONOGRAMA -> "Cronograma"
    TipoDocumento.INFORME_AVANCE -> "Informe de avance"
    TipoDocumento.ACTA_ENTREGA -> "Acta de entrega"
    TipoDocumento.INFORME_FINAL -> "Informe final"
}

private fun textoEstadoAtencion(valor: String): String = Catalogos.estadosAtencion[valor] ?: valor

private fun tamanoLegible(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}
