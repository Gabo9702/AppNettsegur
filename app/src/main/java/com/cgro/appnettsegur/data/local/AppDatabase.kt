package com.cgro.appnettsegur.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [NovedadEntity::class, EvidenciaEntity::class, ProyectoEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun novedadDao(): NovedadDao
    abstract fun evidenciaDao(): EvidenciaDao
    abstract fun proyectoDao(): ProyectoDao

    companion object {
        fun construir(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "nettsegur.db").build()
    }
}
