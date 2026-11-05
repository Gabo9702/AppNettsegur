package com.cgro.appnettsegur.config

/**
 * Roles de la aplicación (MODULOS.md, sección 3).
 *
 * El rol NO se maneja con custom claims (eso necesitaría un servidor y el plan Blaze):
 * se guarda en usuarios/{uid}/rol y las reglas de seguridad lo leen desde ahí.
 */
enum class Rol {
    TECNICO,
    CLIENTE,
    INGENIERO,
    RH,
    ANALISTA;

    companion object {
        /** Convierte el texto guardado en Firebase a un rol, o null si no es válido. */
        fun desde(valor: String?): Rol? =
            valor?.trim()?.uppercase()?.let { texto -> entries.firstOrNull { it.name == texto } }
    }
}
