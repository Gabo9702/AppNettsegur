package com.cgro.appnettsegur.data.repository

import android.content.Context
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.config.RutasFirebase
import com.cgro.appnettsegur.data.remote.FirebaseFlows
import com.cgro.appnettsegur.data.remote.texto
import com.cgro.appnettsegur.util.ArchivoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Descarga de archivos (M1, fase C). Lee archivos/{id} (Base64) y lo decodifica a un archivo local
 * en cacheDir, listo para abrir con un visor. Solo se baja el Base64 al abrir, nunca en las listas.
 */
class ArchivosRepositoryImpl(private val context: Context) : ArchivosRepository {

    override suspend fun descargarAArchivoLocal(archivoId: String): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!FirebaseFlows.configurado(context)) throw IllegalStateException(Mensajes.FIREBASE_NO_CONFIGURADO)
                val snap = FirebaseFlows.ref(RutasFirebase.archivo(archivoId)).get().await()
                if (!snap.exists()) throw IllegalStateException(Mensajes.ARCHIVO_NO_ENCONTRADO)
                val base64 = snap.texto("base64")
                if (base64.isBlank()) throw IllegalStateException(Mensajes.ARCHIVO_NO_ENCONTRADO)
                val mime = snap.texto("mime")
                ArchivoUtils.guardarDesdeBase64(context, base64, mime, nombreBase = archivoId)
            }
        }
}
