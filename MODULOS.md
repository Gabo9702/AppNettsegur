# AppNettsegur — Módulos nuevos: guía completa

> Este documento reúne todo lo necesario para los módulos nuevos: el resumen de las decisiones, el contrato técnico que comparten los tres módulos y los prompts para Claude Code.
> Guárdalo en la **raíz del proyecto** con el nombre `MODULOS.md`, junto a `CLAUDE.md`. Los prompts lo leen con ese nombre.
> `CLAUDE.md` describe la app del taller (técnico, captura offline y sincronización manual). Este documento describe los módulos nuevos. **Si los dos se contradicen, este documento manda.**
> **El código de los tres módulos lo desarrolla una sola persona (Gabriel), en orden, desde su terminal.** Los otros dos integrantes aportan diseño, pruebas y documentación con evidencia propia (sección 8.1).
> Si el contrato necesita cambiar, primero se actualiza aquí y se avisa al equipo por WhatsApp; después se programa.

## Resumen de decisiones

- **Alcance:** 4 roles además del técnico: cliente, ingeniero de proyectos, HCQ y RH, y analista administrativo. Contabilidad, gerencia y soporte quedan como trabajo futuro (parte 1, sección 11).
- **Backend:** solo Firebase (Authentication y Realtime Database, plan gratuito Spark). No se usa Supabase.
- **Módulos:** al quitar Supabase, «base de datos» y Firebase quedan en un solo módulo. Son **3 módulos**:
  - M1 – Datos y Firebase;
  - M2 – Funciones por rol;
  - M3 – Notificaciones.
- **Orden de trabajo (secuencial, una sola persona):** M1 completo → M3 → M2. Cada módulo en su rama y con una sesión nueva de Claude Code. M3 va antes que M2 porque M2 coloca la campana que crea M3.
- **Evidencia individual (punto 18 del taller):** como los commits de código serán de Gabriel, Santiago y Juan Diego deben dejar evidencia propia de diseño, pruebas y documentación (parte 1, sección 8.1).

### Lo que cambia por usar solo Firebase

Desde el 3 de febrero de 2026, Cloud Storage para Firebase siempre requiere el plan Blaze, y Cloud Functions también. Por eso:

- **Documentos y cartas:** se guardan dentro de Realtime Database en Base64, con un límite de 1,5 MB por archivo.
- **Notificaciones con la app abierta:** el aviso al cliente y al ingeniero llega de inmediato.
- **Notificaciones con la app cerrada:** se revisan cada 15 minutos con WorkManager, así que pueden tardar. Está documentado como limitación y como trabajo futuro.
- **Sincronización del técnico:** la novedad y sus notificaciones se escriben en **una sola operación** (`updateChildren` multi-ruta). O se guarda todo o no se guarda nada, y reintentar no duplica nada. Así se cumple el taller y el aviso sale solo cuando Firebase confirma el registro, nunca al pulsar GUARDAR.

### Cambios que hay que hacer en la documentación del taller al terminar

El taller penaliza que la app y la documentación no coincidan (punto 22). Hay que actualizar:

- **Punto 11:** la sincronización usa `updateChildren` multi-ruta en lugar de `setValue`.
- **Diccionario de datos:** agregar el campo `uidTecnico`.
- **Punto 5:** la primera vez que se inicia sesión se necesita Internet; después la app abre sin conexión.
- **Punto 18:** Gabriel, desarrollo de M1, M2 y M3; Santiago, diseño visual de las pantallas nuevas; Juan Diego, pruebas y documentación (parte 1, sección 8.1).
- **Documento académico:** agregar la sección «Trabajo futuro».

---

# PARTE 1 — Contrato compartido entre los módulos

## 0. Regla número uno: no romper el taller

El taller se califica completo sobre el flujo del técnico. Nada de lo nuevo puede romper esto:

- El técnico abre la app y captura **sin Internet**, siempre que ya haya iniciado sesión antes (ver 5.1).
- **GUARDAR** escribe solo en Room.
- Firebase solo se usa al pulsar **SINCRONIZAR**. No hay sincronización automática de novedades.
- Los estados son PENDIENTE, SINCRONIZADO y ERROR. Hay envío individual, tiempo límite de 15 s y ningún duplicado en la segunda sincronización.
- **No** se activa `setPersistenceEnabled(true)`.

Después de cualquier cambio que toque el flujo del técnico, se repiten las **pruebas A a E** del taller.

---

## 1. Alcance

| Rol | Entra en esta entrega | Casos de uso / HU |
|---|---|---|
| Técnico | Ya existe (taller) | CU-02, HU-05 |
| Cliente | **Sí** | CU-03, CU-06, HU-02, 03, 06, 07, 08, 09, 10 |
| Ingeniero de proyectos | **Sí** | CU-05, HU-11, HU-12 |
| HCQ y RH | **Sí** | CU-07, HU-15 |
| Analista administrativo | **Sí** | CU-09, HU-13 |
| Contabilidad, Gerencia y Soporte | **No: trabajo futuro** (sección 11) | CU-01, CU-04, CU-08, HU-14, 16, 17 |

Los tres módulos son:

| Módulo | Qué hace | Rama de Git |
|---|---|---|
| **M1 – Datos y Firebase** | Inicio de sesión, roles, estructura de RTDB, reglas de seguridad, cambios en Room, repositorios compartidos y ajuste de la sincronización del técnico | `modulo/datos-firebase` |
| **M2 – Funciones por rol** | Pantallas y lógica de cliente, ingeniero, RH y analista | `modulo/roles` |
| **M3 – Notificaciones** | Recepción de avisos, bandeja, notificaciones de Android y alertas de vencimiento | `modulo/notificaciones` |

---

## 2. Limitaciones del plan gratuito de Firebase (Spark)

Se usa **solo** Firebase Authentication (correo y contraseña) y Firebase Realtime Database.

- **Cloud Storage para Firebase requiere el plan Blaze**, que pide tarjeta. Por eso los **documentos** (PDF o imagen) se guardan en RTDB como **Base64**, con un **límite de 1,5 MB** por archivo original. Los archivos más grandes se rechazan con un mensaje.
- **Cloud Functions requiere el plan Blaze.** Sin servidor no hay notificaciones *push* reales, así que:
  - quien genera el evento escribe la notificación en RTDB, en la **misma escritura atómica** que el dato;
  - la app del destinatario la recibe con un *listener* mientras está abierta, y con un **WorkManager periódico** (cada 15 min, con red) cuando está cerrada;
  - la notificación con la app cerrada puede llegar con **hasta 15 minutos de retraso o más** (Android puede diferir los trabajos en segundo plano). Esto se documenta como limitación.
- Los roles **no** se manejan con *custom claims*, porque necesitan un servidor. Se guardan en `usuarios/{uid}/rol` y las reglas de seguridad los leen desde ahí.

---

## 3. Roles y permisos

| Rol (`rol`) | Qué puede ver | Qué puede hacer | ¿Necesita Internet? |
|---|---|---|---|
| `TECNICO` | Sus proyectos asignados (en caché) y sus registros | Capturar, guardar y sincronizar novedades (taller) | No para capturar; sí para iniciar sesión la primera vez y para sincronizar |
| `CLIENTE` | Solo los proyectos cuyo `clienteUid` es su uid: avance, cronograma, documentos, personal asignado, novedades sincronizadas y cartas de presentación | Descargar y abrir documentos | Sí |
| `INGENIERO` | Los proyectos cuyo `ingenieroUid` es su uid, con sus novedades | Cargar documentos, marcar hitos del cronograma y asignar técnicos **habilitados** | Sí |
| `RH` | Todos los empleados | Crear y editar empleados, cargar ARL, seguridad social y certificado de alturas con su fecha de vencimiento | Sí |
| `ANALISTA` | Empleados y proyectos | Cargar carnés y generar cartas de presentación en PDF | Sí |

- Cada cliente (empresa) tiene **una sola cuenta de usuario** en esta entrega. Varios usuarios por cliente queda como trabajo futuro.
- Las cuentas se crean **a mano** en la consola de Firebase (ver sección 9). No hay registro abierto ni rol de administrador.
- Las pantallas de los roles distintos al técnico funcionan **en línea**. Sin conexión muestran: «Esta sección necesita conexión a Internet».

---

## 4. Estructura de Firebase RTDB

```
usuarios/{uid}:
  nombre, correo, rol (TECNICO|CLIENTE|INGENIERO|RH|ANALISTA), empleadoId? (solo técnicos)

proyectos/{proyectoId}:
  nombreProyecto, nombreCliente, clienteUid, ingenieroUid,
  estado (EN_EJECUCION|FINALIZADO), avancePorcentaje (0–100, calculado de los hitos),
  hitos/{hitoId}: { nombre, fechaInicio, fechaFin, completado, orden }
  tecnicos/{uidTecnico}: true

novedades/{idRegistro}:            ← YA EXISTE (taller). Se agrega: uidTecnico
indices/novedadesPorProyecto/{proyectoId}/{idRegistro}: true
indices/proyectosPorTecnico/{uidTecnico}/{proyectoId}: true

documentos/{proyectoId}/{documentoId}:
  tipo (COTIZACION|CRONOGRAMA|INFORME_AVANCE|ACTA_ENTREGA|INFORME_FINAL),
  nombre, archivoId, mime, tamanoBytes, subidoPor, fecha

archivos/{archivoId}:               ← contenido pesado separado de los metadatos
  base64, mime, nombre, tamanoBytes, subidoPor, fecha

empleados/{empleadoId}:
  nombre, cedula, cargo, uid? (si tiene cuenta de técnico),
  cumplimiento/{ARL|SEGURIDAD_SOCIAL|ALTURAS}: { fechaExpedicion, fechaVencimiento, archivoId }
  carne: { archivoId, fecha }

cartas/{cartaId}:
  proyectoId, empleados/{empleadoId}: true, archivoId, generadaPor, fecha

notificaciones/{uidDestinatario}/{notificacionId}:
  tipo (NOVEDAD_SINCRONIZADA|DOCUMENTO_NUEVO|CARTA_NUEVA),
  titulo, mensaje, proyectoId, refId, fecha (ServerValue.TIMESTAMP), leida (false)
```

Reglas de diseño:

- **Los ID se generan en el dispositivo (UUID)**, igual que en el taller. No se usa `push()`.
- **Los ID de notificación son deterministas**: `nov_{idRegistro}`, `doc_{documentoId}`, `carta_{cartaId}`. Así, un reintento sobrescribe la misma notificación y no la duplica.
- **«Habilitado» no se guarda; se calcula.** Un empleado está habilitado si sus tres documentos de cumplimiento existen y `fechaVencimiento >= hoy`.
- **El Base64 nunca se descarga en las listas.** Las listas leen `documentos/…` y `cartas/…` (solo metadatos). `archivos/{archivoId}` se descarga únicamente al abrir el archivo.

### 4.1 Reglas de seguridad (`database.rules.json`, responsabilidad de M1)

- Todo exige `auth != null`.
- El rol se lee con `root.child('usuarios').child(auth.uid).child('rol').val()`.
- `usuarios/{uid}`: cada quien lee el suyo. Nadie escribe `rol` desde la app.
- `novedades/{id}`:
  - **escribe** solo un TECNICO, y solo si `newData.child('uidTecnico').val() == auth.uid`;
  - **lee** el técnico autor, el CLIENTE y el INGENIERO del proyecto de la novedad.
- `proyectos/{pid}`: lee el cliente, el ingeniero o un técnico asignado. Solo el INGENIERO del proyecto escribe `hitos`, `avancePorcentaje` y `tecnicos`.
- `documentos/{pid}`: escribe el INGENIERO del proyecto y lee también el CLIENTE.
- `archivos/{id}`: lo escriben INGENIERO, RH y ANALISTA. La lectura se permite a usuarios autenticados con un rol distinto de TECNICO, **más** el técnico en el caso de las cartas. Una regla más fina queda como trabajo futuro y debe documentarse.
- `empleados`: escribe RH (y ANALISTA solo el nodo `carne`). Leen RH, ANALISTA e INGENIERO. El CLIENTE solo puede leer los empleados asignados a su proyecto, a través de la pantalla de personal (ver nota).
- `notificaciones/{uid}`:
  - el destinatario lee y solo puede cambiar `leida`;
  - crean notificaciones el TECNICO (`NOVEDAD_SINCRONIZADA`), el INGENIERO (`DOCUMENTO_NUEVO`) y el ANALISTA (`CARTA_NUEVA`).
- `.indexOn` donde haga falta, por ejemplo `fecha` en notificaciones.

> **Nota sobre `empleados` y el cliente.** RTDB no filtra consultas por regla. Para HU-07, M1 publica una **vista reducida** en `proyectos/{pid}/personal/{empleadoId}` con los campos `nombre`, `cedula` y `estadoCumplimiento`, que el cliente sí puede leer. Esa vista se actualiza cuando el ingeniero asigna técnicos o cuando RH cambia documentos, siempre con escritura multi-ruta.

---

## 5. Cambios permitidos al flujo del técnico (solo M1)

### 5.1 Inicio de sesión

- Pantalla de inicio de sesión con correo y contraseña de Firebase Auth. Al entrar se lee `usuarios/{uid}` y se guarda **en caché** (DataStore) uid, nombre, rol y empleadoId.
- Al abrir la app:
  - si `FirebaseAuth.currentUser != null` y hay sesión en caché, se entra **sin pedir Internet**, directo al inicio del rol;
  - la **primera vez** sí se necesita Internet. Esto se documenta en el taller.
- `nombreTecnico` deja de venir de la pantalla de perfil: se toma del usuario en caché. La pantalla de perfil del taller se retira o queda solo para mostrar los datos.
- **Cerrar sesión con registros PENDIENTES o ERROR** muestra una advertencia: «Tienes N registros sin sincronizar. Sincroniza antes de cerrar sesión». La salida se bloquea hasta que el técnico confirme por segunda vez.

### 5.2 Proyectos del técnico

- El catálogo `assets/proyectos.json` se reemplaza por los **proyectos asignados al técnico**, leídos de `indices/proyectosPorTecnico/{uid}` y guardados en Room. Se incluye `clienteUid` e `ingenieroUid`, que la sincronización necesita para notificar.
- La lista se actualiza **cuando hay conexión**: al iniciar sesión, al abrir la pantalla de captura con red y en el botón «Actualizar proyectos».
- Sin conexión se usa la caché. Si la caché está vacía, la captura muestra: «Conéctate una vez para descargar tus proyectos asignados».

### 5.3 Sincronización: de `setValue` a `updateChildren` multi-ruta

Se mantiene **todo** el algoritmo del punto 11 del taller. Solo cambia la escritura de cada registro, que pasa a ser **una única escritura atómica**:

```kotlin
val updates = buildMap<String, Any?> {
    put("novedades/$id", mapaNovedad)                                  // incluye uidTecnico
    put("indices/novedadesPorProyecto/$proyectoId/$id", true)
    clienteUid?.let   { putAll(NotificacionFactory.novedadSincronizada(it, registro)) }
    ingenieroUid?.let { putAll(NotificacionFactory.novedadSincronizada(it, registro)) }
}
withTimeout(15_000) { FirebaseDatabase.getInstance().reference.updateChildren(updates).await() }
```

- **Atómica**: o se escriben todas las rutas o ninguna. La confirmación sigue siendo la tarea de Firebase completada con éxito.
- **Idempotente**: todas las llaves son fijas, así que un reintento no duplica la novedad ni la notificación.
- Limitación conocida: si un reintento sobrescribe una notificación ya leída, esta vuelve a aparecer como no leída. Es aceptable y debe documentarse.
- **Hay que actualizar el punto 11 del documento del taller**: donde dice «setValue en novedades/{idRegistro}» debe decir «updateChildren multi-ruta (novedad, índice y notificaciones)».

### 5.4 Room

- Migración de versión: **no usar `fallbackToDestructiveMigration`**, porque borraría los registros pendientes.
- Campos nuevos:
  - en `novedades`: `uidTecnico`;
  - en la tabla de proyectos: `clienteUid`, `ingenieroUid` y `fechaActualizacion`.
- Los datos de los demás roles **no** se guardan en Room: se leen en línea.

---

## 6. Contrato de código

### 6.1 Paquetes y archivos de cada módulo

Para evitar conflictos al integrar, **cada archivo tiene un solo módulo dueño**.

```
config/            Roles.kt, RutasFirebase.kt, Limites.kt (agregar)          → M1
data/model/        Usuario, Proyecto, Hito, Documento, Archivo, Empleado,
                   DocumentoCumplimiento, Carta, Notificacion                  → M1
data/remote/       FirebaseFlows.kt (callbackFlow genérico), *RemoteDataSource → M1
data/repository/   SesionRepository, ProyectosRepository, DocumentosRepository,
                   ArchivosRepository, EmpleadosRepository, CartasRepository   → M1
                   NotificacionesRepository                                    → M3
data/repository/SyncRepository.kt (taller)                                    → M1 (solo 5.3)
notificaciones/    NotificacionFactory.kt                                      → M1 (lo crea) / M3 (lo mantiene)
notificaciones/    *el resto*                                                  → M3
ui/login/                                                                     → M1
navigation/        NavGraph, Destinos (incluye rutas de todos los roles)       → M1
ui/cliente/ ui/ingeniero/ ui/rh/ ui/analista/ ui/compartido/                   → M2
ui/notificaciones/ (bandeja, campana con contador)                             → M3
ui/inicio/ ui/captura/ ui/listado/ ui/detalle/ (taller)                        → nadie, salvo lo que exige 5.1–5.2 (M1)
```

### 6.2 Interfaces que M1 entrega primero

M2 y M3 programan contra estas interfaces. Las firmas mínimas son las siguientes; M1 puede agregar métodos, pero **no cambiar** estos sin avisar.

```kotlin
interface SesionRepository {
    val usuario: StateFlow<Usuario?>            // null = sin sesión
    suspend fun iniciarSesion(correo: String, clave: String): Result<Usuario>
    suspend fun cerrarSesion()
}
interface ProyectosRepository {
    fun proyectosDelCliente(uid: String): Flow<List<Proyecto>>
    fun proyectosDelIngeniero(uid: String): Flow<List<Proyecto>>
    fun proyecto(id: String): Flow<Proyecto?>
    fun novedadesDelProyecto(id: String): Flow<List<NovedadRemota>>
    fun personalDelProyecto(id: String): Flow<List<PersonalAsignado>>
    suspend fun marcarHito(proyectoId: String, hitoId: String, completado: Boolean): Result<Unit>
    suspend fun asignarTecnico(proyectoId: String, empleado: Empleado, asignar: Boolean): Result<Unit>
}
interface DocumentosRepository {
    fun documentos(proyectoId: String): Flow<List<Documento>>
    suspend fun subir(proyectoId: String, tipo: TipoDocumento, uri: Uri): Result<Documento> // escribe documento + archivo + notificación al cliente
}
interface ArchivosRepository {
    suspend fun descargarAArchivoLocal(archivoId: String): Result<File>   // decodifica Base64 a cacheDir
}
interface EmpleadosRepository {
    fun empleados(): Flow<List<Empleado>>
    fun empleado(id: String): Flow<Empleado?>
    suspend fun guardar(empleado: Empleado): Result<Unit>
    suspend fun subirCumplimiento(empleadoId: String, tipo: TipoCumplimiento, expedicion: LocalDate, vencimiento: LocalDate, uri: Uri): Result<Unit>
    suspend fun subirCarne(empleadoId: String, uri: Uri): Result<Unit>
}
interface CartasRepository {
    fun cartasDelProyecto(proyectoId: String): Flow<List<Carta>>
    suspend fun guardarCarta(proyectoId: String, empleados: List<Empleado>, pdf: File): Result<Carta> // + notificación al cliente
}
object NotificacionFactory {   // devuelve las rutas a incluir en una escritura multi-ruta
    fun novedadSincronizada(uidDestino: String, r: NovedadEntity): Map<String, Any?>
    fun documentoNuevo(uidDestino: String, proyectoId: String, d: Documento): Map<String, Any?>
    fun cartaNueva(uidDestino: String, proyectoId: String, c: Carta): Map<String, Any?>
}
```

- `Empleado` incluye una función calculada `estadoCumplimiento(hoy)`, que devuelve `VIGENTE`, `POR_VENCER` (15 días o menos) o `VENCIDO`, y la propiedad `habilitado`.
- **Destinos de navegación** (M1 los crea, inicialmente con pantallas de relleno): `Login`, `InicioTecnico` (la pantalla Inicio actual), `InicioCliente`, `InicioIngeniero`, `InicioRH`, `InicioAnalista`, `ProyectoDetalle(proyectoId)`, `EmpleadoDetalle(empleadoId)`, `Bandeja`, `NovedadRemota(idRegistro)`.
- **Barra superior compartida**: M1 crea `ui/compartido/BarraRol(titulo, acciones)` con un espacio para la campana. M3 entrega el composable `CampanaNotificaciones()` y M2 lo coloca en cada pantalla de inicio.

---

## 7. Notificaciones (resumen; el detalle lo implementa M3)

| Evento | Quién lo escribe | Destinatarios | Momento |
|---|---|---|---|
| Novedad sincronizada (CU-02, paso 10) | Técnico, en la escritura multi-ruta de SINCRONIZAR | Cliente e ingeniero del proyecto | Solo cuando Firebase confirma; **nunca al pulsar GUARDAR** |
| Documento nuevo (CU-05, paso 21) | Ingeniero, al subir el documento | Cliente del proyecto | Al confirmar la subida |
| Carta de presentación nueva (CU-09, paso 35) | Analista | Cliente del proyecto | Al confirmar la subida |
| Documento de cumplimiento vencido o por vencer (CU-07, flujo alterno) | **No se escribe en Firebase**: lo calcula la app de RH | RH | Al abrir la app y una vez al día (WorkManager) |

El técnico no recibe notificaciones en esta entrega.

---

## 8. Flujo de trabajo con Git

Una sola persona desarrolla los tres módulos, **uno detrás de otro**:

1. **M1 completo** (fases A a E) en la rama `modulo/datos-firebase`. Al terminar: pruebas A a E del taller e integración en `main`.
2. **M3** en la rama `modulo/notificaciones`, creada desde el `main` que ya tiene M1. Al terminar, se integra en `main`.
3. **M2** en la rama `modulo/roles`, creada desde el `main` que ya tiene M1 y M3. Al terminar, se integra en `main`.
4. **Una sesión nueva de Claude Code por módulo** (o `/clear` entre módulos), para que el contexto de uno no se mezcle con el siguiente.
5. **Git lo maneja Claude Code**, según el prompt de cada módulo: hace un commit por fase y sube la rama al repositorio remoto. Al terminar el módulo, y después de que confirmes que lo revisaste, integra la rama en `main` y sube `main`. Prefijo de los commits: `feat(m2): pantalla de documentos del cliente`.
6. Antes de integrar cada módulo en `main`: `./gradlew assembleDebug` sin errores, pruebas del módulo en verde y, si se tocó el flujo del técnico, las pruebas A a E.
7. Si un módulo necesita cambiar algo de M1 (por ejemplo, agregar un método a un repositorio), se puede hacer en ese momento, porque el dueño es la misma persona. El cambio se anota en el resumen del módulo (`docs/mX_resumen.md`) y en este documento si altera el contrato.

### 8.1 Evidencia individual (punto 18 del taller)

El taller exige que **cada integrante demuestre aportes propios con evidencia verificable**. Como los commits de código serán de Gabriel, los otros dos integrantes aportan así:

| Integrante | Responsabilidad | Evidencia que debe dejar |
|---|---|---|
| **Gabriel Rodríguez** | Desarrollo de M1, M2 y M3 con Claude Code | Commits de cada módulo; `docs/m1_resumen.md`, `docs/m2_resumen.md` y `docs/m3_resumen.md`; explicación del código en la sustentación |
| **Santiago Torres** | Diseño visual de las pantallas nuevas: inicio de sesión, pantallas de cada rol, bandeja y campana de notificaciones | Boceto en Figma **antes** de que se programe cada módulo; revisión de cada pantalla contra el sistema visual (CLAUDE.md, sección 10.1); capturas antes y después; lista de ajustes visuales que Gabriel aplica |
| **Juan Diego Rojas** | Pruebas y documentación | Ejecución y capturas de las pruebas A a E y de la prueba con dos emuladores (M3, fase 5); bitácora de errores encontrados y cómo se resolvieron; actualización de la documentación (sección 10) |

- Cuando Santiago o Juan Diego trabajen **junto con Gabriel** en una sesión (por ejemplo, revisando una pantalla o una prueba), el commit puede incluir la línea `Co-authored-by: Nombre <correo>`. **Solo si de verdad participaron.**
- Si Santiago o Juan Diego hacen cambios directos (archivos de documentación, ajustes de textos o colores en `config/Mensajes.kt` o `ui/theme/`), deben hacer **sus propios commits** desde su computador.

---

## 9. Datos de prueba (responsabilidad de M1)

- **5 cuentas** en Firebase Authentication: `tecnico@`, `cliente@`, `ingeniero@`, `rh@` y `analista@`, con un dominio de prueba.
- Archivo `firebase/seed_plantilla.json` con `usuarios`, 2 `proyectos` (con hitos), 3 `empleados` y los `indices`, usando marcadores `UID_TECNICO`, `UID_CLIENTE`, etc. El equipo reemplaza los marcadores por los uid reales y lo **importa** desde la consola de RTDB.
- Archivo `firebase/database.rules.json`, que se **publica** desde la consola.
- Guía corta `firebase/LEEME.md` con estos pasos.

---

## 10. Cambios que hay que hacer en la documentación

- **Taller, punto 11:** cambiar `setValue` por `updateChildren` multi-ruta (ver 5.3).
- **Taller, diccionario:** agregar `uidTecnico`.
- **Taller, punto 5:** el inicio de sesión requiere Internet la primera vez; después la app abre sin conexión.
- **Taller, punto 18:** actualizar las responsabilidades y evidencias según la sección 8.1.
- **Documento académico:** agregar la sección «Trabajo futuro» (sección 11).

---

## 11. Trabajo futuro (fuera de esta entrega)

- **Contabilidad (HU-14, CU-08):** registro de facturas y estado de facturación visible para el cliente (N-07).
- **Gerencia (HU-16):** panel general de proyectos y novedades.
- **Soporte (HU-17, CU-01, CU-04):**
  - que el cliente reporte la novedad (CU-01);
  - que soporte la asigne al técnico;
  - el ciclo Registrada → Asignada → Solucionada / Escalada;
  - que el técnico descargue sus novedades asignadas;
  - el informe periódico al cliente (CU-04).
- **Confidencialidad (N-11):** firma del acuerdo de confidencialidad de los técnicos.
- **Notificaciones push reales** con la app cerrada: Firebase Cloud Messaging más Cloud Functions, que requieren el plan Blaze.
- **Cloud Storage** para archivos mayores de 1,5 MB (requiere el plan Blaze).
- **Varios usuarios por empresa cliente** y reglas de lectura más finas para `archivos`.

---

# PARTE 2 — Prompts para Claude Code (terminal de Android Studio)

## Antes de empezar

1. Guarda este documento (`MODULOS.md`) en la **raíz del proyecto**, junto a `CLAUDE.md`, y súbelo a `main`.
2. **Orden obligatorio:** M1 → M3 → M2. No empieces un módulo hasta que el anterior esté integrado en `main` (parte 1, sección 8).
3. Para cada módulo, abre **una sesión nueva** de Claude Code en la raíz del proyecto (o escribe `/clear`) y pega el prompt completo (el texto dentro del bloque de código).
4. Antes de M2 y M3, pide a Santiago el boceto de esas pantallas, para que Claude lo tenga como referencia.
5. Al terminar cada módulo, pasa a Juan Diego lo necesario para ejecutar y documentar las pruebas (sección 8.1).

---

## Prompt M1 – Datos y Firebase

```
Lee completos CLAUDE.md y MODULOS.md. Si se contradicen, manda MODULOS.md.
Eres responsable del módulo M1 – Datos y Firebase (MODULOS.md, secciones 1, 4, 5, 6 y 9).

Contexto clave:
- Solo usamos Firebase Authentication (correo y contraseña) y Realtime Database, en el plan gratuito Spark. No uses Cloud Storage, Cloud Functions, Firestore ni FCM (sección 2).
- La regla número uno (sección 0) es no romper el flujo offline del técnico que exige el taller.

Trabaja en la rama modulo/datos-firebase, creada desde main.
Trabaja por fases. Al terminar cada fase:
1. compila con ./gradlew assembleDebug y corrige los errores;
2. resume qué hiciste y en qué archivos;
3. haz el commit de la fase en la rama, con prefijo feat(m1): / fix(m1):, y súbela al repositorio remoto (git push de la rama);
4. DETENTE y espera mi confirmación.

FASE A – Contrato (la base que usan M2 y M3)
- Dependencias: Firebase Auth (con el mismo BoM que ya usa el proyecto) y kotlinx-coroutines-play-services, si no está.
- Crea config/Roles.kt, config/RutasFirebase.kt (todas las rutas de la sección 4 como funciones) y los modelos de data/model/.
- Crea las interfaces de la sección 6.2, con implementaciones vacías o de relleno que compilen.
- Crea NotificacionFactory con las tres funciones. Los ID deben ser deterministas.
- Crea los destinos de navegación de la sección 6.2, con pantallas de relleno que solo muestren el nombre del rol, y el composable compartido BarraRol con un espacio para acciones.
- Crea la pantalla de inicio de sesión y SesionRepository real:
  - iniciar sesión, leer usuarios/{uid}, guardar la sesión en caché con DataStore;
  - al abrir la app, si currentUser no es null y hay caché, entrar sin Internet al inicio del rol;
  - cerrar sesión con la advertencia de pendientes (5.1);
  - mensajes claros para correo o contraseña incorrectos, sin red y usuario sin rol.
- Retira la pantalla de perfil del técnico: el nombre del técnico sale de la sesión.
Al terminar, haz el commit de esta fase como en las demás.

FASE B – Datos de prueba y reglas
- firebase/seed_plantilla.json, firebase/database.rules.json y firebase/LEEME.md (sección 9).
- Las reglas deben cumplir la sección 4.1, incluida la vista reducida proyectos/{pid}/personal.
- Revisa que las reglas permitan la escritura multi-ruta del técnico (5.3) y que rechacen:
  - que un técnico escriba una novedad con un uidTecnico distinto al suyo;
  - que un cliente lea un proyecto ajeno;
  - que un usuario cambie su propio rol.
- Escribe en LEEME.md cómo probar esas reglas con el simulador de reglas de la consola.

FASE C – Repositorios reales
- FirebaseFlows.kt: un callbackFlow genérico sobre ValueEventListener que quita el listener al cerrar el flujo.
- Implementa ProyectosRepository, DocumentosRepository, ArchivosRepository, EmpleadosRepository y CartasRepository contra RTDB.
- Archivos:
  - solo PDF, JPG o PNG, con un máximo de 1,5 MB (en config/Limites.kt);
  - se validan el tipo MIME y el tamaño antes de subir;
  - Base64 en archivos/{id} y metadatos aparte;
  - descargarAArchivoLocal decodifica a cacheDir.
- Las subidas que generan una notificación (documento y carta) y la asignación de técnicos (que actualiza la vista reducida de personal y los índices) se hacen con UNA escritura updateChildren multi-ruta.
- Empleado.estadoCumplimiento(hoy) y habilitado se calculan, no se guardan.
- Pruebas unitarias de estadoCumplimiento y de NotificacionFactory.

FASE D – Ajustes al flujo del técnico (lo más delicado)
- Migración de Room (5.4), sin destructive migration. Pruébala con una base de datos de la versión anterior que tenga registros PENDIENTES.
- Proyectos del técnico desde indices/proyectosPorTecnico, guardados en Room (5.2). Elimina assets/proyectos.json.
- En SyncRepository cambia SOLO la escritura de cada registro por la escritura multi-ruta de la sección 5.3 (novedad + índice + notificaciones al cliente y al ingeniero). No cambies nada más del algoritmo:
  - verificación de conexión;
  - selección de PENDIENTE y ERROR;
  - envío uno por uno;
  - tiempo límite de 15 s;
  - marcar SINCRONIZADO solo con confirmación;
  - continuar ante errores;
  - resumen final;
  - simulador de fallo en debug.
- Después guíame paso a paso para repetir en el emulador las pruebas A, B, C, D y E del taller. En C y D debo verificar en la consola que la novedad, el índice y las dos notificaciones existen una sola vez.

FASE E – Cierre
- Resume en un archivo docs/m1_resumen.md:
  - qué se construyó;
  - las decisiones tomadas;
  - el problema técnico principal que resolvió M1 (para la columna «Problema técnico resuelto» del punto 18);
  - los cambios que hay que llevar a la documentación (MODULOS.md, parte 1, sección 10).

AL TERMINAR EL MÓDULO – Subir a Git
- Primero muéstrame un resumen de todo lo hecho en el módulo y las pruebas que debo revisar. Espera a que te confirme que ya las revisé.
- Con mi confirmación:
  1. verifica que no quedan cambios sin commit en modulo/datos-firebase;
  2. actualiza main desde el repositorio remoto (git pull);
  3. integra modulo/datos-firebase en main con git merge --no-ff;
  4. compila de nuevo en main con ./gradlew assembleDebug;
  5. sube main al repositorio remoto (git push).
- Si hay conflictos al integrar, NO los resuelvas por tu cuenta: muéstramelos, explícame las opciones y espera mi decisión.
- Si el proyecto no tiene repositorio remoto configurado, avísame y no hagas push.
- Nunca uses git push --force ni git reset --hard.
- Al final, recuérdame escribir /clear y pegar el prompt del siguiente módulo (M3).

No ejecutes comandos destructivos de Git ni borres archivos fuera de lo indicado sin preguntarme.
```

---

## Prompt M2 – Funciones por rol

```
Lee completos CLAUDE.md y MODULOS.md. Si se contradicen, manda MODULOS.md.
Eres responsable del módulo M2 – Funciones por rol: cliente, ingeniero de proyectos, HCQ y RH y analista administrativo (MODULOS.md, secciones 1, 3 y 6).
Contabilidad, gerencia y soporte son trabajo futuro: NO los implementes.

Condiciones:
- M1 y M3 ya están integrados en main. Crea la rama modulo/roles desde main.
- Solo trabaja en los paquetes que te asigna la sección 6.1: ui/cliente, ui/ingeniero, ui/rh, ui/analista y ui/compartido (excepto BarraRol).
- Usa las interfaces de la sección 6.2. Si te falta un método, puedes agregarlo al repositorio de M1, pero PRIMERO dime cuál y por qué, y espera mi confirmación. No cambies las firmas existentes. Anota el cambio en docs/m2_resumen.md.
- No toques el flujo del técnico (ui/inicio, ui/captura, ui/listado, ui/detalle, SyncRepository).
- Estas pantallas funcionan en línea. Sin conexión muestran el mensaje de la sección 3 y un botón «Reintentar». Nunca deben cerrarse de golpe.
- Respeta el sistema visual de CLAUDE.md, sección 10.1: colores, Plus Jakarta Sans, tarjetas e íconos Outlined.
- Arquitectura MVVM: un ViewModel por pantalla, con StateFlow y estados de carga, vacío, error y contenido.

Trabaja por fases. Al terminar cada fase:
1. compila con ./gradlew assembleDebug;
2. resume qué hiciste;
3. haz el commit de la fase en la rama, con prefijo feat(m2): / fix(m2):, y súbela al repositorio remoto (git push de la rama);
4. DETENTE y espera mi confirmación.

FASE 1 – Cliente (CU-03, CU-06; HU-02, 03, 06–10)
- InicioCliente: «Mis proyectos», con una tarjeta por proyecto que muestre nombre, estado y barra de avance.
- ProyectoDetalle, versión cliente, con pestañas:
  - Avance: porcentaje y cronograma de hitos (completado / pendiente, con fechas).
  - Documentos: agrupados por tipo. Al tocar uno se descarga y se abre con un Intent ACTION_VIEW mediante FileProvider. Mientras descarga se muestra el progreso, y si falla, el error.
  - Personal asignado: nombre, cédula y estado de cumplimiento con un chip de color e ícono. Usa la vista reducida que entrega personalDelProyecto (HU-07).
  - Novedades: las novedades sincronizadas del proyecto, filtrables por estado de la atención y fecha. Al tocar una se abre NovedadRemota con los datos, las fotos (miniatura Base64) y las firmas.
  - Cartas de presentación: lista y descarga.

FASE 2 – Ingeniero de proyectos (CU-05; HU-11, HU-12)
- InicioIngeniero: los proyectos a su cargo.
- ProyectoDetalle, versión ingeniero: las mismas pestañas del cliente, más estas acciones:
  - Cargar documento:
    - selector de tipo (TipoDocumento) y de archivo (OpenDocument con PDF, JPG y PNG);
    - si el formato no se admite o el archivo pasa de 1,5 MB, rechazarlo con un mensaje (flujo alterno de CU-05);
    - pedir confirmación antes de subir;
    - al terminar, mostrar «Documento publicado. El cliente fue notificado».
  - Cronograma: marcar o desmarcar hitos. El avance se recalcula.
  - Técnicos:
    - asignar o quitar empleados;
    - SOLO se pueden asignar los que están habilitados;
    - los no habilitados se muestran deshabilitados con el motivo («ARL vencida el 12/10/2026»), según el flujo alterno de CU-07.

FASE 3 – HCQ y RH (CU-07; HU-15)
- InicioRH: lista de empleados con buscador y un chip de estado (Vigente / Por vencer / Vencido), ordenada con los vencidos primero.
- Crear y editar empleado:
  - nombre;
  - cédula: solo dígitos, entre 6 y 10, igual que la regla R3 del taller (reutiliza esa validación si existe);
  - cargo;
  - uid de técnico, opcional.
- EmpleadoDetalle: para ARL, seguridad social y certificado de alturas, permite cargar el archivo con su fecha de expedición y su fecha de vencimiento.
  - Valida que el vencimiento sea posterior a la expedición y que la expedición no sea futura.
  - Muestra el estado calculado y si el empleado está habilitado para obra.

FASE 4 – Analista administrativo (CU-09; HU-13)
- InicioAnalista: empleados y proyectos.
- Cargar el carné de un empleado (imagen o PDF).
- Generar una carta de presentación:
  - elegir el proyecto y los técnicos asignados a él que estén habilitados;
  - generar el PDF en el dispositivo con android.graphics.pdf.PdfDocument: encabezado «NETTSEGUR TECNOLOGÍA & SEGURIDAD ELECTRÓNICA S.A.S.», fecha, cliente, proyecto, una tabla con nombre y cédula de cada técnico, y el nombre del analista que la emite;
  - mostrar una vista previa y luego guardarla con CartasRepository.guardarCarta, que también notifica al cliente.
- Si un técnico no está habilitado, no se puede incluir.

FASE 5 – Integración
- Coloca CampanaNotificaciones() (ya creada por M3) en la BarraRol de las cuatro pantallas de inicio.
- Revisa la navegación completa de cada rol con las cuentas de prueba de firebase/LEEME.md.
- Comprueba que el botón Atrás nunca devuelve al inicio de sesión estando dentro de la app.
- Prueba en el emulador, con capturas, que un cliente no ve proyectos ajenos.
- Con dos emuladores (ingeniero y cliente), prueba que al subir un documento o una carta de presentación el cliente recibe el aviso una sola vez. Es la prueba que M3 dejó pendiente.
- Escribe docs/m2_resumen.md: qué construiste, decisiones y el problema técnico principal que resolviste (para el punto 18).

AL TERMINAR EL MÓDULO – Subir a Git
- Primero muéstrame un resumen de todo lo hecho en el módulo y las pruebas que debo revisar. Espera a que te confirme que ya las revisé.
- Con mi confirmación:
  1. verifica que no quedan cambios sin commit en modulo/roles;
  2. actualiza main desde el repositorio remoto (git pull);
  3. integra modulo/roles en main con git merge --no-ff;
  4. compila de nuevo en main con ./gradlew assembleDebug;
  5. sube main al repositorio remoto (git push).
- Si hay conflictos al integrar, NO los resuelvas por tu cuenta: muéstramelos, explícame las opciones y espera mi decisión.
- Si el proyecto no tiene repositorio remoto configurado, avísame y no hagas push.
- Nunca uses git push --force ni git reset --hard.
- Al final, avísame que los tres módulos quedaron integrados y recuérdame pedirle a Juan Diego que actualice la documentación (MODULOS.md, parte 1, sección 10).
```

---

## Prompt M3 – Notificaciones

```
Lee completos CLAUDE.md y MODULOS.md. Si se contradicen, manda MODULOS.md.
Eres responsable del módulo M3 – Notificaciones (MODULOS.md, secciones 2, 4, 6 y 7).

Contexto clave:
- Usamos el plan gratuito de Firebase: NO hay Cloud Functions ni push de servidor (sección 2).
- Quien genera el evento escribe la notificación en RTDB, en notificaciones/{uid}/{id}. Esa parte ya la resuelve M1 con NotificacionFactory y las escrituras multi-ruta.
- Tu trabajo es la RECEPCIÓN: bandeja, contador, notificaciones del sistema Android y alertas de vencimiento para RH.

Condiciones:
- M1 ya está integrado en main. Crea la rama modulo/notificaciones desde main. M2 (pantallas de roles) todavía NO existe: prueba la campana y la bandeja desde las pantallas de relleno de cada rol que dejó M1.
- Solo trabaja en el paquete notificaciones/ (excepto la creación inicial de NotificacionFactory, que ya hizo M1; puedes mejorarla sin cambiar sus firmas), en ui/notificaciones/ y en NotificacionesRepository.
- No toques SyncRepository ni el flujo del técnico. Si algo de la escritura no te sirve, dime qué cambiarías y por qué, y espera mi confirmación antes de modificar código de M1.
- Los destinatarios de esta entrega son cliente, ingeniero y RH. El técnico no recibe notificaciones.

Trabaja por fases. Al terminar cada fase:
1. compila con ./gradlew assembleDebug;
2. resume qué hiciste;
3. haz el commit de la fase en la rama, con prefijo feat(m3): / fix(m3):, y súbela al repositorio remoto (git push de la rama);
4. DETENTE y espera mi confirmación.

FASE 1 – Repositorio y bandeja
- NotificacionesRepository:
  - observar(uid): Flow<List<Notificacion>>, ordenadas por fecha y limitadas a las últimas 50;
  - noLeidas(uid): Flow<Int>;
  - marcarLeida(uid, id) y marcarTodasLeidas(uid). Solo pueden escribir el campo leida.
- Pantalla Bandeja:
  - lista agrupada por Hoy / Esta semana / Anteriores, con ícono por tipo y diferencia visual entre leídas y no leídas;
  - acción «Marcar todas como leídas» y estado vacío;
  - al tocar una notificación se marca como leída y se navega al destino correspondiente: NOVEDAD_SINCRONIZADA → NovedadRemota(refId); DOCUMENTO_NUEVO y CARTA_NUEVA → ProyectoDetalle(proyectoId), en la pestaña que corresponda.
- Composable CampanaNotificaciones(): ícono Outlined con contador de no leídas (máximo «9+»). Al tocarlo abre la Bandeja.

FASE 2 – Notificaciones del sistema Android
- Canal «Actualizaciones de proyectos», creado en Application.
- Permiso POST_NOTIFICATIONS en Android 13 o superior:
  - se pide la primera vez que el cliente o el ingeniero entra a su inicio, con una explicación previa en un diálogo propio;
  - si lo niega, la bandeja sigue funcionando y se muestra un aviso discreto.
- Con la app abierta:
  - un listener sobre notificaciones/{uid}, ligado al ciclo de vida del proceso (ProcessLifecycleOwner);
  - si llega una no leída nueva con la app en primer plano, solo se actualiza el contador y se muestra un snackbar;
  - si la app está en segundo plano, se publica una notificación del sistema con NotificationCompat y un PendingIntent que abre el destino.
- Registro de «ya mostradas»: guarda en DataStore los ID ya mostrados para no repetirlos. Se limpia al cerrar sesión.

FASE 3 – App cerrada (WorkManager periódico)
- Un PeriodicWorkRequest de 15 minutos (el mínimo de Android), con la restricción NetworkType.CONNECTED. Se programa al iniciar sesión cliente o ingeniero y se cancela al cerrar sesión. Usa enqueueUniquePeriodicWork con la política KEEP.
- El worker:
  - verifica que FirebaseAuth.currentUser exista;
  - lee las no leídas que no se hayan mostrado y publica las notificaciones del sistema;
  - si hay más de 3, publica una sola notificación resumida («Tienes N actualizaciones nuevas»).
- Deja un comentario en el código y una nota en docs/m3_resumen.md explicando que, sin un servidor (Cloud Functions + FCM, que requieren el plan Blaze), la notificación con la app cerrada puede llegar con 15 minutos de retraso o más. Es una limitación conocida y queda como trabajo futuro.
- IMPORTANTE: este worker solo LEE notificaciones. No sincroniza novedades ni escribe nada, salvo el registro local de «ya mostradas». El taller prohíbe la sincronización automática de novedades.

FASE 4 – Alertas de vencimiento para RH (flujo alterno de CU-07)
- Al entrar RH a su inicio, y con un worker diario (con red, solo si el rol es RH):
  - revisa los documentos de cumplimiento VENCIDOS o POR_VENCER (15 días o menos) usando Empleado.estadoCumplimiento;
  - publica notificaciones locales con un ID determinista por empleado, tipo de documento y fecha de vencimiento, para no repetir la misma alerta el mismo día.
- Estas alertas NO se escriben en Firebase.
- En el inicio de RH, tarjeta resumen: «N documentos vencidos · M por vencer».

FASE 5 – Pruebas
- Pruebas unitarias de la agrupación por fecha, del conteo de no leídas y de la lógica de «ya mostradas».
- Guíame paso a paso para una prueba con DOS emuladores (técnico y cliente):
  1. el técnico guarda un registro sin conexión y verifica que el cliente NO recibe nada;
  2. el técnico sincroniza y verifica que el cliente y el ingeniero reciben el aviso;
  3. el técnico pulsa SINCRONIZAR otra vez y verifica que no llega un aviso duplicado;
  4. la prueba «el ingeniero sube un documento y el cliente recibe el aviso» queda para M2, porque la pantalla del ingeniero todavía no existe;
  5. con la app del cliente cerrada, el aviso llega dentro del ciclo del worker.
  Indica qué captura tomar en cada paso.
- Escribe docs/m3_resumen.md: qué construiste, decisiones, limitaciones y el problema técnico principal que resolviste (para el punto 18).

AL TERMINAR EL MÓDULO – Subir a Git
- Primero muéstrame un resumen de todo lo hecho en el módulo y las pruebas que debo revisar. Espera a que te confirme que ya las revisé.
- Con mi confirmación:
  1. verifica que no quedan cambios sin commit en modulo/notificaciones;
  2. actualiza main desde el repositorio remoto (git pull);
  3. integra modulo/notificaciones en main con git merge --no-ff;
  4. compila de nuevo en main con ./gradlew assembleDebug;
  5. sube main al repositorio remoto (git push).
- Si hay conflictos al integrar, NO los resuelvas por tu cuenta: muéstramelos, explícame las opciones y espera mi decisión.
- Si el proyecto no tiene repositorio remoto configurado, avísame y no hagas push.
- Nunca uses git push --force ni git reset --hard.
- Al final, recuérdame escribir /clear y pegar el prompt del siguiente módulo (M2).
```
