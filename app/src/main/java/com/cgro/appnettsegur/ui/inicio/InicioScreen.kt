package com.cgro.appnettsegur.ui.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cgro.appnettsegur.BuildConfig
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.data.repository.ProgresoSync
import com.cgro.appnettsegur.ui.componentes.TarjetaSeccion
import com.cgro.appnettsegur.ui.theme.Blanco
import com.cgro.appnettsegur.ui.theme.ColorPendiente
import com.cgro.appnettsegur.ui.theme.ColorSincronizado
import com.cgro.appnettsegur.ui.theme.NeutroOscuro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    hayInternet: Boolean,
    progreso: ProgresoSync,
    falloSimuladoEn: Int?,
    onSincronizar: () -> Unit,
    onSimularFallo: (Int?) -> Unit,
    onNuevo: () -> Unit,
    onVerRegistros: () -> Unit,
    onEditarPerfil: () -> Unit,
    viewModel: InicioViewModel = viewModel(factory = InicioViewModel.Factory),
) {
    val s by viewModel.estado.collectAsStateWithLifecycle()
    var menuDesarrollador by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Nettsegur", style = MaterialTheme.typography.titleLarge)
                        if (s.nombreTecnico.isNotBlank()) {
                            Text(
                                "Técnico: ${s.nombreTecnico}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    if (BuildConfig.DEBUG) {
                        IconButton(onClick = { menuDesarrollador = true }) {
                            Icon(Icons.Outlined.BugReport, contentDescription = "Menú de desarrollador")
                        }
                    }
                    IconButton(onClick = onEditarPerfil) {
                        Icon(Icons.Outlined.ManageAccounts, contentDescription = "Editar perfil del técnico")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BannerConexion(hayInternet, s.conteos.porSincronizar)

            // ---------- Contadores ----------
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Contador("Locales", s.conteos.total, Icons.Outlined.Inventory2, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                Contador("Pendientes", s.conteos.pendientes, Icons.Outlined.Schedule, ColorPendiente, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Contador("Sincronizados", s.conteos.sincronizados, Icons.Outlined.CheckCircle, ColorSincronizado, Modifier.weight(1f))
                Contador("Con error", s.conteos.conError, Icons.Outlined.ErrorOutline, MaterialTheme.colorScheme.error, Modifier.weight(1f))
            }

            if (BuildConfig.DEBUG && falloSimuladoEn != null) {
                Text(
                    "Modo prueba: el registro N.º $falloSimuladoEn fallará al sincronizar.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // ---------- Acciones ----------
            Button(onClick = onNuevo, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text("  Nuevo registro")
            }
            OutlinedButton(onClick = onVerRegistros, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null)
                Text("  Ver registros")
            }
            Button(
                onClick = onSincronizar,
                enabled = !progreso.sincronizando, // botón deshabilitado mientras se envía
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
            ) {
                Icon(Icons.Outlined.CloudUpload, contentDescription = null)
                Text(if (progreso.sincronizando) "  SINCRONIZANDO…" else "  SINCRONIZAR")
            }
            if (progreso.sincronizando) {
                Column(
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    LinearProgressIndicator(
                        progress = { if (progreso.total == 0) 0f else progreso.actual.toFloat() / progreso.total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(Mensajes.progresoSync(progreso.actual, progreso.total), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (menuDesarrollador) {
        MenuDesarrollador(
            falloSimuladoEn = falloSimuladoEn,
            onAplicar = {
                onSimularFallo(it)
                menuDesarrollador = false
            },
            onCerrar = { menuDesarrollador = false },
        )
    }
}

@Composable
private fun BannerConexion(hayInternet: Boolean, porSincronizar: Int) {
    val (fondo, texto, icono, mensaje) = if (hayInternet) {
        Banner(ColorSincronizado.copy(alpha = 0.22f), NeutroOscuro, Icons.Outlined.Wifi, Mensajes.bannerConectado(porSincronizar))
    } else {
        Banner(ColorPendiente.copy(alpha = 0.30f), NeutroOscuro, Icons.Outlined.CloudOff, Mensajes.BANNER_SIN_CONEXION)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(fondo, MaterialTheme.shapes.medium)
            .padding(14.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, contentDescription = null, tint = texto)
        Spacer(Modifier.size(10.dp))
        Text(mensaje, color = texto, style = MaterialTheme.typography.bodyMedium)
    }
}

private data class Banner(val fondo: Color, val texto: Color, val icono: ImageVector, val mensaje: String)

@Composable
private fun Contador(titulo: String, valor: Int, icono: ImageVector, color: Color, modifier: Modifier = Modifier) {
    TarjetaSeccion(titulo = null, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icono,
                contentDescription = null,
                tint = if (color == ColorPendiente) NeutroOscuro else Blanco,
                modifier = Modifier
                    .background(color, MaterialTheme.shapes.small)
                    .padding(6.dp)
                    .size(20.dp),
            )
            Spacer(Modifier.size(10.dp))
            Column {
                Text("$valor", style = MaterialTheme.typography.headlineSmall)
                Text(titulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Solo en builds debug: hace fallar a propósito el registro N.º __ al sincronizar (Prueba E). */
@Composable
private fun MenuDesarrollador(falloSimuladoEn: Int?, onAplicar: (Int?) -> Unit, onCerrar: () -> Unit) {
    var activo by remember { mutableStateOf(falloSimuladoEn != null) }
    var numero by remember { mutableStateOf(falloSimuladoEn?.toString() ?: "1") }
    val numeroValido = numero.toIntOrNull()?.takeIf { it > 0 }

    AlertDialog(
        onDismissRequest = onCerrar,
        icon = { Icon(Icons.Outlined.BugReport, contentDescription = null) },
        title = { Text("Menú de desarrollador") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Simular fallo en el registro N.º", modifier = Modifier.weight(1f))
                    Switch(checked = activo, onCheckedChange = { activo = it })
                }
                OutlinedTextField(
                    value = numero,
                    onValueChange = { numero = it.filter(Char::isDigit).take(3) },
                    label = { Text("N.º de registro en la sincronización") },
                    enabled = activo,
                    isError = activo && numeroValido == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text(
                    "Los registros se envían del más antiguo al más reciente. El N.º 1 es el primero que se envía.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAplicar(if (activo) numeroValido else null) },
                enabled = !activo || numeroValido != null,
            ) { Text("Aplicar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } },
    )
}
