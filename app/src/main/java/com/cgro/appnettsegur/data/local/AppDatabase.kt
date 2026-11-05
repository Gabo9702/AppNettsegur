package com.cgro.appnettsegur.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NovedadEntity::class, EvidenciaEntity::class, ProyectoEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun novedadDao(): NovedadDao
    abstract fun evidenciaDao(): EvidenciaDao
    abstract fun proyectoDao(): ProyectoDao

    companion object {

        /**
         * Migración 1→2 (MODULOS.md, sección 5.4). AGREGA columnas con ALTER TABLE para NO perder
         * los registros PENDIENTES/ERROR del técnico. NUNCA se usa fallbackToDestructiveMigration.
         */
        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE novedades ADD COLUMN uidTecnico TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE proyectos ADD COLUMN clienteUid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE proyectos ADD COLUMN ingenieroUid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE proyectos ADD COLUMN fechaActualizacion INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun construir(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "nettsegur.db")
                .addMigrations(MIGRACION_1_2)
                .build()
    }
}
