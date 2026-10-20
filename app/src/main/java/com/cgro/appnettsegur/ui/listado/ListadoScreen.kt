package com.cgro.appnettsegur.ui.listado

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cgro.appnettsegur.config.Limites
import com.cgro.appnettsegur.data.local.EstadoSync
import com.cgro.appnettsegur.data.local.NovedadEntity
import com.cgro.appnettsegur.ui.componentes.EtiquetaEstado
import com.cgro.appnettsegur.util.Formatos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListadoScreen(
    onVolver: () -> Unit,
    onAbrir: (String) -> Unit,
    onNuevo: () -> Unit,
    viewModel: ListadoViewModel = viewModel(factory = ListadoViewModel.Factory),
) {
    val s by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registros locales") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevo,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Nuevo registro") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val opciones = listOf<Pair<EstadoSync?, String>>(
                    null to "Todos",
                    EstadoSync.PENDIENTE to "Pendientes",
                    EstadoSync.SINCRONIZADO to "Sincronizados",
                    EstadoSync.ERROR to "Con error",
                )
                opciones.forEach { (estado, texto) ->
                    FilterChip(
                        selected = s.filtro == estado,
                        onClick = { viewModel.filtrar(estado) },
                        label = { Text(texto) },
                    )
                }
            }

            when {
                s.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                s.registros.isEmpty() -> EstadoVacio(
                    if (s.totalSinFiltro == 0) "Aún no hay registros en este dispositivo.\nPulse «Nuevo registro» para empezar."
                    else "No hay registros con este estado."
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(s.registros, key = { it.idRegistro }) { registro ->
                        TarjetaRegistro(registro, onClick = { onAbrir(registro.idRegistro) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaRegistro(registro: NovedadEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    registro.nombreCliente,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                EtiquetaEstado(registro.estadoSync)
            }
            Text(registro.tipoServicio, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${Formatos.fecha(registro.fechaServicio)} · ID ${Formatos.idCorto(registro.idRegistro, Limites.CARACTERES_ID_CORTO)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun EstadoVacio(texto: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                Icons.Outlined.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Text(
                texto,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
