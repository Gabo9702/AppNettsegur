package com.cgro.appnettsegur.ui.rh

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * Campo de fecha de solo lectura que abre un DatePicker de Material 3 al tocarlo (M2, fase 3).
 * El DatePicker trabaja en UTC; se convierte a LocalDate con ZoneOffset.UTC para no desfasar el día.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorFecha(
    etiqueta: String,
    fecha: LocalDate?,
    onFecha: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    var abierto by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = fecha?.format(FMT).orEmpty(),
        onValueChange = {},
        readOnly = true,
        enabled = false, // el campo no se edita a mano; se toca para abrir el calendario
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
        modifier = modifier
            .fillMaxWidth()
            .clickable { abierto = true },
    )

    if (abierto) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fecha?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { millis ->
                        onFecha(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = estado)
        }
    }
}
