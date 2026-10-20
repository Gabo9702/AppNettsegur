package com.cgro.appnettsegur.data.repository

import android.graphics.Bitmap
import com.cgro.appnettsegur.BuildConfig
import com.cgro.appnettsegur.config.Limites
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.data.local.AppDatabase
import com.cgro.appnettsegur.data.local.EvidenciaEntity
import com.cgro.appnettsegur.data.local.NovedadEntity
import com.cgro.appnettsegur.data.remote.FirebaseNovedadesDataSource
import com.cgro.appnettsegur.util.ConectividadMonitor
import com.cgro.appnettsegur.util.ImagenUtils
import com.google.firebase.database.DatabaseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** Progreso visible en la UI mientras se sincroniza. */
data class ProgresoSync(val sincronizando: Boolean = false, val actual: Int = 0, val total: Int = 0)

/** Error lanzado a propósito por el simulador de fallos (solo debug, Prueba E). */
class FalloSimuladoException : Exception(Mensajes.SYNC_FALLO_SIMULADO)

/** Registro sin sus archivos locales: se marca ERROR sin intentar enviarlo. */
private class ArchivosFaltantesException(mensaje: String) : Exception(mensaje)

/**
 * Sincronización MANUAL (CLAUDE.md, sección 9.2). Solo se ejecuta cuando el usuario pulsa
 * SINCRONIZAR o "Reintentar". No hay WorkManager, ni reintentos en segundo plano, ni envío al reconectar.
 */
class SyncRepository(
    private val db: AppDatabase,
    private val firebase: FirebaseNovedadesDataSource,
    private val conectividad: ConectividadMonitor,
) {
    private val novedadDao = db.novedadDao()
    private val evidenciaDao = db.evidenciaDao()

    // Bandera para ignorar un segundo toque mientras ya se está sincronizando.
    private val sincronizando = AtomicBoolean(false)

    private val _progreso = MutableStateFlow(ProgresoSync())
    val progreso: StateFlow<ProgresoSync> = _progreso.asStateFlow()

    /** Solo debug: posición (1, 2, 3…) del registro que debe fallar a propósito. null = desactivado. */
    val falloSimuladoEn = MutableStateFlow<Int?>(null)

    /** Devuelve el mensaje que la UI debe mostrar al terminar, o null si ya había otra sincronización. */
    suspend fun sincronizar(): String? {
        if (!sincronizando.compareAndSet(false, true)) return null
        try {
            if (!conectividad.hayInternetValidado()) return Mensajes.SYNC_SIN_CONEXION
            if (!firebase.estaConfigurado()) return Mensajes.SYNC_FIREBASE_NO_CONFIGURADO

            val lista = novedadDao.obtenerNoSincronizados() // PENDIENTE y ERROR, del más antiguo al más nuevo
            if (lista.isEmpty()) return Mensajes.SYNC_NADA_PENDIENTE

            var ok = 0
            var conError = 0
            // UNO POR UNO: el fallo de un registro no detiene a los demás.
            lista.forEachIndexed { indice, registro ->
                _progreso.value = ProgresoSync(sincronizando = true, actual = indice + 1, total = lista.size)
                try {
                    if (BuildConfig.DEBUG && falloSimuladoEn.value == indice + 1) throw FalloSimuladoException()

                    val evidencias = evidenciaDao.obtenerDeRegistro(registro.idRegistro)
                    validarArchivos(registro, evidencias)
                    val mapa = aMapaFirebase(registro, evidencias)

                    // El SDK de RTDB encola las escrituras sin conexión y no devuelve error: sin este
                    // timeout el botón quedaría esperando para siempre. Si la escritura llega tarde,
                    // cae sobre la misma llave (idRegistro) y no se duplica.
                    withTimeout(Limites.TIMEOUT_SYNC_MS) {
                        firebase.escribir(registro.idRegistro, mapa)
                    }
                    // Solo se marca SINCRONIZADO después de que Firebase confirmó.
                    novedadDao.marcarSincronizado(registro.idRegistro, System.currentTimeMillis())
                    ok++
                } catch (e: CancellationException) {
                    if (e !is TimeoutCancellationException) throw e // cancelación real: no ocultarla
                    novedadDao.marcarError(registro.idRegistro, mensajeLegible(e))
                    conError++
                } catch (e: Exception) {
                    novedadDao.marcarError(registro.idRegistro, mensajeLegible(e))
                    conError++
                }
            }
            return Mensajes.resumenSync(ok, conError)
        } finally {
            _progreso.value = ProgresoSync()
            sincronizando.set(false)
        }
    }

    private fun validarArchivos(registro: NovedadEntity, evidencias: List<EvidenciaEntity>) {
        fun disponible(ruta: String) = File(ruta).let { it.exists() && it.length() > 0 }
        if (evidencias.isEmpty() || evidencias.any { !disponible(it.rutaArchivo) }) {
            throw ArchivosFaltantesException(Mensajes.SYNC_FALTA_EVIDENCIA)
        }
        if (!disponible(registro.rutaFirmaTecnico) || !disponible(registro.rutaFirmaRepresentante)) {
            throw ArchivosFaltantesException(Mensajes.SYNC_FALTA_FIRMA)
        }
    }

    /** Estructura de novedades/{idRegistro} (sección 9.1). Las imágenes viajan como miniaturas Base64. */
    private suspend fun aMapaFirebase(r: NovedadEntity, evidencias: List<EvidenciaEntity>): Map<String, Any?> {
        val fotos = evidencias.mapIndexed { i, e ->
            i.toString() to mapOf(
                "base64" to ImagenUtils.miniaturaBase64(
                    File(e.rutaArchivo), Limites.LADO_MAX_MINIATURA_PX,
                    Bitmap.CompressFormat.JPEG, Limites.CALIDAD_JPEG_MINIATURA
                ),
                "ancho" to e.ancho,
                "alto" to e.alto,
                "hashSha256" to e.hashSha256,
            )
        }.toMap()

        val firmas = mapOf(
            "tecnico" to ImagenUtils.miniaturaBase64(
                File(r.rutaFirmaTecnico), Limites.LADO_MAX_FIRMA_PX, Bitmap.CompressFormat.PNG, 100
            ),
            "representante" to ImagenUtils.miniaturaBase64(
                File(r.rutaFirmaRepresentante), Limites.LADO_MAX_FIRMA_PX, Bitmap.CompressFormat.PNG, 100
            ),
        )

        return mapOf(
            "idRegistro" to r.idRegistro,
            "proyectoId" to r.proyectoId,
            "nombreCliente" to r.nombreCliente,
            "nombreTecnico" to r.nombreTecnico,
            "fechaServicio" to r.fechaServicio,
            "horaInicio" to r.horaInicio,
            "horaFin" to r.horaFin,
            "tipoServicio" to r.tipoServicio,
            "tipoActividad" to r.tipoActividad,
            "serialEquipo" to r.serialEquipo,
            "descServAsignado" to r.descServAsignado,
            "estadoAtencion" to r.estadoAtencion,
            "descServRealizado" to r.descServRealizado,
            "motivoNoResuelta" to r.motivoNoResuelta,
            "observacionesRecomendaciones" to r.observacionesRecomendaciones,
            "nombreRepresentanteCliente" to r.nombreRepresentanteCliente,
            "cedulaRepresentanteCliente" to r.cedulaRepresentanteCliente,
            "rolRepresentanteCliente" to r.rolRepresentanteCliente,
            "fechaCreacion" to r.fechaCreacion,
            "evidencias" to fotos,
            "firmas" to firmas,
        )
    }

    /** Convierte la excepción en un texto que el técnico pueda entender. */
    private fun mensajeLegible(e: Throwable): String = when {
        e is TimeoutCancellationException -> Mensajes.SYNC_TIMEOUT
        e is FalloSimuladoException || e is ArchivosFaltantesException -> e.message ?: Mensajes.SYNC_ERROR_DESCONOCIDO
        e is DatabaseException && e.message?.contains("permission", ignoreCase = true) == true ->
            Mensajes.SYNC_PERMISO_DENEGADO
        e.message?.contains("permission", ignoreCase = true) == true -> Mensajes.SYNC_PERMISO_DENEGADO
        else -> e.message?.takeIf { it.isNotBlank() } ?: Mensajes.SYNC_ERROR_DESCONOCIDO
    }
}
