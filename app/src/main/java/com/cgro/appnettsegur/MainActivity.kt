package com.cgro.appnettsegur

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cgro.appnettsegur.navigation.NavGraph
import com.cgro.appnettsegur.ui.theme.AppNettsegurTheme

class MainActivity : ComponentActivity() {

    // Ruta pendiente cuando se abre la app tocando una notificación del sistema (M3, fase 2).
    private var rutaNotificacion by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rutaNotificacion = intent?.getStringExtra(EXTRA_RUTA)
        enableEdgeToEdge()
        setContent {
            AppNettsegurTheme {
                NavGraph(
                    rutaPendiente = rutaNotificacion,
                    onRutaConsumida = { rutaNotificacion = null },
                )
            }
        }
    }

    // La Activity es singleTop: al tocar la notificación con la app ya abierta llega aquí.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        rutaNotificacion = intent.getStringExtra(EXTRA_RUTA)
    }

    companion object {
        const val EXTRA_RUTA = "nav_ruta"
    }
}
