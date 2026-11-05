package com.cgro.appnettsegur.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.ui.compartido.BarraRol

/**
 * Pantallas de RELLENO de la fase A (MODULOS.md, sección 6.2).
 *
 * M1 crea los destinos de navegación con estas pantallas provisionales. M2 reemplaza los inicios
 * de rol y el detalle de proyecto/empleado; M3 reemplaza la bandeja. Se dejan funcionales (con
 * barra, campana que abre la bandeja y cerrar sesión) para que M3 pueda probar la navegación.
 */

/** Inicio de relleno de un rol distinto al técnico: muestra el nombre del rol. */
@Composable
fun InicioRolRelleno(
    tituloRol: String,
    nombreUsuario: String,
    onBandeja: () -> Unit,
    onCerrarSesion: () -> Unit,
) {
    Scaffold(
        topBar = {
            BarraRol(
                titulo = tituloRol,
                subtitulo = nombreUsuario.takeIf { it.isNotBlank() },
                acciones = {
                    // Espacio de la campana (M3): por ahora un acceso directo a la bandeja.
                    IconButton(onClick = onBandeja) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones")
                    }
                    IconButton(onClick = onCerrarSesion) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = Mensajes.CERRAR_SESION)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                Mensajes.pantallaEnConstruccion(tituloRol),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Pantalla de relleno simple con botón de volver (detalle de proyecto/empleado, bandeja, novedad). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaRellenoSimple(titulo: String, onVolver: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                Mensajes.pantallaEnConstruccion(titulo),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
