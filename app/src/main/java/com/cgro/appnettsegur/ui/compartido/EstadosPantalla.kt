package com.cgro.appnettsegur.ui.compartido

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cgro.appnettsegur.config.Mensajes

/**
 * Estados de pantalla compartidos por los roles de M2 (carga, vacío, sin conexión).
 *
 * Las pantallas de los roles distintos al técnico funcionan EN LÍNEA (MODULOS.md, sección 3):
 * sin conexión no se cierran de golpe, muestran el mensaje y un botón "Reintentar".
 */

/** Indicador de carga centrado. */
@Composable
fun PantallaCargando(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

/** Estado vacío con ícono y mensaje (listas sin elementos). */
@Composable
fun PantallaVacia(
    mensaje: String,
    modifier: Modifier = Modifier,
    icono: ImageVector = Icons.Outlined.Inbox,
) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icono,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Text(
            mensaje,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Aviso de que la sección necesita Internet, con botón "Reintentar" (sección 3).
 * Se muestra cuando no hay conexión y todavía no se cargaron datos.
 */
@Composable
fun SeccionNecesitaInternet(onReintentar: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Text(
            Mensajes.SECCION_NECESITA_INTERNET,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = onReintentar) {
            Icon(Icons.Outlined.Refresh, contentDescription = null)
            Text("  Reintentar")
        }
    }
}
