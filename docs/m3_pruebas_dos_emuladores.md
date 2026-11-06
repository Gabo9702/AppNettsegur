# M3 — Guía de prueba con dos emuladores (técnico y cliente)

> Módulo **M3 – Notificaciones**, fase 5. Esta guía es para Juan Diego (pruebas y documentación,
> parte 1, sección 8.1). Verifica que el aviso llega **solo cuando Firebase confirma** la novedad,
> que no se duplica y que llega con la app cerrada dentro del ciclo del worker.
>
> **Requisito previo:** `app/google-services.json` configurado y las cuentas de prueba creadas e
> importado el seed (ver `firebase/LEEME.md`). Sin eso, el login y la recepción no funcionan.

## Preparación

1. Levanta **dos emuladores** (o un emulador + un dispositivo físico). Llámalos **TÉC** y **CLI**.
2. Instala el APK debug en ambos: `./gradlew installDebug` con cada uno seleccionado, o arrastra el
   APK de `app/build/outputs/apk/debug/`.
3. En **TÉC** inicia sesión con `tecnico@...`. En **CLI** inicia sesión con `cliente@...`
   (debe ser el cliente de un proyecto asignado al técnico; ver el seed).
4. En **CLI**, cuando entres al inicio, **acepta el permiso de notificaciones** (Android 13+).
   - 📸 *Captura 0:* el diálogo de explicación del permiso y el inicio del cliente con la campana.

## Paso 1 — El técnico guarda sin conexión y el cliente NO recibe nada

1. En **TÉC**, activa **modo avión** (o desactiva datos/Wi-Fi).
2. Crea una novedad completa (mín. 1 foto y las dos firmas) y pulsa **GUARDAR EN EL DISPOSITIVO**.
   Queda en estado **PENDIENTE**.
3. Observa **CLI**: **no** debe aparecer ninguna notificación ni cambiar el contador de la campana.
   - 📸 *Captura 1:* lado a lado — TÉC con el registro PENDIENTE y CLI con la campana en 0.
   - ✔️ Esto prueba que **GUARDAR nunca notifica**: el aviso no sale al guardar.

## Paso 2 — El técnico sincroniza y cliente e ingeniero reciben el aviso

1. En **TÉC**, desactiva el modo avión (recupera Internet). **No** debe enviarse nada solo (no hay
   sincronización automática).
2. Pulsa **SINCRONIZAR**. El registro pasa a **SINCRONIZADO**.
3. Observa **CLI**:
   - si la app del cliente está **en primer plano**: aparece un **snackbar** y el contador de la
     campana sube a **1**;
   - si está **en segundo plano**: aparece una **notificación del sistema** "Nueva novedad registrada".
   - 📸 *Captura 2a:* TÉC con el registro SINCRONIZADO.
   - 📸 *Captura 2b:* CLI con el aviso (snackbar o notificación del sistema) y la campana en 1.
4. Abre la campana en **CLI** → la **Bandeja** muestra la notificación en el grupo "Hoy".
   Al tocarla, se marca como leída y navega a la novedad.
   - 📸 *Captura 2c:* la bandeja con la notificación y, al tocar, el detalle de la novedad remota.
5. (Opcional, si tienes una cuenta `ingeniero@...` del mismo proyecto en un tercer dispositivo)
   el ingeniero recibe el mismo aviso.

## Paso 3 — Segunda sincronización: no llega aviso duplicado

1. En **TÉC**, pulsa **SINCRONIZAR** otra vez (sin crear nada nuevo).
2. Observa **CLI**: **no** debe aparecer una notificación nueva ni subir el contador por ese registro.
   - El nodo de la notificación tiene un **ID determinista** (`nov_{idRegistro}`): el reintento
     **sobrescribe** el mismo nodo, no crea uno nuevo. El registro de "ya mostradas" evita repetir
     el aviso del sistema.
   - 📸 *Captura 3:* CLI con la campana aún en 1 (o en 0 si ya la leíste), sin aviso nuevo.
   - ⚠️ *Limitación documentada:* si la notificación ya estaba **leída**, el reintento la vuelve a
     marcar como no leída (puede subir el contador). Es aceptable y está documentado (sección 5.3).

## Paso 4 — (Queda para M2)

La prueba "el **ingeniero sube un documento** y el cliente recibe el aviso" se hará en **M2**, porque
la pantalla del ingeniero para subir documentos todavía no existe. La tubería (NotificacionFactory +
escritura multi-ruta de M1, y la recepción de M3) ya está lista.

## Paso 5 — Con la app del cliente cerrada, el aviso llega dentro del ciclo del worker

1. En **CLI**, **cierra la app** (deslízala fuera de recientes para que el proceso termine).
2. En **TÉC**, crea y **sincroniza** otra novedad.
3. Espera el ciclo del **worker periódico** (mínimo **15 minutos** en Android; puede tardar más
   porque Android difiere los trabajos en segundo plano).
   - Para forzarlo en la prueba y no esperar 15 min, usa:
     `adb shell cmd jobscheduler run -f com.cgro.appnettsegur <JOB_ID>` o, más sencillo, desde
     **Android Studio → App Inspection → Background Task Inspector**, localiza el *Work*
     "notificaciones_recepcion" y pulsa **Run next**.
   - 📸 *Captura 5:* la notificación del sistema en **CLI** con la app cerrada, y (opcional) el
     Background Task Inspector mostrando el trabajo periódico encolado.
   - ⚠️ *Limitación conocida:* sin un servidor (Cloud Functions + FCM requieren el plan Blaze), no
     hay push real; por eso el aviso con la app cerrada puede llegar con **15 minutos de retraso o más**.

## Qué se está verificando (resumen)

| Paso | Qué prueba |
|---|---|
| 1 | GUARDAR no notifica; sin conexión no sale nada. |
| 2 | El aviso llega a cliente (e ingeniero) **solo al confirmar Firebase** la sincronización. |
| 3 | Una segunda sincronización **no duplica** el aviso (ID determinista + "ya mostradas"). |
| 5 | Con la app cerrada, el aviso llega dentro del ciclo del worker (con el retraso documentado). |
