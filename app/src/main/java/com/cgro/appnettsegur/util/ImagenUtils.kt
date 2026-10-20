package com.cgro.appnettsegur.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.cgro.appnettsegur.config.Limites
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/** Resultado de procesar una foto: una copia propia en almacenamiento interno. */
data class ImagenProcesada(
    val rutaArchivo: String,
    val tamanoBytes: Long,
    val hashSha256: String,
    val ancho: Int,
    val alto: Int,
)

object ImagenUtils {

    fun carpetaEvidencias(context: Context) = File(context.filesDir, "evidencias").apply { mkdirs() }
    fun carpetaTemporal(context: Context) = File(carpetaEvidencias(context), "tmp").apply { mkdirs() }
    fun carpetaFirmas(context: Context) = File(context.filesDir, "firmas").apply { mkdirs() }
    private fun carpetaCamara(context: Context) = File(context.cacheDir, "camara").apply { mkdirs() }

    /**
     * Archivo vacío en cacheDir donde la app de cámara escribirá la foto.
     * Se expone con FileProvider (ver res/xml/file_paths.xml) porque la cámara es otra app.
     */
    fun crearArchivoCamara(context: Context): Uri {
        val archivo = File(carpetaCamara(context), "foto_${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
    }

    /**
     * Lee la imagen de [uri], corrige la orientación EXIF, la reduce a lado mayor ≤ 1600 px,
     * la comprime a JPEG calidad 80 y guarda una COPIA PROPIA en filesDir/evidencias/tmp/.
     * Nunca guardamos la URI original: el permiso sobre ella se puede perder al cerrar la app.
     */
    suspend fun procesarEvidencia(context: Context, uri: Uri): ImagenProcesada = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        // 1. Leer solo las dimensiones para decodificar con un submuestreo y no agotar la memoria.
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // Con inJustDecodeBounds, decodeStream devuelve null a propósito: solo llena outWidth/outHeight.
        val entrada = resolver.openInputStream(uri) ?: throw IOException("No se pudo abrir la imagen")
        entrada.use { BitmapFactory.decodeStream(it, null, limites) }
        if (limites.outWidth <= 0 || limites.outHeight <= 0) throw IOException("El archivo no es una imagen")

        val opciones = BitmapFactory.Options().apply {
            inSampleSize = calcularSubmuestreo(limites.outWidth, limites.outHeight, Limites.LADO_MAX_IMAGEN_PX)
        }
        val original = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
            ?: throw IOException("No se pudo decodificar la imagen")

        // 2. Corregir la orientación según EXIF (las cámaras guardan la foto "acostada").
        val orientacion = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val orientada = aplicarOrientacion(original, orientacion)

        // 3. Redimensionar al tamaño final y comprimir a JPEG.
        val final = reducir(orientada, Limites.LADO_MAX_IMAGEN_PX)
        val destino = File(carpetaTemporal(context), "${UUID.randomUUID()}.jpg")
        destino.outputStream().use { final.compress(Bitmap.CompressFormat.JPEG, Limites.CALIDAD_JPEG_LOCAL, it) }

        val resultado = ImagenProcesada(
            rutaArchivo = destino.absolutePath,
            tamanoBytes = destino.length(),
            hashSha256 = Hash.sha256(destino),
            ancho = final.width,
            alto = final.height,
        )
        if (orientada !== original) orientada.recycle()
        if (final !== orientada) final.recycle()
        original.recycle()
        resultado
    }

    /**
     * Miniatura para Firebase: lado mayor ≤ [ladoMax], en Base64. La imagen original
     * se queda en el dispositivo; a RTDB solo viaja esta versión liviana.
     */
    suspend fun miniaturaBase64(
        archivo: File,
        ladoMax: Int,
        formato: Bitmap.CompressFormat,
        calidad: Int,
    ): String = withContext(Dispatchers.IO) {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(archivo.absolutePath, limites)
        val opciones = BitmapFactory.Options().apply {
            inSampleSize = calcularSubmuestreo(limites.outWidth, limites.outHeight, ladoMax)
        }
        val bitmap = BitmapFactory.decodeFile(archivo.absolutePath, opciones)
            ?: throw IOException("No se pudo leer ${archivo.name}")
        val miniatura = reducir(bitmap, ladoMax)
        val bytes = ByteArrayOutputStream().use { salida ->
            miniatura.compress(formato, calidad, salida)
            salida.toByteArray()
        }
        if (miniatura !== bitmap) miniatura.recycle()
        bitmap.recycle()
        Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Mueve una foto temporal a su ubicación definitiva. Si renameTo falla (otro volumen),
     * copia y borra el temporal.
     */
    fun mover(origen: File, destino: File) {
        if (origen.absolutePath == destino.absolutePath) return
        destino.parentFile?.mkdirs()
        if (!origen.renameTo(destino)) {
            origen.copyTo(destino, overwrite = true)
            origen.delete()
        }
    }

    /** Borra las fotos temporales que quedaron de formularios cancelados o de la cámara. */
    fun limpiarTemporales(context: Context) {
        carpetaTemporal(context).listFiles()?.forEach { it.delete() }
        carpetaCamara(context).listFiles()?.forEach { it.delete() }
    }

    /** Mayor potencia de 2 que deja la imagen todavía por encima de [ladoMax]. */
    private fun calcularSubmuestreo(ancho: Int, alto: Int, ladoMax: Int): Int {
        var muestreo = 1
        while (max(ancho, alto) / (muestreo * 2) >= ladoMax) muestreo *= 2
        return muestreo
    }

    private fun reducir(bitmap: Bitmap, ladoMax: Int): Bitmap {
        val ladoMayor = max(bitmap.width, bitmap.height)
        if (ladoMayor <= ladoMax) return bitmap
        val escala = ladoMax.toFloat() / ladoMayor
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * escala).roundToInt().coerceAtLeast(1),
            (bitmap.height * escala).roundToInt().coerceAtLeast(1),
            true
        )
    }

    private fun aplicarOrientacion(bitmap: Bitmap, orientacion: Int): Bitmap {
        val matriz = Matrix()
        when (orientacion) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matriz.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matriz.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matriz.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matriz.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matriz.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matriz.postRotate(90f); matriz.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matriz.postRotate(270f); matriz.postScale(-1f, 1f) }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
    }
}
