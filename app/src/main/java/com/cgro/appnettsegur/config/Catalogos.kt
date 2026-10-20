package com.cgro.appnettsegur.config

/**
 * Listas de selección del formulario. Ajustar aquí para que coincidan
 * con el formato real de Nettsegur.
 */
object Catalogos {

    val tiposServicio = listOf(
        "Instalación",
        "Mantenimiento preventivo",
        "Mantenimiento correctivo",
        "Garantía",
        "Visita técnica",
    )

    val tiposActividad = listOf(
        "CCTV / cámaras",
        "Control de acceso",
        "Cercado eléctrico",
        "Alarmas y sensores",
        "Cableado y redes",
    )

    // Valores que se guardan en estadoAtencion y su texto visible.
    const val RESUELTA = "RESUELTA"
    const val NO_RESUELTA = "NO_RESUELTA"

    val estadosAtencion = linkedMapOf(
        RESUELTA to "Resuelta",
        NO_RESUELTA to "No resuelta",
    )
}
