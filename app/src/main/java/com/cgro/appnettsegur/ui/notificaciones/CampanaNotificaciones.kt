package com.cgro.appnettsegur.ui.notificaciones

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cgro.appnettsegur.config.Mensajes

/**
 * Campana de notificaciones (M3, fase 1): ícono Outlined con el contador de no leídas.
 *
 * M1 dejó el slot `acciones` en BarraRol; M2 coloca esta campana en cada pantalla de inicio de rol.
 * El contador se limita a "9+" para no romper el diseño.
 */
@Composable
fun CampanaNotificaciones(
    onAbrir: () -> Unit,
    viewModel: CampanaViewModel = viewModel(factory = CampanaViewModel.Factory),
) {
    val noLeidas by viewModel.noLeidas.collectAsStateWithLifecycle()
    val descripcion = if (noLeidas > 0) Mensajes.campanaNoLeidas(noLeidas) else Mensajes.CAMPANA_DESC

    IconButton(onClick = onAbrir, modifier = androidx.compose.ui.Modifier.semantics { contentDescription = descripcion }) {
        BadgedBox(
            badge = {
                if (noLeidas > 0) {
                    Badge { Text(if (noLeidas > 9) "9+" else noLeidas.toString()) }
                }
            },
        ) {
            Icon(Icons.Outlined.Notifications, contentDescription = null)
        }
    }
}
