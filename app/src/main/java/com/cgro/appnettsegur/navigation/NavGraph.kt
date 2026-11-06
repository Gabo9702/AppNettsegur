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
import com.cgro.appnettsegur.config.Rol
import com.cgro.appnettsegur.ui.SyncViewModel
import com.cgro.appnettsegur.ui.captura.CapturaScreen
import com.cgro.appnettsegur.ui.componentes.LocalMensajero
import com.cgro.appnettsegur.ui.detalle.DetalleScreen
import com.cgro.appnettsegur.ui.cliente.InicioClienteScreen
import com.cgro.appnettsegur.ui.ingeniero.InicioIngenieroScreen
import com.cgro.appnettsegur.ui.compartido.NovedadRemotaScreen
import com.cgro.appnettsegur.ui.compartido.ProyectoDetalleScreen
import com.cgro.appnettsegur.ui.inicio.InicioScreen
import com.cgro.appnettsegur.ui.listado.ListadoScreen
import com.cgro.appnettsegur.ui.login.LoginScreen
import com.cgro.appnettsegur.ui.notificaciones.BandejaScreen
import kotlinx.coroutines.launch

@Composable
fun NavGraph(
    rutaPendiente: String? = null,
    onRutaConsumida: () -> Unit = {},
) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    val mensajero: (String) -> Unit = { texto ->
        alcance.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(texto)
        }
    }

    val container = (LocalContext.current.applicationContext as AppNettsegur).container
    val sesion = container.sesionRepository
    val usuario by sesion.usuario.collectAsStateWithLifecycle()

    // Creado con el ViewModelStore de la Activity: compartido por todas las pantallas.
    val sync: SyncViewModel = viewModel(factory = SyncViewModel.Factory)
    val progreso by sync.progreso.collectAsStateWithLifecycle()
    val hayInternet by sync.hayInternet.collectAsStateWithLifecycle()
    val falloSimuladoEn by sync.falloSimuladoEn.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { sync.mensajes.collect { mensajero(it) } }

    // M3: con la app en primer plano, una notificación nueva solo muestra un snackbar.
    LaunchedEffect(Unit) { container.observadorNotificaciones.snackbars.collect { mensajero(it) } }

    // Pantalla inicial: inicio del rol si hay sesión en caché (sin Internet); si no, Login (5.1).
    var inicio by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        if (inicio == null) {
            val u = sesion.restaurarSesion()
            inicio = if (u != null) Rutas.inicioDeRol(u.rol) else Rutas.LOGIN
            // Si el técnico ya tenía sesión, intenta refrescar sus proyectos asignados (sección 5.2).
            if (u?.rol == Rol.TECNICO) {
                alcance.launch { container.novedadRepository.actualizarProyectosDelTecnico(u.uid) }
            }
        }
    }
    val destinoInicial = inicio ?: return

    // M3: si la app se abrió tocando una notificación, navega a su destino (solo con sesión activa).
    LaunchedEffect(rutaPendiente, usuario?.uid) {
        val ruta = rutaPendiente ?: return@LaunchedEffect
        if (usuario != null) {
            navController.navigate(ruta) { launchSingleTop = true }
            onRutaConsumida()
        }
    }

    // Tras iniciar sesión: ir al inicio del rol, sin dejar el login en la pila.
    val irAInicioDeRol: (Rol) -> Unit = { rol ->
        // Al iniciar sesión el técnico descarga sus proyectos asignados (sección 5.2).
        if (rol == Rol.TECNICO) {
            alcance.launch { sesion.usuario.value?.uid?.let { container.novedadRepository.actualizarProyectosDelTecnico(it) } }
        }
        navController.navigate(Rutas.inicioDeRol(rol)) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }
    // Cerrar sesión: limpia la sesión y vuelve al login, sin dejar nada en la pila.
    val cerrarSesion: () -> Unit = {
        alcance.launch {
            sesion.cerrarSesion()
            navController.navigate(Rutas.LOGIN) { popUpTo(navController.graph.id) { inclusive = true } }
        }
    }

    CompositionLocalProvider(LocalMensajero provides mensajero) {
        Box(Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = destinoInicial) {

                composable(Rutas.LOGIN) {
                    LoginScreen(onLogueado = irAInicioDeRol)
                }

                // ---------- Técnico (flujo del taller) ----------
                composable(Rutas.INICIO) {
                    InicioScreen(
                        hayInternet = hayInternet,
                        progreso = progreso,
                        falloSimuladoEn = falloSimuladoEn,
                        onSincronizar = sync::sincronizar,
                        onSimularFallo = sync::simularFalloEn,
                        onNuevo = { navController.navigate(Rutas.nuevaCaptura()) },
                        onVerRegistros = { navController.navigate(Rutas.LISTADO) },
                        onCerrarSesion = cerrarSesion,
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

                // ---------- Inicios de los demás roles ----------
                // M2: cliente real. Ingeniero, RH y analista siguen en relleno hasta sus fases.
                composable(Rutas.INICIO_CLIENTE) {
                    InicioClienteScreen(
                        nombreUsuario = usuario?.nombre.orEmpty(),
                        onAbrirProyecto = { navController.navigate(Rutas.proyectoDetalle(it)) },
                        onBandeja = { navController.navigate(Rutas.BANDEJA) },
                        onCerrarSesion = cerrarSesion,
                    )
                }
                composable(Rutas.INICIO_INGENIERO) {
                    InicioIngenieroScreen(
                        nombreUsuario = usuario?.nombre.orEmpty(),
                        onAbrirProyecto = { navController.navigate(Rutas.proyectoDetalle(it)) },
                        onBandeja = { navController.navigate(Rutas.BANDEJA) },
                        onCerrarSesion = cerrarSesion,
                    )
                }
                composable(Rutas.INICIO_RH) {
                    InicioRolRelleno("HCQ y RH", usuario?.nombre.orEmpty(),
                        onBandeja = { navController.navigate(Rutas.BANDEJA) }, onCerrarSesion = cerrarSesion,
                        mostrarResumenVencimientos = true)
                }
                composable(Rutas.INICIO_ANALISTA) {
                    InicioRolRelleno("Analista", usuario?.nombre.orEmpty(),
                        onBandeja = { navController.navigate(Rutas.BANDEJA) }, onCerrarSesion = cerrarSesion)
                }

                // ---------- Pantallas compartidas ----------
                // M2: detalle de proyecto (cliente e ingeniero). EMPLEADO_DETALLE llega en la fase 3.
                composable(
                    Rutas.PROYECTO_DETALLE,
                    arguments = listOf(navArgument(Rutas.ARG_PROYECTO) { type = NavType.StringType }),
                ) {
                    ProyectoDetalleScreen(
                        onVolver = { navController.popBackStack() },
                        onAbrirNovedad = { navController.navigate(Rutas.novedadRemota(it)) },
                    )
                }
                composable(
                    Rutas.EMPLEADO_DETALLE,
                    arguments = listOf(navArgument(Rutas.ARG_EMPLEADO) { type = NavType.StringType }),
                ) { PantallaRellenoSimple("Empleado", onVolver = { navController.popBackStack() }) }
                // ---------- Bandeja de notificaciones (M3) ----------
                composable(Rutas.BANDEJA) {
                    BandejaScreen(
                        onVolver = { navController.popBackStack() },
                        onAbrirNovedad = { refId -> navController.navigate(Rutas.novedadRemota(refId)) },
                        onAbrirProyecto = { pid -> navController.navigate(Rutas.proyectoDetalle(pid)) },
                    )
                }
                composable(
                    Rutas.NOVEDAD_REMOTA,
                    arguments = listOf(navArgument(Rutas.ARG_ID) { type = NavType.StringType }),
                ) { NovedadRemotaScreen(onVolver = { navController.popBackStack() }) }
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
