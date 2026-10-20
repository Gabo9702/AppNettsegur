package com.cgro.appnettsegur.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Trazos dibujados en el pad de firma, en píxeles del lienzo. */
data class FirmaFormulario(
    val trazos: List<List<Offset>> = emptyList(),
    val anchoPx: Int = 0,
    val altoPx: Int = 0,
    /** Ruta del PNG ya guardado (al editar un registro). Null si hay que firmar de nuevo. */
    val rutaExistente: String? = null,
) {
    val tieneTrazos: Boolean get() = trazos.any { it.isNotEmpty() }

    /** Una firma sin trazos se considera vacía; una firma guardada debe existir y pesar más de 0 bytes. */
    val esValida: Boolean
        get() = tieneTrazos || rutaExistente?.let { File(it).let { f -> f.exists() && f.length() > 0 } } == true
}

object FirmaUtils {

    /** Dibuja los trazos en negro sobre fondo blanco y los guarda como PNG en [destino]. */
    suspend fun exportarPng(firma: FirmaFormulario, grosorPx: Float, destino: File) = withContext(Dispatchers.IO) {
        val bitmap = Bitmap.createBitmap(
            firma.anchoPx.coerceAtLeast(1),
            firma.altoPx.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val lienzo = Canvas(bitmap)
        lienzo.drawColor(Color.WHITE)
        val pincel = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = grosorPx
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        firma.trazos.filter { it.isNotEmpty() }.forEach { trazo ->
            if (trazo.size == 1) {
                lienzo.drawPoint(trazo[0].x, trazo[0].y, pincel)
            } else {
                val camino = Path().apply {
                    moveTo(trazo[0].x, trazo[0].y)
                    trazo.drop(1).forEach { lineTo(it.x, it.y) }
                }
                lienzo.drawPath(camino, pincel)
            }
        }
        destino.parentFile?.mkdirs()
        destino.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
