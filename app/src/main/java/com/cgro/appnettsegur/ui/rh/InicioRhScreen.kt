package com.cgro.appnettsegur.ui.rh

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.data.model.Empleado
import com.cgro.appnettsegur.ui.compartido.ChipCumplimiento
import com.cgro.appnettsegur.ui.compartido.MensajesRoles
import com.cgro.appnettsegur.ui.compartido.PantallaCargando
import com.cgro.appnettsegur.ui.compartido.PantallaVacia
import com.cgro.appnettsegur.ui.compartido.SeccionNecesitaInternet
import com.cgro.appnettsegur.ui.componentes.LocalMensajero
import com.cgro.appnettsegur.ui.componentes.TarjetaSeccion
import com.cgro.appnettsegur.ui.notificaciones.CampanaNotificaciones
import com.cgro.appnettsegur.ui.notificaciones.ResumenVencimientosRh
import java.time.LocalDate
import kotlinx.coroutines.launch

/**
 * Inicio de HCQ y RH (M2, fase 3): resumen de vencimientos (M3), buscador y lista de empleados con
 * chip de estado, ordenada con los vencidos primero. Permite crear un empleado nuevo.
 */
@Composable
fun InicioRhScreen(
    nombreUsuario: String,
    onAbrirEmpleado: (String) -> Unit,
    onBandeja: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: InicioRhViewModel = viewModel(factory = InicioRhViewModel.Factory),
) {
    val s by viewModel.estado.collectAsStateWithLifecycle()
    val hayInternet by viewModel.hayInternet.collectAsStateWithLifecycle()
    val busqueda by viewModel.busqueda.collectAsStateWithLifecycle()
    val mensajero = LocalMensajero.current
    val scope = rememberCoroutineScope()
    var mostrarForm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            com.cgro.appnettsegur.ui.compartido.BarraRol(
                titulo = MensajesRoles.INICIO_RH_TITULO,
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
            // Tarjeta resumen de vencimientos (M3, fase 4) y buscador: siempre visibles.
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ResumenVencimientosRh()
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = viewModel::buscar,
                    label = { Text(MensajesRoles.RH_BUSCAR) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = { mostrarForm = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Text("  " + MensajesRoles.RH_NUEVO_EMPLEADO)
                }
            }

            when {
                s.cargando && s.empleados.isEmpty() -> PantallaCargando()

                s.empleados.isEmpty() && !hayInternet ->
                    SeccionNecesitaInternet(onReintentar = viewModel::reintentar)

                s.empleados.isEmpty() && busqueda.isNotBlank() ->
                    PantallaVacia(MensajesRoles.RH_SIN_RESULTADOS, icono = Icons.Outlined.PersonSearch)

                s.empleados.isEmpty() ->
                    PantallaVacia(MensajesRoles.SIN_EMPLEADOS)

                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, bottom = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(s.empleados, key = { it.empleadoId }) { emp ->
                        FilaEmpleado(emp, onAbrir = { onAbrirEmpleado(emp.empleadoId) })
                    }
                }
            }
        }
    }

    if (mostrarForm) {
        EmpleadoFormDialog(
            base = null,
            onDismiss = { mostrarForm = false },
            onGuardar = { empleado ->
                mostrarForm = false
                scope.launch {
                    viewModel.guardar(empleado)
                        .onSuccess {
                            mensajero(MensajesRoles.EMP_GUARDADO)
                            onAbrirEmpleado(empleado.empleadoId)
                        }
                        .onFailure { mensajero(it.message ?: Mensajes.ERROR_GUARDANDO) }
                }
            },
        )
    }
}

@Composable
private fun FilaEmpleado(empleado: Empleado, onAbrir: () -> Unit) {
    val hoy = remember { LocalDate.now() }
    TarjetaSeccion(titulo = null, modifier = Modifier.fillMaxWidth().clickable(onClick = onAbrir)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(empleado.nombre, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${empleado.cargo} · C.C. ${empleado.cedula}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ChipCumplimiento(empleado.estadoCumplimiento(hoy))
        }
    }
}
