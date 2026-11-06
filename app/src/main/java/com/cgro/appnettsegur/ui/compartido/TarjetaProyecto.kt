package com.cgro.appnettsegur.ui.compartido

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cgro.appnettsegur.data.model.EstadoProyecto
import com.cgro.appnettsegur.data.model.Proyecto
import com.cgro.appnettsegur.ui.componentes.TarjetaSeccion

/** Tarjeta de un proyecto en las listas de cliente e ingeniero: nombre, estado y barra de avance. */
@Composable
fun TarjetaProyecto(proyecto: Proyecto, onAbrir: () -> Unit, modifier: Modifier = Modifier) {
    TarjetaSeccion(
        titulo = null,
        modifier = modifier.fillMaxWidth().clickable(onClick = onAbrir),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                proyecto.nombreProyecto,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            EtiquetaEstadoProyecto(proyecto.estado)
        }
        Text(
            proyecto.nombreCliente,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(MensajesRoles.avance(proyecto.avancePorcentaje), style = MaterialTheme.typography.labelMedium)
            LinearProgressIndicator(
                progress = { proyecto.avancePorcentaje.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EtiquetaEstadoProyecto(estado: EstadoProyecto) {
    val texto = when (estado) {
        EstadoProyecto.EN_EJECUCION -> "En ejecución"
        EstadoProyecto.FINALIZADO -> "Finalizado"
    }
    val color = when (estado) {
        EstadoProyecto.EN_EJECUCION -> MaterialTheme.colorScheme.secondary
        EstadoProyecto.FINALIZADO -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(texto, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Medium)
}
