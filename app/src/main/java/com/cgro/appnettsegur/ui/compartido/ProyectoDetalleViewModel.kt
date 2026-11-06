package com.cgro.appnettsegur.ui.compartido

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cgro.appnettsegur.AppNettsegur
import android.net.Uri
import com.cgro.appnettsegur.config.Rol
import com.cgro.appnettsegur.data.model.Carta
import com.cgro.appnettsegur.data.model.Documento
import com.cgro.appnettsegur.data.model.Empleado
import com.cgro.appnettsegur.data.model.NovedadRemota
import com.cgro.appnettsegur.data.model.PersonalAsignado
import com.cgro.appnettsegur.data.model.Proyecto
import com.cgro.appnettsegur.data.model.TipoDocumento
import com.cgro.appnettsegur.data.repository.ArchivosRepository
import com.cgro.appnettsegur.data.repository.CartasRepository
import com.cgro.appnettsegur.data.repository.DocumentosRepository
import com.cgro.appnettsegur.data.repository.EmpleadosRepository
import com.cgro.appnettsegur.data.repository.ProyectosRepository
import com.cgro.appnettsegur.data.repository.SesionRepository
import com.cgro.appnettsegur.navigation.Rutas
import com.cgro.appnettsegur.util.ConectividadMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.io.File

data class ProyectoDetalleUi(
    val cargando: Boolean = true,
    val proyecto: Proyecto? = null,
    val documentos: List<Documento> = emptyList(),
    val personal: List<PersonalAsignado> = emptyList(),
    val novedades: List<NovedadRemota> = emptyList(),
    val cartas: List<Carta> = emptyList(),
)

/**
 * Detalle de un proyecto, compartido por cliente e ingeniero (M2, fases 1 y 2).
 *
 * El rol de la sesión decide qué acciones se muestran: el cliente solo lee; el ingeniero puede
 * cargar documentos, marcar hitos y asignar técnicos (la UI de esas acciones se agrega en la fase 2).
 * Todo es en línea (MODULOS.md, sección 3); "Reintentar" vuelve a suscribirse a RTDB.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProyectoDetalleViewModel(
    private val proyectos: ProyectosRepository,
    private val documentos: DocumentosRepository,
    private val archivos: ArchivosRepository,
    private val cartas: CartasRepository,
    private val empleados: EmpleadosRepository,
    sesion: SesionRepository,
    conectividad: ConectividadMonitor,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val proyectoId: String = checkNotNull(savedStateHandle[Rutas.ARG_PROYECTO])

    val hayInternet: StateFlow<Boolean> = conectividad.hayInternet

    /** Rol de la sesión: el cliente solo lee; el ingeniero ve además las acciones de edición. */
    val rol: StateFlow<Rol?> = sesion.usuario
        .map { it?.rol }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Empleados para la pestaña Técnicos (solo ingeniero). Se leen únicamente si el rol es INGENIERO:
     * así el cliente no toca /empleados, que sus reglas no le permiten leer.
     */
    val empleadosDisponibles: StateFlow<List<Empleado>> = rol
        .flatMapLatest { r -> if (r == Rol.INGENIERO) empleados.empleados() else flowOf(emptyList()) }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val reintentos = MutableStateFlow(0)

    val estado: StateFlow<ProyectoDetalleUi> = reintentos
        .flatMapLatest {
            combine(
                proyectos.proyecto(proyectoId),
                documentos.documentos(proyectoId),
                proyectos.personalDelProyecto(proyectoId),
                proyectos.novedadesDelProyecto(proyectoId),
                cartas.cartasDelProyecto(proyectoId),
            ) { p, docs, personal, novs, cartasLista ->
                ProyectoDetalleUi(
                    cargando = false,
                    proyecto = p,
                    documentos = docs,
                    personal = personal,
                    novedades = novs,
                    cartas = cartasLista,
                )
            }.onStart { emit(ProyectoDetalleUi(cargando = true)) }
        }
        .catch { emit(ProyectoDetalleUi(cargando = false)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProyectoDetalleUi())

    fun reintentar() {
        reintentos.value++
    }

    /** Descarga un archivo (Base64 → cacheDir) para abrirlo con un visor. Lo usa cliente e ingeniero. */
    suspend fun descargar(archivoId: String): Result<File> = archivos.descargarAArchivoLocal(archivoId)

    // ---- Acciones del ingeniero (CU-05, HU-11, HU-12) ----

    /** Sube un documento: valida tipo/tamaño y notifica al cliente (todo en una escritura multi-ruta). */
    suspend fun subirDocumento(tipo: TipoDocumento, uri: Uri): Result<Documento> =
        documentos.subir(proyectoId, tipo, uri)

    /** Marca o desmarca un hito; el avance se recalcula en el repositorio. */
    suspend fun marcarHito(hitoId: String, completado: Boolean): Result<Unit> =
        proyectos.marcarHito(proyectoId, hitoId, completado)

    /** Asigna o quita un técnico (solo habilitados, lo valida la UI) y actualiza la vista de personal. */
    suspend fun asignarTecnico(empleado: Empleado, asignar: Boolean): Result<Unit> =
        proyectos.asignarTecnico(proyectoId, empleado, asignar)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppNettsegur
                ProyectoDetalleViewModel(
                    app.container.proyectosRepository,
                    app.container.documentosRepository,
                    app.container.archivosRepository,
                    app.container.cartasRepository,
                    app.container.empleadosRepository,
                    app.container.sesionRepository,
                    app.container.conectividad,
                    createSavedStateHandle(),
                )
            }
        }
    }
}
