package com.cgro.appnettsegur.ui.compartido

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Barra superior compartida por las pantallas de cada rol (MODULOS.md, sección 6.2).
 *
 * La crea M1. El slot [acciones] deja un espacio para la campana de notificaciones:
 * M3 entrega el composable CampanaNotificaciones() y M2 lo coloca aquí en cada inicio de rol.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraRol(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    navegacionIcono: @Composable (() -> Unit)? = null,
    colores: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    acciones: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = {
            if (subtitulo.isNullOrBlank()) {
                Text(titulo, style = MaterialTheme.typography.titleLarge)
            } else {
                androidx.compose.foundation.layout.Column {
                    Text(titulo, style = MaterialTheme.typography.titleLarge)
                    Text(
                        subtitulo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        navigationIcon = { navegacionIcono?.invoke() },
        actions = acciones,
        colors = colores,
    )
}
