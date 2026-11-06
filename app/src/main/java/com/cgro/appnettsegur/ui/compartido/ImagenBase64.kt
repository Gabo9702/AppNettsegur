package com.cgro.appnettsegur.ui.compartido

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

/**
 * Muestra una imagen codificada en Base64 (M2).
 *
 * Las evidencias y firmas viajan a Firebase como miniaturas Base64 (CLAUDE.md, sección 7);
 * aquí se decodifican a Bitmap para mostrarlas. Si el Base64 no es válido, se muestra un ícono.
 */
@Composable
fun ImagenBase64(
    base64: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val bitmap = remember(base64) {
        base64?.takeIf { it.isNotBlank() }?.let { dato ->
            runCatching {
                val bytes = Base64.decode(dato, Base64.NO_WRAP)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            Icon(
                Icons.Outlined.BrokenImage,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
