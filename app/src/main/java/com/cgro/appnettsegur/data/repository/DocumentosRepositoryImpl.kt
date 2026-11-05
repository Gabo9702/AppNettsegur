package com.cgro.appnettsegur.data.repository

import android.content.Context
import android.net.Uri
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.config.RutasFirebase
import com.cgro.appnettsegur.data.model.Documento
import com.cgro.appnettsegur.data.model.TipoDocumento
import com.cgro.appnettsegur.data.remote.FirebaseFlows
import com.cgro.appnettsegur.data.remote.aDocumento
import com.cgro.appnettsegur.data.remote.texto
import com.cgro.appnettsegur.notificaciones.NotificacionFactory
import com.cgro.appnettsegur.util.ArchivoUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Documentos de un proyecto (M1, fase C). El contenido pesado va en archivos/{id} en Base64;
 * documentos/{proyectoId}/{id} guarda solo los metadatos.
 */
class DocumentosRepositoryImpl(private val context: Context) : DocumentosRepository {

    private val configurado get() = FirebaseFlows.configurado(context)

    override fun documentos(proyectoId: String): Flow<List<Documento>> {
        if (!configurado) return flowOf(emptyList())
        return FirebaseFlows.flujoValor(FirebaseFlows.ref(RutasFirebase.documentos(proyectoId))) { snap ->
            snap.children.mapNotNull { it.aDocumento(proyectoId) }
        }
    }

    /**
     * Sube un documento: valida tipo y tamaño, y escribe en UNA sola operación multi-ruta el
     * metadato (documentos/...), el contenido (archivos/...) y la notificación al cliente (CU-05).
     */
    override suspend fun subir(proyectoId: String, tipo: TipoDocumento, uri: Uri): Result<Documento> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!configurado) throw IllegalStateException(Mensajes.FIREBASE_NO_CONFIGURADO)
                val subidoPor = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw IllegalStateException(Mensajes.FIREBASE_NO_CONFIGURADO)

                val leido = ArchivoUtils.leerYValidar(context, uri)
                val archivoId = UUID.randomUUID().toString()
                val documentoId = UUID.randomUUID().toString()
                val ahora = System.currentTimeMillis()

                val documento = Documento(
                    documentoId = documentoId,
                    proyectoId = proyectoId,
                    tipo = tipo,
                    nombre = leido.nombre,
                    archivoId = archivoId,
                    mime = leido.mime,
                    tamanoBytes = leido.bytes.size.toLong(),
                    subidoPor = subidoPor,
                    fecha = ahora,
                )

                // El cliente del proyecto recibe la notificación.
                val clienteUid = FirebaseFlows.ref(RutasFirebase.proyecto(proyectoId)).get().await()
                    .texto("clienteUid")

                val updates = buildMap<String, Any?> {
                    put(RutasFirebase.documento(proyectoId, documentoId), documento.aMapa())
                    put(RutasFirebase.archivo(archivoId), archivoMapa(leido, subidoPor, ahora))
                    if (clienteUid.isNotBlank()) {
                        putAll(NotificacionFactory.documentoNuevo(clienteUid, proyectoId, documento))
                    }
                }
                FirebaseFlows.raiz().updateChildren(updates).await()
                documento
            }
        }

    private fun Documento.aMapa(): Map<String, Any?> = mapOf(
        "tipo" to tipo.name,
        "nombre" to nombre,
        "archivoId" to archivoId,
        "mime" to mime,
        "tamanoBytes" to tamanoBytes,
        "subidoPor" to subidoPor,
        "fecha" to fecha,
    )

    private fun archivoMapa(leido: ArchivoUtils.ArchivoLeido, subidoPor: String, fecha: Long): Map<String, Any?> =
        mapOf(
            "base64" to ArchivoUtils.aBase64(leido.bytes),
            "mime" to leido.mime,
            "nombre" to leido.nombre,
            "tamanoBytes" to leido.bytes.size.toLong(),
            "subidoPor" to subidoPor,
            "fecha" to fecha,
        )
}
