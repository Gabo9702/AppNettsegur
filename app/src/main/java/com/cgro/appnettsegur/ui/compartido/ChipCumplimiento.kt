package com.cgro.appnettsegur.ui.compartido

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cgro.appnettsegur.data.model.EstadoCumplimiento
import com.cgro.appnettsegur.ui.theme.ColorPendiente
import com.cgro.appnettsegur.ui.theme.ColorSincronizado
import com.cgro.appnettsegur.ui.theme.NeutroOscuro
import com.cgro.appnettsegur.ui.theme.Blanco

/**
 * Chip del estado de cumplimiento de un empleado (M2): color + ícono + texto, nunca solo color.
 *
 * El estado lo calcula [EstadoCumplimiento]: VIGENTE, POR_VENCER (15 días o menos) o VENCIDO.
 */
@Composable
fun ChipCumplimiento(estado: EstadoCumplimiento, modifier: Modifier = Modifier) {
    val d = estiloCumplimiento(estado)
    Row(
        modifier = modifier
            .background(d.fondo, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Cumplimiento: ${d.etiqueta}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(d.icono, contentDescription = null, tint = d.texto, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(d.etiqueta, color = d.texto, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Texto legible del estado de cumplimiento. */
fun textoCumplimiento(estado: EstadoCumplimiento): String = when (estado) {
    EstadoCumplimiento.VIGENTE -> "Vigente"
    EstadoCumplimiento.POR_VENCER -> "Por vencer"
    EstadoCumplimiento.VENCIDO -> "Vencido"
}

private data class EstiloCumplimiento(val fondo: Color, val texto: Color, val icono: ImageVector, val etiqueta: String)

@Composable
private fun estiloCumplimiento(estado: EstadoCumplimiento): EstiloCumplimiento = when (estado) {
    EstadoCumplimiento.VIGENTE ->
        EstiloCumplimiento(ColorSincronizado, NeutroOscuro, Icons.Outlined.CheckCircle, "Vigente")
    EstadoCumplimiento.POR_VENCER ->
        EstiloCumplimiento(ColorPendiente, NeutroOscuro, Icons.Outlined.Schedule, "Por vencer")
    EstadoCumplimiento.VENCIDO ->
        EstiloCumplimiento(MaterialTheme.colorScheme.error, Blanco, Icons.Outlined.ErrorOutline, "Vencido")
}
