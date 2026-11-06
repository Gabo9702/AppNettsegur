package com.cgro.appnettsegur.ui.ingeniero

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cgro.appnettsegur.data.model.TipoDocumento
import com.cgro.appnettsegur.ui.compartido.MensajesRoles
import com.cgro.appnettsegur.ui.compartido.etiquetaTipoDocumento
import com.cgro.appnettsegur.ui.componentes.LocalMensajero
import com.cgro.appnettsegur.ui.componentes.SelectorLista
import com.cgro.appnettsegur.util.ArchivoUtils
import kotlinx.coroutines.launch

/**
 * Diálogo para cargar un documento (M2, fase 2, CU-05).
 *
 * El usuario elige el tipo y el archivo (PDF, JPG o PNG). El archivo se valida apenas se selecciona:
 * si el formato no se admite o pasa de 1,5 MB, se rechaza con un mensaje (flujo alterno de CU-05).
 * El propio diálogo es la confirmación antes de subir: explica que se notificará al cliente.
 */
@Composable
fun CargarDocumentoDialog(
    onDismiss: () -> Unit,
    onPublicar: (TipoDocumento, Uri) -> Unit,
) {
    val context = LocalContext.current
    val mensajero = LocalMensajero.current
    val scope = rememberCoroutineScope()

    var tipo by remember { mutableStateOf<TipoDocumento?>(null) }
    var uri by remember { mutableStateOf<Uri?>(null) }
    var nombre by remember { mutableStateOf<String?>(null) }

    val selector = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { elegido ->
        if (elegido == null) return@rememberLauncherForActivityResult
        // Validación del tipo y el tamaño ANTES de permitir publicar (flujo alterno de CU-05).
        scope.launch {
            runCatching { ArchivoUtils.leerYValidar(context, elegido) }
                .onSuccess { leido ->
                    uri = elegido
                    nombre = leido.nombre
                }
                .onFailure { mensajero(it.message ?: MensajesRoles.DESCARGA_ERROR) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(MensajesRoles.CARGAR_DOCUMENTO) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SelectorLista(
                    etiqueta = MensajesRoles.DOC_SELECCIONAR_TIPO,
                    opciones = TipoDocumento.entries,
                    seleccion = tipo,
                    textoDe = { etiquetaTipoDocumento(it) },
                    onSeleccion = { tipo = it },
                )
                OutlinedButton(onClick = {
                    selector.launch(arrayOf("application/pdf", "image/jpeg", "image/png"))
                }) {
                    Icon(Icons.Outlined.AttachFile, contentDescription = null)
                    Text("  " + (nombre ?: MensajesRoles.DOC_SELECCIONAR_ARCHIVO))
                }
                val t = tipo
                val u = uri
                if (t != null && u != null) {
                    Text(MensajesRoles.docConfirmarTexto(etiquetaTipoDocumento(t), nombre.orEmpty()))
                }
            }
        },
        confirmButton = {
            val t = tipo
            val u = uri
            TextButton(
                onClick = { if (t != null && u != null) onPublicar(t, u) },
                enabled = t != null && u != null,
            ) { Text(MensajesRoles.DOC_PUBLICAR) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
