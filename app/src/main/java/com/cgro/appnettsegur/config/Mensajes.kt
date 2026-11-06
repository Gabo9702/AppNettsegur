package com.cgro.appnettsegur.config

/** Todos los textos que ve el usuario y que pueden cambiarse en vivo. */
object Mensajes {

    // Reglas de validación (sección 6)
    const val R1_MOTIVO = "Indique por qué la novedad no quedó resuelta (mínimo ${Limites.MIN_MOTIVO_NO_RESUELTA} caracteres)."
    const val R2_HORAS = "La hora de inicio debe ser anterior a la hora de finalización."
    const val R2_FECHA_FUTURA = "La fecha del servicio no puede ser posterior a hoy."
    const val R3_CEDULA = "La cédula debe contener solo números (entre ${Limites.MIN_CEDULA} y ${Limites.MAX_CEDULA} dígitos)."
    const val R4_SERIAL = "Serial inválido: use solo letras y números, sin espacios (4 a 20 caracteres)."
    const val R5_DESC_REALIZADO = "Describa la solución aplicada (mínimo ${Limites.MIN_DESC_REALIZADO} caracteres y diferente del servicio asignado)."
    const val R6_SIN_FOTO = "Agregue una foto de evidencia antes de guardar."
    const val R6_FOTO_NO_DISPONIBLE = "La foto ya no está disponible; tómela de nuevo."
    const val R6_MAX_FOTOS = "Puede adjuntar máximo ${Limites.MAX_EVIDENCIAS} fotos."
    const val R6_FIRMA_TECNICO = "Falta la firma del técnico."
    const val R6_FIRMA_REPRESENTANTE = "Falta la firma del representante."
    const val CAMPO_OBLIGATORIO = "Este campo es obligatorio."
    const val HAY_ERRORES = "Revise los campos marcados en rojo."

    // Captura
    const val GUARDADO = "Guardado en el dispositivo"
    const val CAPTURA_SIN_PROYECTOS = "Conéctate una vez para descargar tus proyectos asignados."
    const val ERROR_GUARDANDO = "No se pudo guardar en el dispositivo. Intente de nuevo."
    const val ERROR_IMAGEN = "No se pudo procesar la imagen. Intente con otra foto."
    const val SALIR_SIN_GUARDAR_TITULO = "¿Salir sin guardar?"
    const val SALIR_SIN_GUARDAR_TEXTO = "Los cambios que no ha guardado se perderán."
    const val SINCRONIZADO_NO_EDITABLE =
        "Este registro ya fue enviado y no puede modificarse. Si hay un cambio, registre una nueva novedad."

    // Detalle
    const val ELIMINAR_TITULO = "¿Eliminar este registro?"
    const val ELIMINAR_TEXTO = "Se borrarán del dispositivo el registro, sus fotos y sus firmas. Esta acción no se puede deshacer."
    const val ELIMINADO = "Registro eliminado"

    // Conexión (sección 10.3)
    const val BANNER_SIN_CONEXION =
        "Sin conexión: tus registros se guardan en el dispositivo. Pulsa SINCRONIZAR cuando tengas Internet."

    fun bannerConectado(pendientes: Int) =
        "Conectado. Tienes $pendientes registros pendientes por sincronizar."

    // Sincronización (sección 9.2)
    const val SYNC_SIN_CONEXION = "Sin conexión. Tus registros siguen pendientes."
    const val SYNC_NADA_PENDIENTE = "No hay registros por sincronizar."
    const val SYNC_FIREBASE_NO_CONFIGURADO =
        "Firebase no está configurado (falta google-services.json). Tus registros siguen pendientes."
    const val SYNC_FALTA_EVIDENCIA = "Falta evidencia fotográfica"
    const val SYNC_FALTA_FIRMA = "Falta la firma del técnico o del representante"
    const val SYNC_TIMEOUT = "Tiempo de espera agotado: Firebase no confirmó el envío."
    const val SYNC_PERMISO_DENEGADO = "Firebase rechazó el envío (permiso denegado). Revise las reglas de la base de datos."
    const val SYNC_ERROR_DESCONOCIDO = "Error desconocido al enviar."
    const val SYNC_FALLO_SIMULADO = "Fallo simulado desde el menú de desarrollador."

    fun resumenSync(ok: Int, conError: Int) = "$ok sincronizados · $conError con error"
    fun progresoSync(actual: Int, total: Int) = "Enviando $actual de $total…"

    // ---- Módulos nuevos (MODULOS.md) ----

    // Inicio de sesión (sección 5.1)
    const val LOGIN_TITULO = "Iniciar sesión"
    const val LOGIN_SUBTITULO = "Ingrese con su cuenta de Nettsegur."
    const val LOGIN_CORREO = "Correo"
    const val LOGIN_CLAVE = "Contraseña"
    const val LOGIN_ENTRAR = "Entrar"
    const val LOGIN_CORREO_VACIO = "Escriba su correo."
    const val LOGIN_CLAVE_VACIA = "Escriba su contraseña."
    const val LOGIN_CREDENCIALES = "Correo o contraseña incorrectos."
    const val LOGIN_SIN_RED = "Sin conexión. Conéctate a Internet para iniciar sesión la primera vez."
    const val LOGIN_SIN_ROL = "Su cuenta no tiene un rol asignado. Contacte al administrador."
    const val LOGIN_FIREBASE_NO_CONFIGURADO =
        "Firebase no está configurado (falta google-services.json). No es posible iniciar sesión."
    const val LOGIN_ERROR = "No se pudo iniciar sesión. Intente de nuevo."

    // Cierre de sesión con pendientes (sección 5.1)
    const val CERRAR_SESION = "Cerrar sesión"
    const val CERRAR_SESION_TITULO = "¿Cerrar sesión?"
    fun cerrarSesionConPendientes(n: Int) =
        "Tienes $n registros sin sincronizar. Sincroniza antes de cerrar sesión."

    // Secciones que necesitan conexión (sección 3)
    const val SECCION_NECESITA_INTERNET = "Esta sección necesita conexión a Internet."

    // Proyectos del técnico (sección 5.2)
    const val PROYECTOS_SIN_CONEXION = "Sin conexión. No se pueden actualizar los proyectos ahora."
    const val PROYECTOS_ERROR = "No se pudieron actualizar los proyectos. Intenta de nuevo."
    fun proyectosActualizados(n: Int) = "Proyectos actualizados: $n asignados."

    // Firebase / archivos (secciones 2 y 6.2)
    const val FIREBASE_NO_CONFIGURADO = "Firebase no está configurado (falta google-services.json)."
    const val ARCHIVO_TIPO_NO_PERMITIDO = "Solo se permiten archivos PDF, JPG o PNG."
    const val ARCHIVO_MUY_GRANDE = "El archivo supera el máximo de 1,5 MB."
    const val ARCHIVO_NO_LEIDO = "No se pudo leer el archivo seleccionado."
    const val ARCHIVO_NO_ENCONTRADO = "El archivo ya no está disponible en el servidor."
    const val EMPLEADO_SIN_USUARIO = "El empleado no tiene una cuenta de técnico asociada."

    // Pantallas de relleno (se reemplazan en M2)
    fun pantallaEnConstruccion(rol: String) = "Inicio de $rol (en construcción)."

    // Notificaciones (secciones 4 y 7). Títulos y mensajes de cada aviso.
    const val NOTIF_NOVEDAD_TITULO = "Nueva novedad registrada"
    fun notifNovedadMensaje(cliente: String) = "Se sincronizó una novedad del servicio técnico de $cliente."
    const val NOTIF_DOCUMENTO_TITULO = "Nuevo documento en tu proyecto"
    fun notifDocumentoMensaje(nombre: String) = "El ingeniero publicó el documento: $nombre."
    const val NOTIF_CARTA_TITULO = "Nueva carta de presentación"
    const val NOTIF_CARTA_MENSAJE = "El área administrativa publicó una carta de presentación para tu proyecto."

    // Recepción de notificaciones (M3): bandeja, campana y sistema Android.
    const val BANDEJA_TITULO = "Notificaciones"
    const val BANDEJA_VACIA = "No tienes notificaciones todavía."
    const val BANDEJA_MARCAR_TODAS = "Marcar todas como leídas"
    const val CAMPANA_DESC = "Notificaciones"
    fun campanaNoLeidas(n: Int) = "$n notificaciones sin leer"
    // Canal de notificaciones del sistema Android (fase 2).
    const val CANAL_ID = "actualizaciones_proyectos"
    const val CANAL_NOMBRE = "Actualizaciones de proyectos"
    const val CANAL_DESC = "Avisos de novedades, documentos y cartas de tus proyectos."
    // Permiso POST_NOTIFICATIONS (Android 13+), con explicación previa (fase 2).
    const val PERMISO_NOTIF_TITULO = "Permitir notificaciones"
    const val PERMISO_NOTIF_TEXTO =
        "Te avisaremos cuando haya novedades, documentos o cartas nuevas en tus proyectos. Puedes revisarlas también en la campana."
    const val PERMISO_NOTIF_ACEPTAR = "Permitir"
    const val PERMISO_NOTIF_AHORA_NO = "Ahora no"
    const val PERMISO_NOTIF_NEGADO =
        "Sin permiso de notificaciones. Revisa la campana para ver tus avisos."
    // Resumen de varias notificaciones con la app cerrada (fase 3).
    fun notifResumen(n: Int) = "Tienes $n actualizaciones nuevas"
    // Alertas de vencimiento para RH (fase 4).
    const val VENC_TITULO = "Documentos de cumplimiento"
    fun vencMensaje(empleado: String, documento: String, estado: String) =
        "$empleado: $documento $estado."
    fun rhResumenVencimientos(vencidos: Int, porVencer: Int) =
        "$vencidos documentos vencidos · $porVencer por vencer"

    // Perfil
    const val PERFIL_GUARDADO = "Perfil guardado"
}
