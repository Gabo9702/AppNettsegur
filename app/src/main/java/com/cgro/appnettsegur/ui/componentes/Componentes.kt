package com.cgro.appnettsegur.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cgro.appnettsegur.data.local.EstadoSync
import com.cgro.appnettsegur.ui.theme.Blanco
import com.cgro.appnettsegur.ui.theme.ColorPendiente
import com.cgro.appnettsegur.ui.theme.ColorSincronizado
import com.cgro.appnettsegur.ui.theme.NeutroOscuro

/** Muestra un snackbar desde cualquier pantalla (el host vive en NavGraph y sobrevive a la navegación). */
val LocalMensajero = staticCompositionLocalOf<(String) -> Unit> { {} }

/** Tarjeta de sección: bordes de 12 dp, borde fino de color y sombra mínima. */
@Composable
fun TarjetaSeccion(
    titulo: String?,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (titulo != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icono != null) {
                        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            contenido()
        }
    }
}

/** Etiqueta de estado: color + ícono + texto (nunca solo color, por accesibilidad). */
@Composable
fun EtiquetaEstado(estado: EstadoSync, modifier: Modifier = Modifier) {
    val (fondo, texto, icono, etiqueta) = when (estado) {
        EstadoSync.PENDIENTE -> Cuarteto(ColorPendiente, NeutroOscuro, Icons.Outlined.Schedule, "Pendiente")
        EstadoSync.SINCRONIZADO -> Cuarteto(ColorSincronizado, NeutroOscuro, Icons.Outlined.CheckCircle, "Sincronizado")
        EstadoSync.ERROR -> Cuarteto(MaterialTheme.colorScheme.error, Blanco, Icons.Outlined.ErrorOutline, "Error")
    }
    Row(
        modifier = modifier
            .background(fondo, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Estado: $etiqueta" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, contentDescription = null, tint = texto, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(etiqueta, color = texto, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

private data class Cuarteto(val fondo: Color, val texto: Color, val icono: ImageVector, val etiqueta: String)

/** Campo de texto con mensaje de error debajo. */
@Composable
fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    ayuda: String? = null,
    lineas: Int = 1,
    soloLectura: Boolean = false,
    teclado: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        modifier = modifier.fillMaxWidth(),
        isError = error != null,
        supportingText = (error ?: ayuda)?.let { { Text(it) } },
        singleLine = lineas == 1,
        minLines = lineas,
        readOnly = soloLectura,
        enabled = true,
        keyboardOptions = teclado,
    )
}

/** Lista desplegable de solo selección. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectorLista(
    etiqueta: String,
    opciones: List<T>,
    seleccion: T?,
    textoDe: (T) -> String,
    onSeleccion: (T) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    var expandido by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = seleccion?.let(textoDe).orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(textoDe(opcion)) },
                    onClick = {
                        onSeleccion(opcion)
                        expandido = false
                    },
                )
            }
        }
    }
}

/** Fila "etiqueta: valor" para pantallas de solo lectura. */
@Composable
fun DatoLectura(etiqueta: String, valor: String?, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor?.takeIf { it.isNotBlank() } ?: "—", style = MaterialTheme.typography.bodyLarge)
    }
}

/** Diálogo para acciones sensibles (eliminar, salir sin guardar). */
@Composable
fun DialogoConfirmacion(
    titulo: String,
    texto: String,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    destructivo: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(
                    textoConfirmar,
                    color = if (destructivo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

/** Mensaje de error dentro de una tarjeta (para campos que no son de texto). */
@Composable
fun TextoError(mensaje: String?) {
    if (mensaje != null) {
        Text(mensaje, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}
