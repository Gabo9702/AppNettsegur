# M3 — Notificaciones · Resumen

Módulo **M3** (MODULOS.md, secciones 2, 4, 6 y 7). Rama `modulo/notificaciones`, creada desde `main`
después de integrar M1.
Desarrollo: Gabriel Rodríguez. Diseño: Santiago Torres. Pruebas/documentación: Juan Diego Rojas.

M3 implementa la **recepción** de avisos: bandeja, campana con contador, notificaciones del sistema
Android y alertas de vencimiento para RH. La **escritura** de las notificaciones ya la resolvió M1
(`NotificacionFactory` + escrituras multi-ruta).

---

## 1. Qué se construyó

**Fase 1 — Repositorio y bandeja**
- `data/repository/NotificacionesRepository` (interfaz + impl contra RTDB): `observar(uid)` (últimas
  50 por `fecha`), `noLeidas(uid)`, `marcarLeida` y `marcarTodasLeidas` (solo escriben `leida`, según
  las reglas de M1) y `leerUnaVez(uid)` (lectura puntual para el worker).
- `notificaciones/AgrupadorFecha.kt`: agrupación **pura** Hoy / Esta semana / Anteriores.
- `ui/notificaciones/`: `BandejaScreen` (lista agrupada, ícono por tipo, leídas vs no leídas, marcar
  todas, estado vacío, navegación por tipo), `BandejaViewModel`, `CampanaNotificaciones` (badge "9+")
  y `CampanaViewModel`.

**Fase 2 — Notificaciones del sistema Android**
- `PublicadorNotificaciones`: canal "Actualizaciones de proyectos" (creado en `Application`) y
  publicación con `NotificationCompat` + `PendingIntent` al destino.
- `SolicitarPermisoNotificaciones` (`POST_NOTIFICATIONS` en Android 13+, con explicación previa; si
  lo niega, aviso discreto y la bandeja sigue funcionando).
- `ObservadorNotificaciones`: listener de `notificaciones/{uid}` ligado a `ProcessLifecycleOwner`.
  En primer plano solo contador + snackbar; en segundo plano, notificación del sistema.
- `NotificacionesStore` (DataStore) para el registro de "ya mostradas" (se limpia al cerrar sesión);
  lógica pura en `RegistroMostradas`.
- `MainActivity` (singleTop) enruta el `EXTRA_RUTA` de la notificación; `NavGraph` navega al destino.

**Fase 3 — App cerrada (WorkManager)**
- `NotificacionesWorker` (CoroutineWorker): verifica `currentUser`, lee las no leídas no mostradas y
  publica; si hay más de 3, un solo aviso resumido. **Solo lee**.
- `ProgramadorTrabajos`: `PeriodicWorkRequest` de 15 min con `NetworkType.CONNECTED`,
  `enqueueUniquePeriodicWork` política KEEP. Se programa al iniciar sesión cliente/ingeniero y se
  cancela al cerrar sesión.

**Fase 4 — Alertas de vencimiento para RH**
- `VencimientosRh` (pura): revisa el cumplimiento documento por documento (VENCIDO / POR_VENCER).
- `AlertasRhPublicador`: publica alertas **locales** con ID determinista por empleado+tipo+fecha, sin
  repetir la misma alerta el mismo día. **No** se escriben en Firebase.
- `VencimientosRhWorker`: worker diario (con red, solo si el rol en caché es RH).
- `ui/notificaciones/ResumenVencimientosRh` + `RhVencimientosViewModel`: tarjeta "N vencidos ·
  M por vencer" en el inicio de RH; al entrar dispara las alertas del día.

**Fase 5 — Pruebas**
- Unitarias (puras, sin Firebase): `AgrupadorFechaTest`, `RegistroMostradasTest` (nuevas + conteo de
  no leídas) y `VencimientosRhTest`. Todas en verde con `./gradlew testDebugUnitTest`.
- Guía de prueba con dos emuladores: `docs/m3_pruebas_dos_emuladores.md`.

---

## 2. Decisiones tomadas

- **Recepción centralizada en el proceso.** `ObservadorNotificaciones` sigue a `sesion.usuario` y, por
  rol, engancha/suelta el listener y programa/cancela los workers. Así el ciclo de vida de las
  notificaciones vive dentro de M3 sin tocar `SesionRepository` (M1).
- **Primer plano vs. segundo plano** con `ProcessLifecycleOwner`: en primer plano solo contador +
  snackbar (no molestar con una notificación del sistema); en segundo plano, aviso del sistema.
- **Idempotencia en la recepción.** Como los ID de notificación son deterministas (M1), un reintento
  no crea duplicados; el registro local de "ya mostradas" evita repetir el aviso del sistema.
- **Alertas de RH locales y deterministas.** Se calculan en el dispositivo con `VencimientosRh` y se
  deduplican por día; no se escriben en Firebase (no son eventos de otros usuarios).
- **Lógica pura y testeable** separada de Android/Firebase (`AgrupadorFecha`, `RegistroMostradas`,
  `VencimientosRh`), para poder probarla con reloj/fecha fijos.

---

## 3. Limitaciones conocidas (trabajo futuro)

- **Sin push real con la app cerrada.** Cloud Functions + FCM requieren el plan Blaze (sección 2). El
  worker periódico corre cada **15 min** (mínimo de Android) y solo con red, así que el aviso con la
  app cerrada puede llegar con **15 minutos de retraso o más**.
- **Reintento sobre una notificación ya leída** puede volver a marcarla como no leída (limitación
  heredada del diseño de M1, sección 5.3). Aceptable.
- **Técnico no recibe notificaciones** en esta entrega (parte 1, sección 7).

---

## 4. Problema técnico principal resuelto (punto 18)

**Entregar notificaciones a cada rol sin servidor (plan gratuito), diferenciando app abierta, en
segundo plano y cerrada, sin duplicar avisos ni romper el flujo offline del técnico.**

La solución combina tres piezas de recepción sobre los nodos `notificaciones/{uid}` que M1 escribe de
forma atómica: (1) un **listener ligado al ciclo de vida del proceso** que decide entre snackbar
(primer plano) y notificación del sistema (segundo plano); (2) un **WorkManager periódico** para la
app cerrada, que **solo lee** (respeta la prohibición del taller de sincronizar en segundo plano); y
(3) un **registro local de "ya mostradas"** que, junto con los ID deterministas de M1, garantiza que
cada aviso se muestre una sola vez aunque el técnico sincronice dos veces. Para RH, donde no hay un
evento en Firebase, las alertas de vencimiento se **calculan en el dispositivo** y se deduplican por
día.

---

## 5. Cambios que hay que llevar a la documentación (MODULOS.md, parte 1, sección 10)

1. **Nuevo permiso:** `POST_NOTIFICATIONS` (Android 13+), con explicación previa; si se niega, la
   bandeja sigue funcionando. Reflejarlo en el documento del taller (permisos de la app).
2. **Nuevas dependencias:** `androidx.lifecycle:lifecycle-process` y `androidx.work:work-runtime-ktx`.
3. **Limitación de 15 min con la app cerrada** (sección 2): dejarla explícita en "Trabajo futuro"
   (push real con FCM + Cloud Functions, plan Blaze).

---

## 6. Nota sobre el contrato (archivos tocados fuera de los paquetes de M3)

Además de `notificaciones/`, `ui/notificaciones/` y `NotificacionesRepository` (los que la sección 6.1
asigna a M3), se tocaron, como puntos de integración inevitables y **sin cambiar ninguna firma de la
sección 6.2**:

- `AppNettsegur.kt` (AppContainer): se agregaron `notificacionesRepository`, `notificacionesStore` y
  `observadorNotificaciones`, y se arranca el observador.
- `navigation/NavGraph.kt` y `navigation/PantallasRelleno.kt`: la ruta `BANDEJA` abre la bandeja real;
  las pantallas de relleno de cada rol muestran la **campana real** (M3), el permiso (cliente/
  ingeniero) y la tarjeta de vencimientos (RH). Son los rellenos que M1 dejó para que M3/M2 los
  reemplazaran; M2 los sustituirá por las pantallas definitivas.
- `MainActivity.kt`: `singleTop` + enrutado del `EXTRA_RUTA` de la notificación.
- `config/Mensajes.kt`: textos de recepción (bandeja, campana, canal, permiso, resumen, vencimientos).
- `AndroidManifest.xml`, `gradle/libs.versions.toml`, `app/build.gradle.kts`: permiso, `singleTop`,
  dependencias e ícono de barra de estado.

M1 ya había dejado `Limites.MAX_NOTIFICACIONES` y el slot `acciones` de `BarraRol` para la campana.
