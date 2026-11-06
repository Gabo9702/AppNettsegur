package com.cgro.appnettsegur.ui.rh

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
import com.cgro.appnettsegur.data.model.TipoCumplimiento
import com.cgro.appnettsegur.ui.compartido.MensajesRoles
import com.cgro.appnettsegur.ui.componentes.LocalMensajero
import com.cgro.appnettsegur.ui.componentes.TextoError
import com.cgro.appnettsegur.util.ArchivoUtils
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Diálogo para cargar un documento de cumplimiento (ARL, seguridad social o alturas) con sus fechas
 * de expedición y vencimiento (M2, fase 3, CU-07).
 *
 * Valida: la expedición no puede ser futura y el vencimiento debe ser posterior a la expedición.
 * El archivo (PDF/JPG/PNG, ≤1,5 MB) se valida al seleccionarlo.
 */
@Composable
fun CargarCumplimientoDialog(
    tipo: TipoCumplimiento,
    onDismiss: () -> Unit,
    onConfirmar: (LocalDate, LocalDate, Uri) -> Unit,
) {
    val context = LocalContext.current
    val mensajero = LocalMensajero.current
    val scope = rememberCoroutineScope()

    var expedicion by remember { mutableStateOf<LocalDate?>(null) }
    var vencimiento by remember { mutableStateOf<LocalDate?>(null) }
    var uri by remember { mutableStateOf<Uri?>(null) }
    var nombre by remember { mutableStateOf<String?>(null) }
    var errorFechas by remember { mutableStateOf<String?>(null) }

    val selector = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { elegido ->
        if (elegido == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { ArchivoUtils.leerYValidar(context, elegido) }
                .onSuccess { uri = elegido; nombre = it.nombre }
                .onFailure { mensajero(it.message ?: MensajesRoles.DESCARGA_ERROR) }
        }
    }

    fun fechasValidas(): Boolean {
        val e = expedicion
        val v = vencimiento
        return e != null && v != null && !e.isAfter(LocalDate.now()) && v.isAfter(e)
    }

    val nombreDoc = MensajesRoles.NOMBRE_CUMPLIMIENTO[tipo.name] ?: tipo.name

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(nombreDoc) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SelectorFecha(MensajesRoles.FECHA_EXPEDICION, expedicion, onFecha = { expedicion = it; errorFechas = null })
                SelectorFecha(MensajesRoles.FECHA_VENCIMIENTO, vencimiento, onFecha = { vencimiento = it; errorFechas = null })
                OutlinedButton(onClick = { selector.launch(arrayOf("application/pdf", "image/jpeg", "image/png")) }) {
                    Icon(Icons.Outlined.AttachFile, contentDescription = null)
                    Text("  " + (nombre ?: MensajesRoles.CUMPLIMIENTO_CARGAR))
                }
                TextoError(errorFechas)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val e = expedicion
                val v = vencimiento
                val u = uri
                when {
                    u == null -> mensajero(MensajesRoles.DOC_SELECCIONAR_ARCHIVO)
                    !fechasValidas() -> errorFechas = MensajesRoles.CUMPLIMIENTO_FECHAS_INVALIDAS
                    else -> onConfirmar(e!!, v!!, u)
                }
            }) { Text(MensajesRoles.EMP_GUARDAR) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
