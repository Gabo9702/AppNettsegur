package com.cgro.appnettsegur.ui.ingeniero

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.cgro.appnettsegur.data.model.Empleado
import com.cgro.appnettsegur.data.model.PersonalAsignado
import com.cgro.appnettsegur.data.model.TipoCumplimiento
import com.cgro.appnettsegur.ui.compartido.ChipCumplimiento
import com.cgro.appnettsegur.ui.compartido.MensajesRoles
import com.cgro.appnettsegur.ui.compartido.PantallaVacia
import com.cgro.appnettsegur.ui.compartido.ProyectoDetalleViewModel
import com.cgro.appnettsegur.ui.componentes.LocalMensajero
import com.cgro.appnettsegur.ui.componentes.TarjetaSeccion
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Pestaña Técnicos (M2, fase 2, HU-12 y flujo alterno de CU-07): asignar o quitar técnicos.
 *
 * Solo se pueden asignar los HABILITADOS (cumplimiento al día) y que tengan cuenta de técnico (uid).
 * Los no habilitados se muestran deshabilitados con el motivo ("ARL vencida el 12/10/2026").
 */
@Composable
fun TabTecnicos(
    empleados: List<Empleado>,
    personal: List<PersonalAsignado>,
    viewModel: ProyectoDetalleViewModel,
) {
    if (empleados.isEmpty()) {
        PantallaVacia(MensajesRoles.SIN_EMPLEADOS)
        return
    }
    val asignados = remember(personal) { personal.map { it.empleadoId }.toSet() }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TarjetaSeccion(titulo = MensajesRoles.TECNICOS_TITULO) {
                Text(MensajesRoles.TECNICOS_AYUDA, style = MaterialTheme.typography.bodySmall)
            }
        }
        items(empleados, key = { it.empleadoId }) { emp ->
            FilaTecnico(emp, asignado = emp.empleadoId in asignados, viewModel = viewModel)
        }
    }
}

@Composable
private fun FilaTecnico(empleado: Empleado, asignado: Boolean, viewModel: ProyectoDetalleViewModel) {
    val hoy = remember { LocalDate.now() }
    val mensajero = LocalMensajero.current
    val scope = rememberCoroutineScope()
    var trabajando by remember { mutableStateOf(false) }

    val sinCuenta = empleado.uid.isNullOrBlank()
    val motivo = remember(empleado) { motivoNoHabilitado(empleado, hoy) }
    val habilitado = motivo == null
    // Un técnico ya asignado siempre se puede QUITAR, aunque haya dejado de estar habilitado.
    val puedeInteractuar = !trabajando && !sinCuenta && (asignado || habilitado)

    TarjetaSeccion(titulo = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(empleado.nombre, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${empleado.cargo} · C.C. ${empleado.cedula}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when {
                    sinCuenta -> Text(
                        MensajesRoles.TECNICO_SIN_CUENTA,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                    )
                    !habilitado -> Text(
                        MensajesRoles.noHabilitadoDesde(motivo!!),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                    )
                    else -> ChipCumplimiento(empleado.estadoCumplimiento(hoy))
                }
            }
            if (trabajando) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else if (asignado) {
                OutlinedButton(
                    onClick = { accion(scope, viewModel, empleado, asignar = false, mensajero) { trabajando = it } },
                    enabled = !trabajando,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(MensajesRoles.QUITAR) }
            } else {
                Button(
                    onClick = { accion(scope, viewModel, empleado, asignar = true, mensajero) { trabajando = it } },
                    enabled = puedeInteractuar,
                ) { Text(MensajesRoles.ASIGNAR) }
            }
        }
    }
}

private fun accion(
    scope: kotlinx.coroutines.CoroutineScope,
    viewModel: ProyectoDetalleViewModel,
    empleado: Empleado,
    asignar: Boolean,
    mensajero: (String) -> Unit,
    setTrabajando: (Boolean) -> Unit,
) {
    setTrabajando(true)
    scope.launch {
        val res = viewModel.asignarTecnico(empleado, asignar)
        setTrabajando(false)
        res.onFailure { mensajero(it.message ?: "No se pudo actualizar el técnico.") }
    }
}

/**
 * Motivo por el que un empleado NO está habilitado, o null si lo está.
 * Revisa los tres documentos obligatorios en orden y reporta el primero que falta o está vencido.
 */
fun motivoNoHabilitado(empleado: Empleado, hoy: LocalDate): String? {
    val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    for (tipo in TipoCumplimiento.entries) {
        val nombre = MensajesRoles.NOMBRE_CUMPLIMIENTO[tipo.name] ?: tipo.name
        val doc = empleado.cumplimiento[tipo]
        when {
            doc == null -> return "Falta $nombre"
            doc.fechaVencimiento.isBefore(hoy) -> return "$nombre vencida el ${doc.fechaVencimiento.format(fmt)}"
        }
    }
    return null
}
