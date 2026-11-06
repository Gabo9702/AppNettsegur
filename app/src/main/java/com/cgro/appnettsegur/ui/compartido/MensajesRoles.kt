package com.cgro.appnettsegur.ui.compartido

/**
 * Textos de las pantallas de los roles (M2). Se mantienen centralizados aquí, dentro de un paquete
 * de M2, para no modificar config/Mensajes.kt (archivo de M1) y poder ajustarlos en vivo.
 */
object MensajesRoles {

    // Comunes
    const val REINTENTAR = "Reintentar"
    const val VOLVER = "Volver"
    const val DESCARGANDO = "Descargando…"
    const val SIN_APP_PARA_ABRIR = "No hay una app instalada para abrir este archivo."
    const val DESCARGA_ERROR = "No se pudo abrir el archivo. Intenta de nuevo."

    // Cliente / Ingeniero: inicio y lista de proyectos
    const val INICIO_CLIENTE_TITULO = "Mis proyectos"
    const val INICIO_INGENIERO_TITULO = "Proyectos a mi cargo"
    const val SIN_PROYECTOS = "No tienes proyectos asignados por ahora."
    fun avance(p: Int) = "Avance: $p%"

    // Pestañas del detalle de proyecto
    const val TAB_AVANCE = "Avance"
    const val TAB_DOCUMENTOS = "Documentos"
    const val TAB_PERSONAL = "Personal"
    const val TAB_NOVEDADES = "Novedades"
    const val TAB_CARTAS = "Cartas"

    // Avance / cronograma
    const val CRONOGRAMA_TITULO = "Cronograma de hitos"
    const val SIN_HITOS = "El proyecto todavía no tiene hitos."
    const val HITO_COMPLETADO = "Completado"
    const val HITO_PENDIENTE = "Pendiente"

    // Documentos
    const val SIN_DOCUMENTOS = "No hay documentos en este proyecto."
    const val CARGAR_DOCUMENTO = "Cargar documento"
    const val DOC_PUBLICADO = "Documento publicado. El cliente fue notificado."
    const val DOC_SELECCIONAR_TIPO = "Tipo de documento"
    const val DOC_SELECCIONAR_ARCHIVO = "Elegir archivo (PDF, JPG o PNG)"
    const val DOC_CONFIRMAR_TITULO = "¿Publicar el documento?"
    fun docConfirmarTexto(tipo: String, archivo: String) =
        "Se publicará «$archivo» como $tipo y se notificará al cliente del proyecto."
    const val DOC_PUBLICAR = "Publicar"
    const val DOC_SUBIENDO = "Publicando documento…"

    // Personal
    const val SIN_PERSONAL = "No hay personal asignado a este proyecto."

    // Novedades
    const val SIN_NOVEDADES = "No hay novedades sincronizadas en este proyecto."
    const val FILTRO_TODAS = "Todas"
    const val FILTRO_RESUELTAS = "Resueltas"
    const val FILTRO_NO_RESUELTAS = "No resueltas"
    const val NOVEDAD_NO_DISPONIBLE = "La novedad ya no está disponible."

    // Cartas
    const val SIN_CARTAS = "No hay cartas de presentación en este proyecto."
    fun cartaEtiqueta(fecha: String) = "Carta de presentación · $fecha"

    // Cronograma (acción ingeniero)
    const val CRONOGRAMA_EDITAR = "Marca o desmarca los hitos terminados. El avance se recalcula solo."

    // Técnicos (acción ingeniero)
    const val TAB_TECNICOS = "Técnicos"
    const val TECNICOS_TITULO = "Técnicos del proyecto"
    const val TECNICOS_AYUDA = "Solo puedes asignar técnicos habilitados (cumplimiento al día)."
    const val SIN_EMPLEADOS = "No hay empleados registrados."
    const val ASIGNAR = "Asignar"
    const val QUITAR = "Quitar"
    const val TECNICO_SIN_CUENTA = "Sin cuenta de técnico: no se puede asignar."
    fun noHabilitadoDesde(detalle: String) = "No habilitado · $detalle"

    // RH
    const val INICIO_RH_TITULO = "Empleados"
    const val RH_BUSCAR = "Buscar por nombre o cédula"
    const val RH_NUEVO_EMPLEADO = "Nuevo empleado"
    const val RH_SIN_RESULTADOS = "Ningún empleado coincide con la búsqueda."
    const val EMP_NOMBRE = "Nombre"
    const val EMP_CEDULA = "Cédula"
    const val EMP_CARGO = "Cargo"
    const val EMP_UID = "UID de técnico (opcional)"
    const val EMP_GUARDAR = "Guardar"
    const val EMP_GUARDADO = "Empleado guardado"
    const val EMP_CREAR_TITULO = "Nuevo empleado"
    const val EMP_EDITAR_TITULO = "Editar empleado"

    // Cumplimiento (EmpleadoDetalle)
    const val CUMPLIMIENTO_TITULO = "Documentos de cumplimiento"
    const val CARNE_TITULO = "Carné"
    const val CARNE_CARGAR = "Cargar carné"
    const val CARNE_SIN = "Sin carné cargado."
    const val CUMPLIMIENTO_CARGAR = "Cargar documento"
    const val CUMPLIMIENTO_SIN = "Sin cargar"
    const val FECHA_EXPEDICION = "Fecha de expedición"
    const val FECHA_VENCIMIENTO = "Fecha de vencimiento"
    const val CUMPLIMIENTO_FECHAS_INVALIDAS =
        "El vencimiento debe ser posterior a la expedición y la expedición no puede ser futura."
    const val CUMPLIMIENTO_GUARDADO = "Documento de cumplimiento guardado"
    fun venceEl(fecha: String) = "Vence el $fecha"
    fun habilitadoObra(habilitado: Boolean) =
        if (habilitado) "Habilitado para obra" else "No habilitado para obra"
    val NOMBRE_CUMPLIMIENTO = mapOf(
        "ARL" to "ARL",
        "SEGURIDAD_SOCIAL" to "Seguridad social",
        "ALTURAS" to "Certificado de alturas",
    )

    // Analista
    const val INICIO_ANALISTA_TITULO = "Administración"
    const val ANALISTA_TAB_CARNES = "Carnés"
    const val ANALISTA_TAB_CARTAS = "Cartas"
    const val ANALISTA_GENERAR_CARTA = "Generar carta de presentación"
    const val ANALISTA_ELEGIR_PROYECTO = "Proyecto"
    const val ANALISTA_ELEGIR_TECNICOS = "Técnicos a incluir (solo habilitados)"
    const val ANALISTA_SIN_TECNICOS_HABILITADOS =
        "El proyecto no tiene técnicos asignados habilitados para incluir en la carta."
    const val ANALISTA_VISTA_PREVIA = "Vista previa"
    const val ANALISTA_GUARDAR_CARTA = "Guardar y notificar al cliente"
    const val ANALISTA_CARTA_GUARDADA = "Carta guardada. El cliente fue notificado."
    const val ANALISTA_GENERANDO = "Generando carta…"
    const val CARNE_GUARDADO = "Carné cargado"
    const val ELEGIR_ARCHIVO_IMG_PDF = "Elegir imagen o PDF"
}
