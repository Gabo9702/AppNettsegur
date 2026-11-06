package com.cgro.appnettsegur.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.cgro.appnettsegur.config.Limites
import com.cgro.appnettsegur.config.Mensajes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Lectura y validación de archivos para subir a RTDB como Base64 (MODULOS.md, sección 2).
 *
 * Cloud Storage necesita el plan Blaze, así que los archivos se guardan en archivos/{id} en
 * Base64, con un máximo de 1,5 MB por archivo original. Todo el trabajo corre en Dispatchers.IO.
 */
object ArchivoUtils {

    data class ArchivoLeido(val bytes: ByteArray, val mime: String, val nombre: String)

    /** Lee el archivo de la URI, validando tipo MIME y tamaño ANTES de subir. Lanza si no es válido. */
    suspend fun leerYValidar(context: Context, uri: Uri): ArchivoLeido = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        require(mime in Limites.MIMES_PERMITIDOS) { Mensajes.ARCHIVO_TIPO_NO_PERMITIDO }

        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException(Mensajes.ARCHIVO_NO_LEIDO)
        require(bytes.size <= Limites.MAX_ARCHIVO_BYTES) { Mensajes.ARCHIVO_MUY_GRANDE }

        ArchivoLeido(bytes, mime, nombreDe(context, uri, mime))
    }

    fun aBase64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    /** Decodifica un Base64 (archivos/{id}) a un archivo temporal en cacheDir, listo para abrir. */
    suspend fun guardarDesdeBase64(
        context: Context,
        base64: String,
        mime: String,
        nombreBase: String,
    ): File = withContext(Dispatchers.IO) {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        val carpeta = File(context.cacheDir, "descargas").apply { mkdirs() }
        val archivo = File(carpeta, "${nombreBase}.${extensionDe(mime)}")
        archivo.outputStream().use { it.write(bytes) }
        archivo
    }

    fun extensionDe(mime: String): String = when (mime) {
        "application/pdf" -> "pdf"
        "image/png" -> "png"
        else -> "jpg"
    }

    private fun nombreDe(context: Context, uri: Uri, mime: String): String {
        val nombre = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }.getOrNull()
        return nombre?.takeIf { it.isNotBlank() } ?: "archivo.${extensionDe(mime)}"
    }
}
