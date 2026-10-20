package com.cgro.appnettsegur.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.cgro.appnettsegur.data.local.AppDatabase
import com.cgro.appnettsegur.data.local.Conteos
import com.cgro.appnettsegur.data.local.EstadoSync
import com.cgro.appnettsegur.data.local.EvidenciaEntity
import com.cgro.appnettsegur.data.local.NovedadEntity
import com.cgro.appnettsegur.data.local.ProyectoEntity
import com.cgro.appnettsegur.util.FirmaFormulario
import com.cgro.appnettsegur.util.FirmaUtils
import com.cgro.appnettsegur.util.ImagenUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

/** Foto del formulario: puede ser temporal (recién tomada) o ya guardada (al editar). */
data class EvidenciaFormulario(
    val idEvidencia: String,
    val rutaArchivo: String,
    val tamanoBytes: Long,
    val hashSha256: String,
    val ancho: Int,
    val alto: Int,
    val fechaCaptura: Long,
    val esTemporal: Boolean,
)

/** Acceso local (Room + archivos internos) a novedades, evidencias y proyectos. Nunca toca Firebase. */
class NovedadRepository(
    private val context: Context,
    private val db: AppDatabase,
) {
    private val novedadDao = db.novedadDao()
    private val evidenciaDao = db.evidenciaDao()
    private val proyectoDao = db.proyectoDao()

    fun observarNovedades(): Flow<List<NovedadEntity>> = novedadDao.observarTodas()
    fun observarNovedad(id: String): Flow<NovedadEntity?> = novedadDao.observarPorId(id)
    fun observarEvidencias(id: String): Flow<List<EvidenciaEntity>> = evidenciaDao.observarDeRegistro(id)
    fun observarConteos(): Flow<Conteos> = novedadDao.observarConteos()
    fun observarProyectos(): Flow<List<ProyectoEntity>> = proyectoDao.observarTodos()

    suspend fun obtenerNovedad(id: String): NovedadEntity? = novedadDao.obtenerPorId(id)
    suspend fun obtenerEvidencias(id: String): List<EvidenciaEntity> = evidenciaDao.obtenerDeRegistro(id)

    /**
     * GUARDAR: escribe SOLO en el dispositivo (archivos internos + Room). Nunca llama a Firebase.
     *
     * 1. Borra los archivos de las fotos que el usuario quitó.
     * 2. Mueve las fotos temporales a filesDir/evidencias/{idRegistro}_{n}.jpg.
     * 3. Exporta las firmas nuevas a PNG en filesDir/firmas/.
     * 4. Escribe la novedad y sus evidencias en una sola transacción de Room.
     */
    suspend fun guardar(
        novedad: NovedadEntity,
        evidencias: List<EvidenciaFormulario>,
        firmaTecnico: FirmaFormulario,
        firmaRepresentante: FirmaFormulario,
        grosorFirmaPx: Float,
    ) = withContext(Dispatchers.IO) {
        val id = novedad.idRegistro
        val carpetaEvidencias = ImagenUtils.carpetaEvidencias(context)

        // 1. Fotos que estaban guardadas y el usuario quitó al editar.
        val conservadas = evidencias.filter { !it.esTemporal }.map { it.rutaArchivo }.toSet()
        evidenciaDao.obtenerDeRegistro(id)
            .filter { it.rutaArchivo !in conservadas }
            .forEach { File(it.rutaArchivo).delete() }

        // 2. Fotos nuevas: del temporal a su nombre definitivo, buscando un número libre.
        val ocupadas = conservadas.toMutableSet()
        val finales = evidencias.map { evidencia ->
            if (!evidencia.esTemporal) return@map evidencia
            var n = 0
            var destino: File
            do {
                destino = File(carpetaEvidencias, "${id}_$n.jpg")
                n++
            } while (destino.exists() || destino.absolutePath in ocupadas)
            ImagenUtils.mover(File(evidencia.rutaArchivo), destino)
            ocupadas += destino.absolutePath
            evidencia.copy(rutaArchivo = destino.absolutePath, esTemporal = false)
        }

        // 3. Firmas: si hay trazos nuevos se exporta el PNG; si no, se conserva el existente.
        val carpetaFirmas = ImagenUtils.carpetaFirmas(context)
        val rutaTecnico = exportarFirma(firmaTecnico, File(carpetaFirmas, "${id}_tecnico.png"), grosorFirmaPx)
        val rutaRepresentante =
            exportarFirma(firmaRepresentante, File(carpetaFirmas, "${id}_representante.png"), grosorFirmaPx)

        // 4. Room, en una transacción: o se guarda todo o nada.
        db.withTransaction {
            novedadDao.guardar(
                novedad.copy(rutaFirmaTecnico = rutaTecnico, rutaFirmaRepresentante = rutaRepresentante)
            )
            evidenciaDao.eliminarDeRegistro(id)
            evidenciaDao.insertarTodas(finales.map {
                EvidenciaEntity(
                    idEvidencia = it.idEvidencia,
                    idRegistro = id,
                    rutaArchivo = it.rutaArchivo,
                    tamanoBytes = it.tamanoBytes,
                    hashSha256 = it.hashSha256,
                    ancho = it.ancho,
                    alto = it.alto,
                    fechaCaptura = it.fechaCaptura,
                )
            })
        }
    }

    /**
     * Elimina un registro PENDIENTE o ERROR junto con sus fotos y firmas.
     * Devuelve false si el registro ya estaba sincronizado (no se borra).
     */
    suspend fun eliminar(id: String): Boolean = withContext(Dispatchers.IO) {
        val novedad = novedadDao.obtenerPorId(id) ?: return@withContext true
        if (novedad.estadoSync == EstadoSync.SINCRONIZADO) return@withContext false
        evidenciaDao.obtenerDeRegistro(id).forEach { File(it.rutaArchivo).delete() }
        File(novedad.rutaFirmaTecnico).delete()
        File(novedad.rutaFirmaRepresentante).delete()
        novedadDao.eliminar(id) // las evidencias se borran en cascada
        true
    }

    /** Carga assets/proyectos.json en Room la primera vez, para que la selección funcione sin Internet. */
    suspend fun cargarCatalogoSiVacio() = withContext(Dispatchers.IO) {
        if (proyectoDao.contar() > 0) return@withContext
        val json = context.assets.open("proyectos.json").bufferedReader().use { it.readText() }
        val arreglo = JSONArray(json)
        val proyectos = (0 until arreglo.length()).map { i ->
            val o = arreglo.getJSONObject(i)
            ProyectoEntity(
                proyectoId = o.getString("proyectoId"),
                nombreProyecto = o.getString("nombreProyecto"),
                nombreCliente = o.getString("nombreCliente"),
            )
        }
        proyectoDao.insertarTodos(proyectos)
    }

    private suspend fun exportarFirma(firma: FirmaFormulario, destino: File, grosorPx: Float): String {
        if (firma.tieneTrazos) {
            FirmaUtils.exportarPng(firma, grosorPx, destino)
            return destino.absolutePath
        }
        return firma.rutaExistente ?: error("Firma vacía: la validación R6 debió impedir guardar")
    }
}
