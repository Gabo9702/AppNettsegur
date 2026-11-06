package com.cgro.appnettsegur.ui.cliente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.ui.compartido.BarraRol
import com.cgro.appnettsegur.ui.compartido.MensajesRoles
import com.cgro.appnettsegur.ui.compartido.PantallaCargando
import com.cgro.appnettsegur.ui.compartido.PantallaVacia
import com.cgro.appnettsegur.ui.compartido.SeccionNecesitaInternet
import com.cgro.appnettsegur.ui.compartido.TarjetaProyecto
import com.cgro.appnettsegur.ui.notificaciones.CampanaNotificaciones
import com.cgro.appnettsegur.ui.notificaciones.SolicitarPermisoNotificaciones

/**
 * Inicio del cliente (M2, fase 1): lista "Mis proyectos" con una tarjeta por proyecto
 * (nombre, estado y barra de avance). La campana (M3) va en la BarraRol compartida.
 */
@Composable
fun InicioClienteScreen(
    nombreUsuario: String,
    onAbrirProyecto: (String) -> Unit,
    onBandeja: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: InicioClienteViewModel = viewModel(factory = InicioClienteViewModel.Factory),
) {
    val s by viewModel.estado.collectAsStateWithLifecycle()
    val hayInternet by viewModel.hayInternet.collectAsStateWithLifecycle()

    // M3: el cliente (receptor de avisos del sistema) pide el permiso de notificaciones al entrar.
    SolicitarPermisoNotificaciones()

    Scaffold(
        topBar = {
            BarraRol(
                titulo = MensajesRoles.INICIO_CLIENTE_TITULO,
                subtitulo = nombreUsuario.takeIf { it.isNotBlank() },
                acciones = {
                    CampanaNotificaciones(onAbrir = onBandeja)
                    IconButton(onClick = onCerrarSesion) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = Mensajes.CERRAR_SESION)
                    }
                },
            )
        },
    ) { padding ->
        when {
            s.cargando && s.proyectos.isEmpty() ->
                PantallaCargando(Modifier.padding(padding))

            s.proyectos.isEmpty() && !hayInternet ->
                SeccionNecesitaInternet(onReintentar = viewModel::reintentar, modifier = Modifier.padding(padding))

            s.proyectos.isEmpty() ->
                PantallaVacia(MensajesRoles.SIN_PROYECTOS, Modifier.padding(padding), Icons.Outlined.FolderOff)

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 16.dp, bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(s.proyectos, key = { it.proyectoId }) { proyecto ->
                    TarjetaProyecto(proyecto, onAbrir = { onAbrirProyecto(proyecto.proyectoId) })
                }
            }
        }
    }
}
