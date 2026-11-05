package com.cgro.appnettsegur.config

/**
 * Rutas de Firebase Realtime Database (MODULOS.md, sección 4), centralizadas como funciones.
 *
 * Todas devuelven rutas RELATIVAS a la raíz (sin "/" inicial), para poder usarlas tanto con
 * database.getReference(ruta) como en los mapas de updateChildren() multi-ruta de la sección 5.3.
 *
 * Los ID se generan en el dispositivo (UUID), igual que en el taller: nunca se usa push().
 */
object RutasFirebase {

    // ---- usuarios ----
    fun usuario(uid: String) = "usuarios/$uid"
    fun rolUsuario(uid: String) = "usuarios/$uid/rol"

    // ---- proyectos ----
    const val PROYECTOS = "proyectos"
    fun proyecto(proyectoId: String) = "proyectos/$proyectoId"
    fun hitos(proyectoId: String) = "proyectos/$proyectoId/hitos"
    fun hito(proyectoId: String, hitoId: String) = "proyectos/$proyectoId/hitos/$hitoId"
    fun avanceProyecto(proyectoId: String) = "proyectos/$proyectoId/avancePorcentaje"
    fun tecnicosProyecto(proyectoId: String) = "proyectos/$proyectoId/tecnicos"
    fun tecnicoProyecto(proyectoId: String, uidTecnico: String) = "proyectos/$proyectoId/tecnicos/$uidTecnico"
    /** Vista reducida que el cliente sí puede leer (sección 4, nota sobre empleados y el cliente). */
    fun personalProyecto(proyectoId: String) = "proyectos/$proyectoId/personal"
    fun personalProyectoEmpleado(proyectoId: String, empleadoId: String) =
        "proyectos/$proyectoId/personal/$empleadoId"

    // ---- novedades (ya existen en el taller; se agrega uidTecnico) ----
    const val NOVEDADES = "novedades"
    fun novedad(idRegistro: String) = "novedades/$idRegistro"

    // ---- índices ----
    fun novedadesPorProyecto(proyectoId: String) = "indices/novedadesPorProyecto/$proyectoId"
    fun novedadPorProyecto(proyectoId: String, idRegistro: String) =
        "indices/novedadesPorProyecto/$proyectoId/$idRegistro"
    fun proyectosPorTecnico(uidTecnico: String) = "indices/proyectosPorTecnico/$uidTecnico"
    fun proyectoPorTecnico(uidTecnico: String, proyectoId: String) =
        "indices/proyectosPorTecnico/$uidTecnico/$proyectoId"

    // ---- documentos (solo metadatos; el Base64 va aparte en archivos/) ----
    fun documentos(proyectoId: String) = "documentos/$proyectoId"
    fun documento(proyectoId: String, documentoId: String) = "documentos/$proyectoId/$documentoId"

    // ---- archivos (contenido pesado en Base64, separado de los metadatos) ----
    fun archivo(archivoId: String) = "archivos/$archivoId"

    // ---- empleados ----
    const val EMPLEADOS = "empleados"
    fun empleado(empleadoId: String) = "empleados/$empleadoId"
    fun cumplimiento(empleadoId: String, tipo: String) = "empleados/$empleadoId/cumplimiento/$tipo"
    fun carneEmpleado(empleadoId: String) = "empleados/$empleadoId/carne"

    // ---- cartas de presentación ----
    const val CARTAS = "cartas"
    fun carta(cartaId: String) = "cartas/$cartaId"

    // ---- notificaciones ----
    fun notificaciones(uidDestinatario: String) = "notificaciones/$uidDestinatario"
    fun notificacion(uidDestinatario: String, notificacionId: String) =
        "notificaciones/$uidDestinatario/$notificacionId"
}
