package com.cgro.appnettsegur.ui.analista

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.data.model.Empleado
import com.cgro.appnettsegur.ui.compartido.BarraRol
import com.cgro.appnettsegur.ui.compartido.MensajesRoles
import com.cgro.appnettsegur.ui.compartido.PantallaVacia
import com.cgro.appnettsegur.ui.compartido.SeccionNecesitaInternet
import com.cgro.appnettsegur.ui.componentes.LocalMensajero
import com.cgro.appnettsegur.ui.componentes.SelectorLista
import com.cgro.appnettsegur.ui.componentes.TarjetaSeccion
import com.cgro.appnettsegur.ui.ingeniero.motivoNoHabilitado
import com.cgro.appnettsegur.ui.notificaciones.CampanaNotificaciones
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class TabAnalista(val titulo: String) {
    CARNES(MensajesRoles.ANALISTA_TAB_CARNES),
    CARTAS(MensajesRoles.ANALISTA_TAB_CARTAS),
}

/**
 * Inicio del analista administrativo (M2, fase 4): cargar carnés y generar cartas de presentación.
 */
@Composable
fun InicioAnalistaScreen(
    nombreUsuario: String,
    onBandeja: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: InicioAnalistaViewModel = viewModel(factory = InicioAnalistaViewModel.Factory),
) {
    val hayInternet by viewModel.hayInternet.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(TabAnalista.CARNES) }

    Scaffold(
        topBar = {
            BarraRol(
                titulo = MensajesRoles.INICIO_ANALISTA_TITULO,
                subtitulo = nombreUsuario.takeIf { it.isNotBlank() },
                acciones = {
                    CampanaNotificaciones(onAbrir = onBandeja)
                    IconButton(onClick = onCerrarSesion) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = Mensajes.CERRAR_SESION)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tab.ordinal) {
                TabAnalista.entries.forEach { t ->
                    Tab(selected = tab == t, onClick = { tab = t }, text = { Text(t.titulo) })
                }
            }
            when (tab) {
                TabAnalista.CARNES -> TabCarnes(viewModel, hayInternet)
                TabAnalista.CARTAS -> TabCartas(viewModel, nombreUsuario, hayInternet)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pestaña Carnés: cargar el carné (imagen o PDF) de cada empleado
// ---------------------------------------------------------------------------
@Composable
private fun TabCarnes(viewModel: InicioAnalistaViewModel, hayInternet: Boolean) {
    val empleados by viewModel.empleados.collectAsStateWithLifecycle()
    val mensajero = LocalMensajero.current
    val scope = rememberCoroutineScope()
    var empleadoObjetivo by remember { mutableStateOf<String?>(null) }

    val selector = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val id = empleadoObjetivo
        if (uri == null || id == null) return@rememberLauncherForActivityResult
        scope.launch {
            viewModel.subirCarne(id, uri)
                .onSuccess { mensajero(MensajesRoles.CARNE_GUARDADO) }
                .onFailure { mensajero(it.message ?: MensajesRoles.DESCARGA_ERROR) }
        }
    }

    when {
        empleados.isEmpty() && !hayInternet -> SeccionNecesitaInternet(onReintentar = viewModel::reintentar)
        empleados.isEmpty() -> PantallaVacia(MensajesRoles.SIN_EMPLEADOS)
        else -> LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(empleados, key = { it.empleadoId }) { emp ->
                TarjetaSeccion(titulo = null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(emp.nombre, style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (!emp.carneArchivoId.isNullOrBlank()) "Con carné" else MensajesRoles.CARNE_SIN,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(onClick = {
                            empleadoObjetivo = emp.empleadoId
                            selector.launch(arrayOf("application/pdf", "image/jpeg", "image/png"))
                        }) {
                            Icon(Icons.Outlined.Badge, contentDescription = null)
                            Text("  " + MensajesRoles.CARNE_CARGAR)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pestaña Cartas: elegir proyecto y técnicos habilitados, previsualizar y guardar
// ---------------------------------------------------------------------------
@Composable
private fun TabCartas(viewModel: InicioAnalistaViewModel, nombreAnalista: String, hayInternet: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mensajero = LocalMensajero.current
    val scope = rememberCoroutineScope()
    val hoy = remember { LocalDate.now() }

    val proyectos by viewModel.proyectos.collectAsStateWithLifecycle()
    val proyecto by viewModel.proyectoCarta.collectAsStateWithLifecycle()
    val tecnicos by viewModel.tecnicosDelProyecto.collectAsStateWithLifecycle()

    val seleccionados = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    var preview by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var pdf by remember { mutableStateOf<java.io.File?>(null) }
    var trabajando by remember { mutableStateOf(false) }

    // Al cambiar de proyecto se limpian la selección y la vista previa.
    androidx.compose.runtime.LaunchedEffect(proyecto?.proyectoId) {
        seleccionados.clear(); preview = null; pdf = null
    }

    if (proyectos.isEmpty() && !hayInternet) {
        SeccionNecesitaInternet(onReintentar = viewModel::reintentar)
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SelectorLista(
                etiqueta = MensajesRoles.ANALISTA_ELEGIR_PROYECTO,
                opciones = proyectos,
                seleccion = proyecto,
                textoDe = { it.nombreProyecto },
                onSeleccion = { viewModel.elegirProyecto(it) },
            )
        }

        if (proyecto != null) {
            val habilitados = tecnicos.filter { it.habilitado(hoy) }
            if (tecnicos.isEmpty() || habilitados.isEmpty()) {
                item { Text(MensajesRoles.ANALISTA_SIN_TECNICOS_HABILITADOS, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                item { Text(MensajesRoles.ANALISTA_ELEGIR_TECNICOS, style = MaterialTheme.typography.titleMedium) }
                items(tecnicos, key = { it.empleadoId }) { emp ->
                    FilaTecnicoCarta(
                        empleado = emp,
                        hoy = hoy,
                        marcado = emp.empleadoId in seleccionados,
                        onMarcar = { marcar ->
                            if (marcar) seleccionados.add(emp.empleadoId) else seleccionados.remove(emp.empleadoId)
                            preview = null; pdf = null // cambió la selección: hay que volver a previsualizar
                        },
                    )
                }

                val elegidos = tecnicos.filter { it.empleadoId in seleccionados && it.habilitado(hoy) }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                trabajando = true
                                scope.launch {
                                    val archivo = CartaPdf.generar(context, proyecto!!, elegidos, nombreAnalista)
                                    preview = renderizarPrimeraPagina(archivo)
                                    pdf = archivo
                                    trabajando = false
                                }
                            },
                            enabled = elegidos.isNotEmpty() && !trabajando,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null)
                            Text("  " + MensajesRoles.ANALISTA_VISTA_PREVIA)
                        }
                        Button(
                            onClick = {
                                val archivo = pdf ?: return@Button
                                trabajando = true
                                scope.launch {
                                    viewModel.guardarCarta(proyecto!!.proyectoId, elegidos, archivo)
                                        .onSuccess { mensajero(MensajesRoles.ANALISTA_CARTA_GUARDADA) }
                                        .onFailure { mensajero(it.message ?: Mensajes.ERROR_GUARDANDO) }
                                    trabajando = false
                                }
                            },
                            enabled = pdf != null && !trabajando,
                            modifier = Modifier.weight(1f),
                        ) { Text(MensajesRoles.ANALISTA_GUARDAR_CARTA) }
                    }
                }

                if (trabajando) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("  " + MensajesRoles.ANALISTA_GENERANDO)
                        }
                    }
                }

                preview?.let { bmp ->
                    item {
                        Text(MensajesRoles.ANALISTA_VISTA_PREVIA, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Vista previa de la carta",
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaTecnicoCarta(empleado: Empleado, hoy: LocalDate, marcado: Boolean, onMarcar: (Boolean) -> Unit) {
    val motivo = remember(empleado) { motivoNoHabilitado(empleado, hoy) }
    val habilitado = motivo == null
    TarjetaSeccion(titulo = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = marcado, onCheckedChange = onMarcar, enabled = habilitado)
            Column(Modifier.weight(1f)) {
                Text(empleado.nombre, style = MaterialTheme.typography.titleSmall)
                Text(
                    "C.C. ${empleado.cedula}" + (if (!habilitado) " · " + MensajesRoles.noHabilitadoDesde(motivo!!) else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (habilitado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
