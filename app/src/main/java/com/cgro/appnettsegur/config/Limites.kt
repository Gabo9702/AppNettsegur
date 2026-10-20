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
}
