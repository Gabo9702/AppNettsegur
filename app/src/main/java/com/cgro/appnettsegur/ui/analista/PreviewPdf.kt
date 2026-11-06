package com.cgro.appnettsegur.ui.analista

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Renderiza la primera página de un PDF a un Bitmap para la vista previa (M2, fase 4).
 */
suspend fun renderizarPrimeraPagina(archivo: File, anchoPx: Int = 1000): Bitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            ParcelFileDescriptor.open(archivo, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.openPage(0).use { pagina ->
                        val alto = (anchoPx.toFloat() / pagina.width * pagina.height).toInt()
                        val bitmap = createBitmap(anchoPx, alto)
                        bitmap.eraseColor(Color.WHITE)
                        pagina.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        }.getOrNull()
    }
