package com.cgro.appnettsegur.ui.compartido

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Abre un archivo ya descargado al dispositivo con el visor del sistema (M2).
 *
 * Usa el FileProvider declarado en el manifiesto (authority "${applicationId}.fileprovider") para
 * entregar la URI con permiso de lectura temporal. Los archivos se guardan en cacheDir (sección 2:
 * no hay Cloud Storage; el contenido se descarga de RTDB en Base64 y se decodifica al abrir).
 *
 * Devuelve false si no hay ninguna app capaz de abrir ese tipo de archivo.
 */
fun abrirArchivo(context: Context, archivo: File, mime: String): Boolean {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
