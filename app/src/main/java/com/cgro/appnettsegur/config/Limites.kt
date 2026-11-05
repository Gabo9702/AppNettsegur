package com.cgro.appnettsegur.config

/** Límites y parámetros que el docente puede pedir cambiar en vivo. */
object Limites {

    // Validación (R1, R3, R4, R5)
    const val MIN_MOTIVO_NO_RESUELTA = 20
    const val MIN_CEDULA = 6
    const val MAX_CEDULA = 10
    const val REGEX_SERIAL = "^[A-Za-z0-9]{4,20}$"
    const val MIN_DESC_REALIZADO = 30

    // Evidencias (R6)
    const val MIN_EVIDENCIAS = 1
    const val MAX_EVIDENCIAS = 3

    // Imágenes guardadas en el dispositivo
    const val LADO_MAX_IMAGEN_PX = 1600
    const val CALIDAD_JPEG_LOCAL = 80

    // Imágenes enviadas a Firebase (miniaturas en Base64)
    const val LADO_MAX_MINIATURA_PX = 640
    const val CALIDAD_JPEG_MINIATURA = 60
    const val LADO_MAX_FIRMA_PX = 400

    // Sincronización
    const val TIMEOUT_SYNC_MS = 15_000L
    const val NODO_FIREBASE = "novedades"

    // Listado
    const val CARACTERES_ID_CORTO = 8

    // ---- Módulos nuevos (MODULOS.md) ----

    // Documentos y archivos (sección 2): Cloud Storage necesita el plan Blaze, así que
    // los archivos se guardan en RTDB como Base64, con un límite por archivo original.
    const val MAX_ARCHIVO_BYTES = 1_500_000L // 1,5 MB
    val MIMES_PERMITIDOS = setOf("application/pdf", "image/jpeg", "image/png")

    // Cumplimiento (sección 6.2): un documento está POR_VENCER si le quedan 15 días o menos.
    const val DIAS_POR_VENCER = 15L

    // Notificaciones: últimas N que muestra la bandeja (lo usa M3).
    const val MAX_NOTIFICACIONES = 50
}
