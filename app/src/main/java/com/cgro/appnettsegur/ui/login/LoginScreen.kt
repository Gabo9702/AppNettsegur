package com.cgro.appnettsegur.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cgro.appnettsegur.config.Mensajes
import com.cgro.appnettsegur.config.Rol
import com.cgro.appnettsegur.ui.componentes.TarjetaSeccion

/**
 * Inicio de sesión (MODULOS.md, sección 5.1). La primera vez requiere Internet; después la app
 * entra sin conexión con la sesión en caché (lo resuelve SesionRepository).
 */
@Composable
fun LoginScreen(
    onLogueado: (Rol) -> Unit,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var verClave by remember { mutableStateOf(false) }

    // El VM avisa el rol cuando el inicio de sesión fue exitoso.
    LaunchedEffect(Unit) { viewModel.logueado.collect { onLogueado(it) } }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text("Nettsegur", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(Mensajes.LOGIN_SUBTITULO, style = MaterialTheme.typography.bodyMedium)

            TarjetaSeccion(Mensajes.LOGIN_TITULO) {
                OutlinedTextField(
                    value = estado.correo,
                    onValueChange = viewModel::onCorreo,
                    label = { Text(Mensajes.LOGIN_CORREO) },
                    singleLine = true,
                    enabled = !estado.cargando,
                    isError = estado.error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = estado.clave,
                    onValueChange = viewModel::onClave,
                    label = { Text(Mensajes.LOGIN_CLAVE) },
                    singleLine = true,
                    enabled = !estado.cargando,
                    isError = estado.error != null,
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { verClave = !verClave }) {
                            Icon(
                                if (verClave) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (verClave) "Ocultar contraseña" else "Mostrar contraseña",
                            )
                        }
                    },
                    visualTransformation = if (verClave) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    supportingText = estado.error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(
                onClick = viewModel::entrar,
                enabled = !estado.cargando,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (estado.cargando) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(Mensajes.LOGIN_ENTRAR)
                }
            }
        }
    }
}
