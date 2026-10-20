package com.cgro.appnettsegur.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Observa la conexión con ConnectivityManager. Solo se considera "con Internet" una red con
 * NET_CAPABILITY_INTERNET **y** NET_CAPABILITY_VALIDATED (Android comprobó que sí sale a Internet;
 * un Wi-Fi sin salida o con portal cautivo no cuenta).
 *
 * Solo sirve para el banner y para decidir si vale la pena intentar SINCRONIZAR.
 * Recuperar la conexión NUNCA dispara un envío automático.
 */
class ConectividadMonitor(context: Context) {

    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val redesValidadas = mutableSetOf<Network>()

    private val _hayInternet = MutableStateFlow(hayInternetValidado())
    val hayInternet: StateFlow<Boolean> = _hayInternet.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capacidades: NetworkCapabilities) {
            synchronized(redesValidadas) {
                if (capacidades.esValidada()) redesValidadas += network else redesValidadas -= network
                _hayInternet.value = redesValidadas.isNotEmpty()
            }
        }

        override fun onLost(network: Network) {
            synchronized(redesValidadas) {
                redesValidadas -= network
                _hayInternet.value = redesValidadas.isNotEmpty()
            }
        }
    }

    init {
        val solicitud = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(solicitud, callback)
    }

    /** Consulta inmediata: ¿la red activa tiene Internet validado en este momento? */
    fun hayInternetValidado(): Boolean {
        val red = cm.activeNetwork ?: return false
        return cm.getNetworkCapabilities(red)?.esValidada() == true
    }

    private fun NetworkCapabilities.esValidada() =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
