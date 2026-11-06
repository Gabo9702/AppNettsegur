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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
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
import com.cgro.appnettsegur.data.model.Carta
import com.cgro.appnettsegur.data.model.Documento
import com.cgro.appnettsegur.data.model.Hito
import com.cgro.appnettsegur.data.model.NovedadRemota
import com.cgro.appnettsegur.data.model.PersonalAsignado
import com.cgro.appnettsegur.data.model.TipoDocumento
import com.cgro.appnettsegur.ui.componentes.LocalMensajero
import com.cgro.appnettsegur.ui.componentes.TarjetaSeccion
import com.cgro.appnettsegur.util.Formatos
import kotlinx.coroutines.launch

/** Pestañas del detalle de proyecto (comunes a cliente e ingeniero en la fase 1). */
private enum class TabProyecto(val titulo: String) {
    AVANCE(MensajesRoles.TAB_AVANCE),
    DOCUMENTOS(MensajesRoles.TAB_DOCUMENTOS),
    PERSONAL(MensajesRoles.TAB_PERSONAL),
    NOVEDADES(MensajesRoles.TAB_NOVEDADES),
    CARTAS(MensajesRoles.TAB_CARTAS),
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
    var tab by rememberSaveable { mutableStateOf(TabProyecto.AVANCE) }

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
                    PrimaryScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = 12.dp) {
                        TabProyecto.entries.forEach { t ->
                            Tab(
                                selected = tab == t,
                                onClick = { tab = t },
                                text = { Text(t.titulo) },
                            )
                        }
                    }
                    when (tab) {
                        TabProyecto.AVANCE -> TabAvance(s.proyecto?.avancePorcentaje ?: 0, s.proyecto?.hitos.orEmpty())
                        TabProyecto.DOCUMENTOS -> TabDocumentos(s.documentos, viewModel)
                        TabProyecto.PERSONAL -> TabPersonal(s.personal)
                        TabProyecto.NOVEDADES -> TabNovedades(s.novedades, onAbrirNovedad)
                        TabProyecto.CARTAS -> TabCartas(s.cartas, viewModel)
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
private fun TabAvance(avance: Int, hitos: List<Hito>) {
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
        item { Text(MensajesRoles.CRONOGRAMA_TITULO, style = MaterialTheme.typography.titleMedium) }
        if (hitos.isEmpty()) {
            item { Text(MensajesRoles.SIN_HITOS, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(hitos, key = { it.hitoId }) { hito -> FilaHito(hito) }
        }
    }
}

@Composable
private fun FilaHito(hito: Hito) {
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
            Text(
                if (hito.completado) MensajesRoles.HITO_COMPLETADO else MensajesRoles.HITO_PENDIENTE,
                style = MaterialTheme.typography.labelMedium,
                color = if (hito.completado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Pestaña Documentos: agrupados por tipo; al tocar uno se descarga y se abre
// ---------------------------------------------------------------------------
@Composable
private fun TabDocumentos(documentos: List<Documento>, viewModel: ProyectoDetalleViewModel) {
    if (documentos.isEmpty()) {
        PantallaVacia(MensajesRoles.SIN_DOCUMENTOS)
        return
    }
    val porTipo = remember(documentos) { documentos.groupBy { it.tipo } }
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
