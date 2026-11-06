package com.cgro.appnettsegur.notificaciones

import com.cgro.appnettsegur.data.model.Notificacion
import com.cgro.appnettsegur.data.model.TipoNotificacion
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas de la lógica de "ya mostradas" y del conteo de no leídas (M3, fase 5).
 * Es lógica pura, así que no necesita Firebase.
 */
class RegistroMostradasTest {

    private fun notif(id: String, leida: Boolean) = Notificacion(
        notificacionId = id,
        tipo = TipoNotificacion.DOCUMENTO_NUEVO,
        titulo = "t", mensaje = "m", proyectoId = "PRY", refId = "REF", fecha = 1L, leida = leida,
    )

    @Test
    fun `nuevas excluye leidas y ya mostradas`() {
        val lista = listOf(
            notif("a", leida = false), // nueva
            notif("b", leida = true),  // leída -> no
            notif("c", leida = false), // ya mostrada -> no
            notif("d", leida = false), // nueva
        )
        val nuevas = RegistroMostradas.nuevas(lista, yaMostradas = setOf("c"))
        assertEquals(listOf("a", "d"), nuevas.map { it.notificacionId })
    }

    @Test
    fun `si todas fueron mostradas o leidas no hay nuevas`() {
        val lista = listOf(notif("a", leida = true), notif("b", leida = false))
        assertEquals(emptyList<String>(), RegistroMostradas.nuevas(lista, yaMostradas = setOf("b")).map { it.notificacionId })
    }

    @Test
    fun `conteoNoLeidas cuenta solo las no leidas`() {
        val lista = listOf(
            notif("a", leida = false),
            notif("b", leida = true),
            notif("c", leida = false),
        )
        assertEquals(2, RegistroMostradas.conteoNoLeidas(lista))
    }

    @Test
    fun `vigentes devuelve todos los ids presentes`() {
        val lista = listOf(notif("a", leida = false), notif("b", leida = true))
        assertEquals(setOf("a", "b"), RegistroMostradas.vigentes(lista))
    }
}
