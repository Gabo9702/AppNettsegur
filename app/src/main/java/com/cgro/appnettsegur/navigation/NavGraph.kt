package com.cgro.appnettsegur.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cgro.appnettsegur.AppNettsegur
import com.cgro.appnettsegur.ui.SyncViewModel
import com.cgro.appnettsegur.ui.captura.CapturaScreen
import com.cgro.appnettsegur.ui.componentes.LocalMensajero
import com.cgro.appnettsegur.ui.detalle.DetalleScreen
import com.cgro.appnettsegur.ui.inicio.InicioScreen
import com.cgro.appnettsegur.ui.listado.ListadoScreen
import com.cgro.appnettsegur.ui.perfil.PerfilScreen
import kotlinx.coroutines.launch

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    val mensajero: (String) -> Unit = { texto ->
        alcance.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(texto)
        }
    }

    // Creado con el ViewModelStore de la Activity: compartido por todas las pantallas.
    val sync: SyncViewModel = viewModel(factory = SyncViewModel.Factory)
    val progreso by sync.progreso.collectAsStateWithLifecycle()
    val hayInternet by sync.hayInternet.collectAsStateWithLifecycle()
    val falloSimuladoEn by sync.falloSimuladoEn.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { sync.mensajes.collect { mensajero(it) } }

    // Pantalla inicial: Perfil si todavía no hay nombre de técnico; si no, Inicio.
    val perfil = (LocalContext.current.applicationContext as AppNettsegur).container.perfilStore
    var inicio by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        if (inicio == null) inicio = if (perfil.obtenerNombre().isBlank()) Rutas.perfil(primerInicio = true) else Rutas.INICIO
    }
    val destinoInicial = inicio ?: return

    CompositionLocalProvider(LocalMensajero provides mensajero) {
        Box(Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = destinoInicial) {
                composable(
                    Rutas.PERFIL,
                    arguments = listOf(navArgument(Rutas.ARG_PRIMER_INICIO) { type = NavType.BoolType; defaultValue = false }),
                ) { entrada ->
                    val primerInicio = entrada.arguments?.getBoolean(Rutas.ARG_PRIMER_INICIO) ?: false
                    PerfilScreen(
                        primerInicio = primerInicio,
                        onListo = {
                            if (primerInicio) {
                                navController.navigate(Rutas.INICIO) { popUpTo(navController.graph.id) { inclusive = true } }
                            } else {
                                navController.popBackStack()
                            }
                        },
                    )
                }
                composable(Rutas.INICIO) {
                    InicioScreen(
                        hayInternet = hayInternet,
                        progreso = progreso,
                        falloSimuladoEn = falloSimuladoEn,
                        onSincronizar = sync::sincronizar,
                        onSimularFallo = sync::simularFalloEn,
                        onNuevo = { navController.navigate(Rutas.nuevaCaptura()) },
                        onVerRegistros = { navController.navigate(Rutas.LISTADO) },
                        onEditarPerfil = { navController.navigate(Rutas.perfil(primerInicio = false)) },
                    )
                }
                composable(
                    Rutas.CAPTURA,
                    arguments = listOf(navArgument(Rutas.ARG_ID) { type = NavType.StringType; nullable = true }),
                ) {
                    CapturaScreen(
                        onVolver = { navController.popBackStack() },
                        onGuardado = {
                            // Tras guardar se vuelve al listado (sin dejar el formulario en la pila).
                            navController.navigate(Rutas.LISTADO) { popUpTo(Rutas.INICIO) }
                        },
                    )
                }
                composable(Rutas.LISTADO) {
                    ListadoScreen(
                        onVolver = { navController.popBackStack() },
                        onAbrir = { navController.navigate(Rutas.detalle(it)) },
                        onNuevo = { navController.navigate(Rutas.nuevaCaptura()) },
                    )
                }
                composable(Rutas.DETALLE) {
                    DetalleScreen(
                        sincronizando = progreso.sincronizando,
                        onVolver = { navController.popBackStack() },
                        onEditar = { navController.navigate(Rutas.editarCaptura(it)) },
                        onReintentar = sync::sincronizar,
                    )
                }
            }
            SnackbarHost(
                snackbar,
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp),
            )
        }
    }
}
