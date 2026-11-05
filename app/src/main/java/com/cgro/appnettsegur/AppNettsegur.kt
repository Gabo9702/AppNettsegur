package com.cgro.appnettsegur

import android.app.Application
import android.content.Context
import com.cgro.appnettsegur.data.local.AppDatabase
import com.cgro.appnettsegur.data.prefs.SesionStore
import com.cgro.appnettsegur.data.remote.FirebaseNovedadesDataSource
import com.cgro.appnettsegur.data.repository.ArchivosRepository
import com.cgro.appnettsegur.data.repository.ArchivosRepositoryRelleno
import com.cgro.appnettsegur.data.repository.CartasRepository
import com.cgro.appnettsegur.data.repository.CartasRepositoryRelleno
import com.cgro.appnettsegur.data.repository.DocumentosRepository
import com.cgro.appnettsegur.data.repository.DocumentosRepositoryRelleno
import com.cgro.appnettsegur.data.repository.EmpleadosRepository
import com.cgro.appnettsegur.data.repository.EmpleadosRepositoryRelleno
import com.cgro.appnettsegur.data.repository.NovedadRepository
import com.cgro.appnettsegur.data.repository.ProyectosRepository
import com.cgro.appnettsegur.data.repository.ProyectosRepositoryRelleno
import com.cgro.appnettsegur.data.repository.SesionRepositoryImpl
import com.cgro.appnettsegur.data.repository.SyncRepository
import com.cgro.appnettsegur.util.ConectividadMonitor
import com.cgro.appnettsegur.util.ImagenUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Contenedor manual de dependencias (sin Hilt). Se crea una sola vez en [AppNettsegur]
 * y los ViewModels obtienen de aquí los repositorios.
 */
class AppContainer(context: Context) {

    /** Alcance que vive mientras vive el proceso (tareas de arranque). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database = AppDatabase.construir(context)
    val conectividad = ConectividadMonitor(context)

    // Sesión (M1): Firebase Auth + caché en DataStore. El nombre del técnico sale de aquí.
    val sesionStore = SesionStore(context)
    val sesionRepository = SesionRepositoryImpl(context, sesionStore)

    // Flujo del técnico (taller).
    val novedadRepository = NovedadRepository(context, database)
    val syncRepository = SyncRepository(database, FirebaseNovedadesDataSource(context), conectividad)

    // Repositorios de los roles (fase A: de relleno; la fase C los hace reales).
    val proyectosRepository: ProyectosRepository = ProyectosRepositoryRelleno()
    val documentosRepository: DocumentosRepository = DocumentosRepositoryRelleno()
    val archivosRepository: ArchivosRepository = ArchivosRepositoryRelleno()
    val empleadosRepository: EmpleadosRepository = EmpleadosRepositoryRelleno()
    val cartasRepository: CartasRepository = CartasRepositoryRelleno()
}

class AppNettsegur : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appScope.launch {
            container.novedadRepository.cargarCatalogoSiVacio()
            // Fotos temporales de formularios que no se guardaron (p. ej. si la app se cerró a la fuerza).
            ImagenUtils.limpiarTemporales(this@AppNettsegur)
        }
    }
}
