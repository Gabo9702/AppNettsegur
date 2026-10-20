package com.cgro.appnettsegur.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Formatos {
    private val fecha = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val fechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    fun fecha(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(fecha)
    fun fechaHora(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(fechaHora)
    fun idCorto(id: String, caracteres: Int): String = id.take(caracteres)
}
