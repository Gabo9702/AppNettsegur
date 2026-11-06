package com.cgro.appnettsegur.ui.compartido

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cgro.appnettsegur.config.Catalogos
import com.cgro.appnettsegur.data.model.NovedadRemota
import com.cgro.appnettsegur.ui.componentes.DatoLectura
import com.cgro.appnettsegur.ui.componentes.TarjetaSeccion
import com.cgro.appnettsegur.util.Formatos

/**
 * Detalle de una novedad sincronizada (M2, fase 1): todos los datos, las fotos (miniatura Base64)
 * y las firmas del técnico y del representante. Solo lectura.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovedadRemotaScreen(
    onVolver: () -> Unit,
    viewModel: NovedadRemotaViewModel = viewModel(factory = NovedadRemotaViewModel.Factory),
) {
    val s by viewModel.estado.collectAsStateWithLifecycle()
    val hayInternet by viewModel.hayInternet.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Novedad") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = MensajesRoles.VOLVER)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when {
                s.cargando && s.novedad == null && !hayInternet ->
                    SeccionNecesitaInternet(onReintentar = viewModel::reintentar)

                s.cargando && s.novedad == null -> PantallaCargando()

                s.novedad == null -> PantallaVacia(MensajesRoles.NOVEDAD_NO_DISPONIBLE)

                else -> Contenido(s.novedad!!)
            }
        }
    }
}

@Composable
private fun Contenido(n: NovedadRemota) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TarjetaSeccion(titulo = "Servicio") {
                DatoLectura("Cliente", n.nombreCliente)
                DatoLectura("Técnico", n.nombreTecnico)
                DatoLectura("Fecha del servicio", Formatos.fecha(n.fechaServicio))
                DatoLectura("Horario", "${n.horaInicio} – ${n.horaFin}")
                DatoLectura("Tipo de servicio", n.tipoServicio)
                DatoLectura("Tipo de actividad", n.tipoActividad)
                DatoLectura("Serial del equipo", n.serialEquipo)
            }
        }
        item {
            TarjetaSeccion(titulo = "Atención") {
                DatoLectura("Servicio asignado", n.descServAsignado)
                DatoLectura("Estado de la atención", Catalogos.estadosAtencion[n.estadoAtencion] ?: n.estadoAtencion)
                DatoLectura("Servicio realizado", n.descServRealizado)
                if (n.estadoAtencion == Catalogos.NO_RESUELTA) {
                    DatoLectura("Motivo (no resuelta)", n.motivoNoResuelta)
                }
                DatoLectura("Observaciones", n.observacionesRecomendaciones)
            }
        }
        item {
            TarjetaSeccion(titulo = "Representante del cliente") {
                DatoLectura("Nombre", n.nombreRepresentanteCliente)
                DatoLectura("Cédula", n.cedulaRepresentanteCliente)
                DatoLectura("Rol", n.rolRepresentanteCliente)
            }
        }
        if (n.evidencias.isNotEmpty()) {
            item {
                TarjetaSeccion(titulo = "Evidencia fotográfica") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(n.evidencias) { ev ->
                            ImagenBase64(
                                base64 = ev.base64,
                                contentDescription = "Foto de evidencia",
                                modifier = Modifier
                                    .width(160.dp)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                        }
                    }
                }
            }
        }
        item {
            TarjetaSeccion(titulo = "Firmas") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FirmaColumna("Técnico", n.firmaTecnicoBase64, Modifier.weight(1f))
                    FirmaColumna("Representante", n.firmaRepresentanteBase64, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FirmaColumna(etiqueta: String, base64: String?, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ImagenBase64(
            base64 = base64,
            contentDescription = "Firma de $etiqueta",
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
    }
}
