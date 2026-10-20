package com.cgro.appnettsegur.data.local

import androidx.room.TypeConverter

/** Guarda el enum [EstadoSync] como texto ("PENDIENTE", "SINCRONIZADO", "ERROR"). */
class Converters {
    @TypeConverter
    fun desdeEstado(estado: EstadoSync): String = estado.name

    @TypeConverter
    fun haciaEstado(valor: String): EstadoSync = EstadoSync.valueOf(valor)
}
