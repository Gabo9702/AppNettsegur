package com.cgro.appnettsegur.navigation

object Rutas {
    const val PERFIL = "perfil?primerInicio={primerInicio}"
    const val INICIO = "inicio"
    const val CAPTURA = "captura?idRegistro={idRegistro}"
    const val LISTADO = "listado"
    const val DETALLE = "detalle/{idRegistro}"

    const val ARG_ID = "idRegistro"
    const val ARG_PRIMER_INICIO = "primerInicio"

    fun perfil(primerInicio: Boolean) = "perfil?primerInicio=$primerInicio"
    fun nuevaCaptura() = "captura"
    fun editarCaptura(idRegistro: String) = "captura?idRegistro=$idRegistro"
    fun detalle(idRegistro: String) = "detalle/$idRegistro"
}
