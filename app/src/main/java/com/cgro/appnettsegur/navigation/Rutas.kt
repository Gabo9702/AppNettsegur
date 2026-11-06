package com.cgro.appnettsegur.navigation

import com.cgro.appnettsegur.config.Rol

object Rutas {
    // Sesión
    const val LOGIN = "login"

    // Inicio por rol (MODULOS.md, sección 6.2)
    const val INICIO = "inicio" // InicioTecnico: la pantalla Inicio del taller
    const val INICIO_CLIENTE = "inicioCliente"
    const val INICIO_INGENIERO = "inicioIngeniero"
    const val INICIO_RH = "inicioRh"
    const val INICIO_ANALISTA = "inicioAnalista"

    // Flujo del técnico (taller)
    const val CAPTURA = "captura?idRegistro={idRegistro}"
    const val LISTADO = "listado"
    const val DETALLE = "detalle/{idRegistro}"

    // Pantallas compartidas de los roles
    const val PROYECTO_DETALLE = "proyecto/{proyectoId}"
    const val EMPLEADO_DETALLE = "empleado/{empleadoId}"
    const val BANDEJA = "bandeja"
    const val NOVEDAD_REMOTA = "novedadRemota/{idRegistro}"

    const val ARG_ID = "idRegistro"
    const val ARG_PROYECTO = "proyectoId"
    const val ARG_EMPLEADO = "empleadoId"

    fun nuevaCaptura() = "captura"
    fun editarCaptura(idRegistro: String) = "captura?idRegistro=$idRegistro"
    fun detalle(idRegistro: String) = "detalle/$idRegistro"
    fun proyectoDetalle(proyectoId: String) = "proyecto/$proyectoId"
    fun empleadoDetalle(empleadoId: String) = "empleado/$empleadoId"
    fun novedadRemota(idRegistro: String) = "novedadRemota/$idRegistro"

    /** Pantalla de inicio que corresponde a cada rol (sección 3). */
    fun inicioDeRol(rol: Rol): String = when (rol) {
        Rol.TECNICO -> INICIO
        Rol.CLIENTE -> INICIO_CLIENTE
        Rol.INGENIERO -> INICIO_INGENIERO
        Rol.RH -> INICIO_RH
        Rol.ANALISTA -> INICIO_ANALISTA
    }
}
