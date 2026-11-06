package com.cgro.appnettsegur.ui.notificaciones

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.data.model.Notificacion
import com.cgro.appnettsegur.data.model.TipoNotificacion
import com.cgro.appnettsegur.notificaciones.AgrupadorFecha
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Bandeja de notificaciones (M3, fase 1).
 *
 * Lista agrupada por Hoy / Esta semana / Anteriores, con ícono por tipo y diferencia visual entre
 * leídas y no leídas. Al tocar una, se marca como leída y se navega al destino que corresponde
 * (MODULOS.md, fase 1 de M3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandejaScreen(
    onVolver: () -> Unit,
    onAbrirNovedad: (refId: String) -> Unit,
    onAbrirProyecto: (proyectoId: String) -> Unit,
    viewModel: BandejaViewModel = viewModel(factory = BandejaViewModel.Factory),
) {
    val s by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Mensajes.BANDEJA_TITULO) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Solo tiene sentido si hay alguna sin leer.
                    if (s.notificaciones.any { !it.leida }) {
                        IconButton(onClick = viewModel::marcarTodasLeidas) {
                            Icon(Icons.Outlined.DoneAll, contentDescription = Mensajes.BANDEJA_MARCAR_TODAS)
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (!s.cargando && s.notificaciones.isEmpty()) {
            BandejaVacia(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        // La agrupación por fecha se calcula aquí con el "ahora" real (probada aparte en AgrupadorFecha).
        val grupos = AgrupadorFecha.agrupar(s.notificaciones, System.currentTimeMillis())

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            grupos.forEach { (grupo, lista) ->
                item(key = "cabecera_${grupo.name}") {
                    Text(
                        grupo.titulo,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                }
                items(lista, key = { it.notificacionId }) { notif ->
                    FilaNotificacion(
                        notif = notif,
                        onClic = {
                            viewModel.marcarLeida(notif.notificacionId)
                            navegar(notif, onAbrirNovedad, onAbrirProyecto)
                        },
                    )
                }
            }
        }
    }
}

/** Navega al destino según el tipo (MODULOS.md, fase 1 de M3). */
private fun navegar(
    notif: Notificacion,
    onAbrirNovedad: (String) -> Unit,
    onAbrirProyecto: (String) -> Unit,
) = when (notif.tipo) {
    TipoNotificacion.NOVEDAD_SINCRONIZADA -> onAbrirNovedad(notif.refId)
    TipoNotificacion.DOCUMENTO_NUEVO, TipoNotificacion.CARTA_NUEVA -> onAbrirProyecto(notif.proyectoId)
}

@Composable
private fun FilaNotificacion(notif: Notificacion, onClic: () -> Unit) {
    // Diferencia visual: no leída = fondo tenue + punto; leída = plana.
    val fondo = if (notif.leida) MaterialTheme.colorScheme.surface
    else MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(fondo, MaterialTheme.shapes.medium)
            .clickable(onClick = onClic)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            iconoDe(notif.tipo),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), CircleShape)
                .padding(8.dp)
                .size(22.dp),
        )
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                notif.titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (notif.leida) FontWeight.Normal else FontWeight.SemiBold,
            )
            Text(
                notif.mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (notif.fecha > 0L) {
                Text(
                    fechaLegible(notif.fecha),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Punto de "no leída".
        if (!notif.leida) {
            Spacer(Modifier.size(8.dp))
            Box(
                Modifier
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape),
            )
        }
    }
}

@Composable
private fun BandejaVacia(modifier: Modifier) {
    Column(
        modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.NotificationsNone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.size(12.dp))
        Text(
            Mensajes.BANDEJA_VACIA,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun iconoDe(tipo: TipoNotificacion): ImageVector = when (tipo) {
    TipoNotificacion.NOVEDAD_SINCRONIZADA -> Icons.AutoMirrored.Outlined.Assignment
    TipoNotificacion.DOCUMENTO_NUEVO -> Icons.Outlined.Description
    TipoNotificacion.CARTA_NUEVA -> Icons.Outlined.Mail
}

private val formatoFecha: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm")

private fun fechaLegible(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDateTime().format(formatoFecha)
