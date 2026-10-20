package com.cgro.appnettsegur.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NovedadDao {

    @Upsert
    suspend fun guardar(novedad: NovedadEntity)

    @Query("SELECT * FROM novedades ORDER BY fechaCreacion DESC")
    fun observarTodas(): Flow<List<NovedadEntity>>

    @Query("SELECT * FROM novedades WHERE idRegistro = :id")
    fun observarPorId(id: String): Flow<NovedadEntity?>

    @Query("SELECT * FROM novedades WHERE idRegistro = :id")
    suspend fun obtenerPorId(id: String): NovedadEntity?

    // Registros que SINCRONIZAR debe enviar, del más antiguo al más reciente.
    @Query(
        "SELECT * FROM novedades WHERE estadoSync IN ('PENDIENTE', 'ERROR') " +
            "ORDER BY fechaCreacion ASC"
    )
    suspend fun obtenerNoSincronizados(): List<NovedadEntity>

    // Solo se llama después de que Firebase confirma la escritura.
    @Query(
        "UPDATE novedades SET estadoSync = 'SINCRONIZADO', fechaSincronizacion = :fecha, " +
            "mensajeError = NULL WHERE idRegistro = :id"
    )
    suspend fun marcarSincronizado(id: String, fecha: Long)

    @Query("UPDATE novedades SET estadoSync = 'ERROR', mensajeError = :mensaje WHERE idRegistro = :id")
    suspend fun marcarError(id: String, mensaje: String)

    @Query("DELETE FROM novedades WHERE idRegistro = :id")
    suspend fun eliminar(id: String)

    @Query(
        "SELECT COUNT(*) AS total, " +
            "COALESCE(SUM(CASE WHEN estadoSync = 'PENDIENTE' THEN 1 ELSE 0 END), 0) AS pendientes, " +
            "COALESCE(SUM(CASE WHEN estadoSync = 'SINCRONIZADO' THEN 1 ELSE 0 END), 0) AS sincronizados, " +
            "COALESCE(SUM(CASE WHEN estadoSync = 'ERROR' THEN 1 ELSE 0 END), 0) AS conError " +
            "FROM novedades"
    )
    fun observarConteos(): Flow<Conteos>
}

@Dao
interface EvidenciaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(evidencias: List<EvidenciaEntity>)

    @Query("DELETE FROM evidencias WHERE idRegistro = :idRegistro")
    suspend fun eliminarDeRegistro(idRegistro: String)

    @Query("SELECT * FROM evidencias WHERE idRegistro = :idRegistro ORDER BY fechaCaptura ASC")
    suspend fun obtenerDeRegistro(idRegistro: String): List<EvidenciaEntity>

    @Query("SELECT * FROM evidencias WHERE idRegistro = :idRegistro ORDER BY fechaCaptura ASC")
    fun observarDeRegistro(idRegistro: String): Flow<List<EvidenciaEntity>>
}

@Dao
interface ProyectoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(proyectos: List<ProyectoEntity>)

    @Query("SELECT COUNT(*) FROM proyectos")
    suspend fun contar(): Int

    @Query("SELECT * FROM proyectos ORDER BY nombreProyecto ASC")
    fun observarTodos(): Flow<List<ProyectoEntity>>
}
