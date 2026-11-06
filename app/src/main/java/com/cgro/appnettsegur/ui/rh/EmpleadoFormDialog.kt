package com.cgro.appnettsegur.ui.rh

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.data.model.Empleado
import com.cgro.appnettsegur.ui.compartido.MensajesRoles
import com.cgro.appnettsegur.ui.componentes.CampoTexto
import com.cgro.appnettsegur.validacion.ReglasValidacion
import java.util.UUID

/**
 * Diálogo para crear o editar los datos básicos de un empleado (M2, fase 3, HU-15).
 *
 * La cédula reutiliza la regla R3 del taller (solo dígitos, entre 6 y 10). El cumplimiento y el
 * carné NO se tocan aquí: se gestionan en EmpleadoDetalle y se conservan al guardar.
 */
@Composable
fun EmpleadoFormDialog(
    base: Empleado?,
    onDismiss: () -> Unit,
    onGuardar: (Empleado) -> Unit,
) {
    var nombre by remember { mutableStateOf(base?.nombre.orEmpty()) }
    var cedula by remember { mutableStateOf(base?.cedula.orEmpty()) }
    var cargo by remember { mutableStateOf(base?.cargo.orEmpty()) }
    var uid by remember { mutableStateOf(base?.uid.orEmpty()) }

    var errorNombre by remember { mutableStateOf<String?>(null) }
    var errorCedula by remember { mutableStateOf<String?>(null) }
    var errorCargo by remember { mutableStateOf<String?>(null) }

    fun validarYGuardar() {
        errorNombre = if (nombre.isBlank()) Mensajes.CAMPO_OBLIGATORIO else null
        errorCargo = if (cargo.isBlank()) Mensajes.CAMPO_OBLIGATORIO else null
        errorCedula = if (cedula.isBlank()) Mensajes.CAMPO_OBLIGATORIO else ReglasValidacion.r3Cedula(cedula)
        if (errorNombre == null && errorCargo == null && errorCedula == null) {
            onGuardar(
                Empleado(
                    empleadoId = base?.empleadoId ?: UUID.randomUUID().toString(),
                    nombre = nombre.trim(),
                    cedula = cedula.trim(),
                    cargo = cargo.trim(),
                    uid = uid.trim().takeIf { it.isNotBlank() },
                    // Se conservan cumplimiento y carné del empleado existente (guardar no los toca).
                    cumplimiento = base?.cumplimiento ?: emptyMap(),
                    carneArchivoId = base?.carneArchivoId,
                    carneFecha = base?.carneFecha,
                )
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (base == null) MensajesRoles.EMP_CREAR_TITULO else MensajesRoles.EMP_EDITAR_TITULO) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CampoTexto(nombre, { nombre = it }, MensajesRoles.EMP_NOMBRE, error = errorNombre)
                CampoTexto(
                    cedula, { cedula = it.filter(Char::isDigit).take(10) }, MensajesRoles.EMP_CEDULA,
                    error = errorCedula, teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                CampoTexto(cargo, { cargo = it }, MensajesRoles.EMP_CARGO, error = errorCargo)
                CampoTexto(uid, { uid = it }, MensajesRoles.EMP_UID, ayuda = "Solo si el empleado tiene cuenta de técnico.")
            }
        },
        confirmButton = { TextButton(onClick = { validarYGuardar() }) { Text(MensajesRoles.EMP_GUARDAR) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
