package com.cgro.appnettsegur.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cgro.appnettsegur.util.FirmaFormulario
import coil3.compose.AsyncImage
import java.io.File

/** Grosor del trazo en dp; se usa igual al dibujar y al exportar el PNG. */
val GROSOR_FIRMA = 3.dp

/**
 * Pad de firma propio en Compose. Mientras el dedo se mueve, el trazo actual vive aquí;
 * al levantar el dedo se entrega la lista completa de trazos al ViewModel.
 * Si [firma] ya tiene un PNG guardado (edición), se muestra la imagen con la opción "Rehacer".
 */
@Composable
fun FirmaPad(
    titulo: String,
    firma: FirmaFormulario,
    onCambio: (FirmaFormulario) -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
) {
    val firmaActual by rememberUpdatedState(firma)
    val trazoEnCurso = remember { mutableStateListOf<Offset>() }
    val colorBorde = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
    val grosorPx = with(LocalDensity.current) { GROSOR_FIRMA.toPx() }
    val mostrarGuardada = !firma.tieneTrazos && firma.rutaExistente != null

    Column(modifier) {
        Text(titulo, style = MaterialTheme.typography.labelLarge)
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color.White, MaterialTheme.shapes.small)
                .border(BorderStroke(1.dp, colorBorde), MaterialTheme.shapes.small)
                .semantics { contentDescription = "Área de firma: $titulo" }
        ) {
            if (mostrarGuardada) {
                AsyncImage(
                    model = File(firma.rutaExistente!!),
                    contentDescription = "Firma guardada: $titulo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .onSizeChanged { tamano ->
                            if (tamano.width != firmaActual.anchoPx || tamano.height != firmaActual.altoPx) {
                                onCambio(firmaActual.copy(anchoPx = tamano.width, altoPx = tamano.height))
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { punto ->
                                onCambio(firmaActual.copy(trazos = firmaActual.trazos + listOf(listOf(punto))))
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { punto ->
                                    trazoEnCurso.clear()
                                    trazoEnCurso.add(punto)
                                },
                                onDrag = { cambio, _ -> trazoEnCurso.add(cambio.position) },
                                onDragEnd = {
                                    onCambio(firmaActual.copy(trazos = firmaActual.trazos + listOf(trazoEnCurso.toList())))
                                    trazoEnCurso.clear()
                                },
                                onDragCancel = { trazoEnCurso.clear() },
                            )
                        }
                ) {
                    val estilo = Stroke(width = grosorPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    (firma.trazos + listOf(trazoEnCurso.toList())).forEach { trazo ->
                        when (trazo.size) {
                            0 -> Unit
                            1 -> drawCircle(Color.Black, radius = grosorPx / 2, center = trazo[0])
                            else -> drawPath(
                                Path().apply {
                                    moveTo(trazo[0].x, trazo[0].y)
                                    trazo.drop(1).forEach { lineTo(it.x, it.y) }
                                },
                                color = Color.Black,
                                style = estilo,
                            )
                        }
                    }
                }
                if (!firma.tieneTrazos && trazoEnCurso.isEmpty()) {
                    Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Draw, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(" Firme aquí", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        TextoError(error)
        OutlinedButton(
            onClick = { onCambio(FirmaFormulario(anchoPx = firma.anchoPx, altoPx = firma.altoPx)) },
            enabled = firma.tieneTrazos || mostrarGuardada,
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = null)
            Text(if (mostrarGuardada) " Rehacer firma" else " Limpiar")
        }
    }
}
