package com.cgro.appnettsegur.ui.detalle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.cgro.appnettsegur.config.Catalogos
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.data.local.EstadoSync
import com.cgro.appnettsegur.ui.componentes.DatoLectura
import com.cgro.appnettsegur.ui.componentes.DialogoConfirmacion
import com.cgro.appnettsegur.ui.componentes.EtiquetaEstado
import com.cgro.appnettsegur.ui.componentes.LocalMensajero
import com.cgro.appnettsegur.ui.componentes.TarjetaSeccion
import com.cgro.appnettsegur.ui.listado.EstadoVacio
import com.cgro.appnettsegur.util.Formatos
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleScreen(
    sincronizando: Boolean,
    onVolver: () -> Unit,
    onEditar: (String) -> Unit,
    onReintentar: () -> Unit,
    viewModel: DetalleViewModel = viewModel(factory = DetalleViewModel.Factory),
) {
    val s by viewModel.estado.collectAsStateWithLifecycle()
    val mensajero = LocalMensajero.current
    var confirmarEliminar by remember { mutableStateOf(false) }
    var fotoAmpliada by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                EventoDetalle.Eliminado -> {
                    mensajero(Mensajes.ELIMINADO)
                    onVolver()
                }
                is EventoDetalle.Mensaje -> mensajero(evento.texto)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del registro") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Editar solo aparece si el registro NO está sincronizado.
                    if (s.puedeEditar) {
                        IconButton(onClick = { s.novedad?.let { onEditar(it.idRegistro) } }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Editar registro")
                        }
                    }
                },
            )
        },
    ) { padding ->
        val n = s.novedad
        when {
            s.cargando -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            n == null -> Box(Modifier.padding(padding)) { EstadoVacio("Este registro ya no existe en el dispositivo.") }
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ---------- Estado ----------
                TarjetaSeccion(titulo = null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(n.nombreCliente, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        EtiquetaEstado(n.estadoSync)
                    }
                    DatoLectura("ID del registro", n.idRegistro)
                    DatoLectura(
                        "Última sincronización",
                        n.fechaSincronizacion?.let { Formatos.fechaHora(it) } ?: "Aún no se ha enviado",
                    )
                    if (n.estadoSync == EstadoSync.ERROR) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.small)
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.size(8.dp))
                            Text(
                                "Último error: ${n.mensajeError ?: Mensajes.SYNC_ERROR_DESCONOCIDO}",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                    if (n.estadoSync == EstadoSync.SINCRONIZADO) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                " Registro enviado: solo lectura.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                TarjetaSeccion("Proyecto y servicio", icono = Icons.Outlined.WorkOutline) {
                    DatoLectura("Proyecto", s.nombreProyecto ?: n.proyectoId)
                    DatoLectura("Cliente", n.nombreCliente)
                    DatoLectura("Técnico", n.nombreTecnico)
                    DatoLectura("Fecha del servicio", Formatos.fecha(n.fechaServicio))
                    DatoLectura("Tipo de servicio", n.tipoServicio)
                    DatoLectura("Tipo de actividad", n.tipoActividad)
                    DatoLectura("Serial del equipo", n.serialEquipo)
                }

                TarjetaSeccion("Atención", icono = Icons.Outlined.Build) {
                    Row {
                        DatoLectura("Hora de inicio", n.horaInicio, Modifier.weight(1f))
                        DatoLectura("Hora de fin", n.horaFin, Modifier.weight(1f))
                    }
                    DatoLectura("Servicio asignado", n.descServAsignado)
                    DatoLectura("Estado de la atención", Catalogos.estadosAtencion[n.estadoAtencion] ?: n.estadoAtencion)
                    DatoLectura("Servicio realizado", n.descServRealizado)
                    if (n.estadoAtencion == Catalogos.NO_RESUELTA) {
                        DatoLectura("Motivo por el que no quedó resuelta", n.motivoNoResuelta)
                    }
                }

                TarjetaSeccion("Observaciones", icono = Icons.AutoMirrored.Outlined.Notes) {
                    DatoLectura("Observaciones y recomendaciones", n.observacionesRecomendaciones)
                }

                TarjetaSeccion("Evidencia fotográfica", icono = Icons.Outlined.PhotoCamera) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        s.evidencias.forEachIndexed { i, evidencia ->
                            AsyncImage(
                                model = File(evidencia.rutaArchivo),
                                contentDescription = "Foto de evidencia ${i + 1}. Toque para ampliar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable { fotoAmpliada = evidencia.rutaArchivo },
                            )
                        }
                    }
                    Text(
                        "Toque una foto para ampliarla.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                TarjetaSeccion("Representante del cliente", icono = Icons.Outlined.Badge) {
                    DatoLectura("Nombre", n.nombreRepresentanteCliente)
                    DatoLectura("Cédula", n.cedulaRepresentanteCliente)
                    DatoLectura("Rol", n.rolRepresentanteCliente)
                }

                TarjetaSeccion("Firmas", icono = Icons.Outlined.Draw) {
                    ImagenFirma("Firma del técnico", n.rutaFirmaTecnico)
                    ImagenFirma("Firma del representante", n.rutaFirmaRepresentante)
                }

                // ---------- Acciones ----------
                if (s.puedeReintentar) {
                    Button(
                        onClick = onReintentar,
                        enabled = !sincronizando,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        if (sincronizando) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Refresh, contentDescription = null)
                        }
                        Text("  Reintentar (sincroniza todos los pendientes)")
                    }
                }
                if (s.puedeEditar) {
                    OutlinedButton(onClick = { onEditar(n.idRegistro) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Edit, contentDescription = null)
                        Text("  Editar")
                    }
                }
                if (s.puedeEliminar) {
                    OutlinedButton(
                        onClick = { confirmarEliminar = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                        Text("  Eliminar")
                    }
                }
                if (n.estadoSync == EstadoSync.PENDIENTE) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            " Para enviarlo, pulse SINCRONIZAR en Inicio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (confirmarEliminar) {
        DialogoConfirmacion(
            titulo = Mensajes.ELIMINAR_TITULO,
            texto = Mensajes.ELIMINAR_TEXTO,
            textoConfirmar = "Eliminar",
            destructivo = true,
            onConfirmar = {
                confirmarEliminar = false
                viewModel.eliminar()
            },
            onCancelar = { confirmarEliminar = false },
        )
    }

    fotoAmpliada?.let { ruta ->
        Dialog(onDismissRequest = { fotoAmpliada = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                AsyncImage(
                    model = File(ruta),
                    contentDescription = "Foto de evidencia ampliada",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                IconButton(onClick = { fotoAmpliada = null }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "Cerrar foto", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ImagenFirma(titulo: String, ruta: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AsyncImage(
            model = File(ruta),
            contentDescription = titulo,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color.White, MaterialTheme.shapes.small),
        )
    }
}
