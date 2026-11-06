package com.cgro.appnettsegur.ui.notificaciones

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.ui.componentes.TarjetaSeccion
import com.cgro.appnettsegur.ui.theme.ColorPendiente

/**
 * Tarjeta resumen de vencimientos para el inicio de RH (M3, fase 4): "N vencidos · M por vencer".
 *
 * Al mostrarse, dispara una vez la revisión que publica las alertas locales del día. M2 reemplazará
 * el inicio de RH, pero puede reutilizar esta tarjeta (o el ViewModel) tal cual.
 */
@Composable
fun ResumenVencimientosRh(
    modifier: Modifier = Modifier,
    viewModel: RhVencimientosViewModel = viewModel(factory = RhVencimientosViewModel.Factory),
) {
    val resumen by viewModel.resumen.collectAsStateWithLifecycle()

    // Al entrar a su inicio, RH recibe las alertas locales de hoy (flujo alterno de CU-07).
    LaunchedEffect(Unit) { viewModel.revisarYAvisar() }

    val r = resumen ?: return
    val hayPendientes = r.vencidos > 0 || r.porVencer > 0

    TarjetaSeccion(titulo = Mensajes.VENC_TITULO, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (hayPendientes) Icons.Outlined.WarningAmber else Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = if (hayPendientes) MaterialTheme.colorScheme.error else ColorPendiente,
            )
            Spacer(Modifier.size(10.dp))
            Text(
                if (hayPendientes) Mensajes.rhResumenVencimientos(r.vencidos, r.porVencer)
                else "Todos los documentos están al día.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
