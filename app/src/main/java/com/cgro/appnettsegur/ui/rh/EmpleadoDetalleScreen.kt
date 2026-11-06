package com.cgro.appnettsegur.ui.rh

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.data.model.Empleado
import com.cgro.appnettsegur.data.model.TipoCumplimiento
import com.cgro.appnettsegur.ui.compartido.ChipCumplimiento
import com.cgro.appnettsegur.ui.compartido.MensajesRoles
import com.cgro.appnettsegur.ui.compartido.PantallaCargando
import com.cgro.appnettsegur.ui.compartido.PantallaVacia
import com.cgro.appnettsegur.ui.compartido.SeccionNecesitaInternet
import com.cgro.appnettsegur.ui.componentes.DatoLectura
import com.cgro.appnettsegur.ui.componentes.LocalMensajero
import com.cgro.appnettsegur.ui.componentes.TarjetaSeccion
import com.cgro.appnettsegur.util.Formatos
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val FMT_DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * Detalle de un empleado (M2, fase 3, CU-07): datos, estado de habilitación, documentos de
 * cumplimiento (con sus fechas) y carné. Permite editar los datos y cargar documentos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmpleadoDetalleScreen(
    onVolver: () -> Unit,
    viewModel: EmpleadoDetalleViewModel = viewModel(factory = EmpleadoDetalleViewModel.Factory),
) {
    val s by viewModel.estado.collectAsStateWithLifecycle()
    val hayInternet by viewModel.hayInternet.collectAsStateWithLifecycle()
    val mensajero = LocalMensajero.current
    val scope = rememberCoroutineScope()

    var editando by remember { mutableStateOf(false) }
    var cargarTipo by remember { mutableStateOf<TipoCumplimiento?>(null) }

    val carneSelector = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            viewModel.subirCarne(uri)
                .onSuccess { mensajero(MensajesRoles.CARNE_GUARDADO) }
                .onFailure { mensajero(it.message ?: MensajesRoles.DESCARGA_ERROR) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.empleado?.nombre ?: "Empleado") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = MensajesRoles.VOLVER)
                    }
                },
                actions = {
                    if (s.empleado != null) {
                        IconButton(onClick = { editando = true }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Editar datos")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when {
                s.cargando && s.empleado == null && !hayInternet ->
                    SeccionNecesitaInternet(onReintentar = viewModel::reintentar)

                s.cargando && s.empleado == null -> PantallaCargando()

                s.empleado == null -> PantallaVacia("El empleado ya no está disponible.")

                else -> Contenido(
                    empleado = s.empleado!!,
                    onCargarCumplimiento = { cargarTipo = it },
                    onCargarCarne = { carneSelector.launch(arrayOf("application/pdf", "image/jpeg", "image/png")) },
                )
            }
        }
    }

    if (editando) {
        EmpleadoFormDialog(
            base = s.empleado,
            onDismiss = { editando = false },
            onGuardar = { empleado ->
                editando = false
                scope.launch {
                    viewModel.guardar(empleado)
                        .onSuccess { mensajero(MensajesRoles.EMP_GUARDADO) }
                        .onFailure { mensajero(it.message ?: Mensajes.ERROR_GUARDANDO) }
                }
            },
        )
    }

    val tipo = cargarTipo
    if (tipo != null) {
        CargarCumplimientoDialog(
            tipo = tipo,
            onDismiss = { cargarTipo = null },
            onConfirmar = { expedicion, vencimiento, uri ->
                cargarTipo = null
                scope.launch {
                    viewModel.subirCumplimiento(tipo, expedicion, vencimiento, uri)
                        .onSuccess { mensajero(MensajesRoles.CUMPLIMIENTO_GUARDADO) }
                        .onFailure { mensajero(it.message ?: MensajesRoles.DESCARGA_ERROR) }
                }
            },
        )
    }
}

@Composable
private fun Contenido(
    empleado: Empleado,
    onCargarCumplimiento: (TipoCumplimiento) -> Unit,
    onCargarCarne: () -> Unit,
) {
    val hoy = remember { LocalDate.now() }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TarjetaSeccion(titulo = "Datos") {
            DatoLectura(MensajesRoles.EMP_NOMBRE, empleado.nombre)
            DatoLectura(MensajesRoles.EMP_CARGO, empleado.cargo)
            DatoLectura(MensajesRoles.EMP_CEDULA, empleado.cedula)
            DatoLectura("Cuenta de técnico (uid)", empleado.uid)
        }

        // Estado calculado y habilitación para obra.
        TarjetaSeccion(titulo = "Estado") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChipCumplimiento(empleado.estadoCumplimiento(hoy))
                Text(
                    MensajesRoles.habilitadoObra(empleado.habilitado(hoy)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (empleado.habilitado(hoy)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        }

        // Documentos de cumplimiento: ARL, seguridad social y alturas.
        TarjetaSeccion(titulo = MensajesRoles.CUMPLIMIENTO_TITULO) {
            TipoCumplimiento.entries.forEach { tipo ->
                FilaCumplimiento(empleado, tipo, hoy, onCargar = { onCargarCumplimiento(tipo) })
            }
        }

        // Carné.
        TarjetaSeccion(titulo = MensajesRoles.CARNE_TITULO) {
            val tieneCarne = !empleado.carneArchivoId.isNullOrBlank()
            Text(
                if (tieneCarne) "Carné cargado" + (empleado.carneFecha?.let { " · ${Formatos.fecha(it)}" } ?: "")
                else MensajesRoles.CARNE_SIN,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onCargarCarne, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Badge, contentDescription = null)
                Text("  " + MensajesRoles.CARNE_CARGAR)
            }
        }
    }
}

@Composable
private fun FilaCumplimiento(
    empleado: Empleado,
    tipo: TipoCumplimiento,
    hoy: LocalDate,
    onCargar: () -> Unit,
) {
    val doc = empleado.cumplimiento[tipo]
    val nombre = MensajesRoles.NOMBRE_CUMPLIMIENTO[tipo.name] ?: tipo.name
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(nombre, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onCargar) {
                Icon(Icons.Outlined.UploadFile, contentDescription = null)
                Text("  " + (if (doc == null) MensajesRoles.CUMPLIMIENTO_CARGAR else "Reemplazar"))
            }
        }
        if (doc == null) {
            Text(
                MensajesRoles.CUMPLIMIENTO_SIN,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            val vencido = doc.fechaVencimiento.isBefore(hoy)
            Text(
                MensajesRoles.venceEl(doc.fechaVencimiento.format(FMT_DIA)),
                style = MaterialTheme.typography.bodySmall,
                color = if (vencido) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
