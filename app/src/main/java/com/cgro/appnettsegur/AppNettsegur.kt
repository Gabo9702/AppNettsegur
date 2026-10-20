package com.cgro.appnettsegur

import android.app.Application
import android.content.Context
import com.cgro.appnettsegur.data.local.AppDatabase
import com.cgro.appnettsegur.data.prefs.PerfilTecnicoStore
import com.cgro.appnettsegur.data.remote.FirebaseNovedadesDataSource
import com.cgro.appnettsegur.data.repository.NovedadRepository
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
    val perfilStore = PerfilTecnicoStore(context)
    val conectividad = ConectividadMonitor(context)
    val novedadRepository = NovedadRepository(context, database)
    val syncRepository = SyncRepository(database, FirebaseNovedadesDataSource(context), conectividad)
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
