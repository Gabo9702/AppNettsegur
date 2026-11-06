package com.cgro.appnettsegur.ui.notificaciones

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.cgro.appnettsegur.AppNettsegur
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.ui.componentes.LocalMensajero
import kotlinx.coroutines.launch

/**
 * Pide el permiso POST_NOTIFICATIONS en Android 13+ (M3, fase 2), con una explicación previa en un
 * diálogo propio. Se muestra una sola vez (se recuerda en DataStore). Si el usuario lo niega, la
 * bandeja sigue funcionando y se muestra un aviso discreto.
 *
 * Se coloca en el inicio del cliente y del ingeniero (los receptores de avisos del sistema).
 * En versiones anteriores a Android 13 no hace nada: el permiso no existe.
 */
@Composable
fun SolicitarPermisoNotificaciones() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val store = remember { (context.applicationContext as AppNettsegur).container.notificacionesStore }
    val alcance = rememberCoroutineScope()
    val mensajero = LocalMensajero.current

    var mostrarRazon by remember { mutableStateOf(false) }

    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (!concedido) mensajero(Mensajes.PERMISO_NOTIF_NEGADO)
    }

    LaunchedEffect(Unit) {
        val concedido = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        // Solo la primera vez que entra y si aún no lo concedió.
        if (!concedido && !store.permisoPreguntado()) mostrarRazon = true
    }

    if (mostrarRazon) {
        AlertDialog(
            onDismissRequest = {
                mostrarRazon = false
                alcance.launch { store.marcarPermisoPreguntado() }
            },
            icon = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
            title = { Text(Mensajes.PERMISO_NOTIF_TITULO) },
            text = { Text(Mensajes.PERMISO_NOTIF_TEXTO) },
            confirmButton = {
                TextButton(onClick = {
                    mostrarRazon = false
                    alcance.launch { store.marcarPermisoPreguntado() }
                    lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text(Mensajes.PERMISO_NOTIF_ACEPTAR) }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarRazon = false
                    alcance.launch { store.marcarPermisoPreguntado() }
                }) { Text(Mensajes.PERMISO_NOTIF_AHORA_NO) }
            },
        )
    }
}
