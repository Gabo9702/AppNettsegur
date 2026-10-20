package com.cgro.appnettsegur.ui.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cgro.appnettsegur.AppNettsegur
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.data.prefs.PerfilTecnicoStore
import com.cgro.appnettsegur.ui.componentes.CampoTexto
import com.cgro.appnettsegur.ui.componentes.LocalMensajero
import com.cgro.appnettsegur.ui.componentes.TarjetaSeccion
import kotlinx.coroutines.launch

class PerfilViewModel(private val perfil: PerfilTecnicoStore) : ViewModel() {

    suspend fun nombreActual(): String = perfil.obtenerNombre()

    fun guardar(nombre: String, alTerminar: () -> Unit) {
        viewModelScope.launch {
            perfil.guardarNombre(nombre)
            alTerminar()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppNettsegur
                PerfilViewModel(app.container.perfilStore)
            }
        }
    }
}

/** Primer inicio: pide el nombre del técnico. Después se puede editar desde Inicio. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    primerInicio: Boolean,
    onListo: () -> Unit,
    viewModel: PerfilViewModel = viewModel(factory = PerfilViewModel.Factory),
) {
    val mensajero = LocalMensajero.current
    var nombre by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (nombre.isEmpty()) nombre = viewModel.nombreActual()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (primerInicio) "Bienvenido" else "Perfil del técnico") },
                navigationIcon = {
                    if (!primerInicio) {
                        IconButton(onClick = onListo) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TarjetaSeccion("Perfil del técnico", icono = Icons.Outlined.Engineering) {
                if (primerInicio) {
                    Text(
                        "Escriba su nombre. Aparecerá en cada constancia de servicio que registre.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                CampoTexto(
                    valor = nombre,
                    onCambio = {
                        nombre = it
                        error = null
                    },
                    etiqueta = "Nombre completo *",
                    error = error,
                    teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
            }
            Button(
                onClick = {
                    if (nombre.isBlank()) {
                        error = Mensajes.CAMPO_OBLIGATORIO
                    } else {
                        viewModel.guardar(nombre) {
                            mensajero(Mensajes.PERFIL_GUARDADO)
                            onListo()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(if (primerInicio) "Continuar" else "Guardar") }
        }
    }
}
