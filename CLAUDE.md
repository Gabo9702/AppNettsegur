# AppNettsegur — Especificación de desarrollo

> Archivo de contexto para el agente de código. Guárdalo en la **raíz del proyecto de Android Studio** con el nombre `CLAUDE.md`.
> Proyecto académico: Universidad de La Sabana · Desarrollo móvil 2026-2 · Taller "Captura Offline-First y sincronización manual con Firebase RTDB".

---

## 1. Tu rol y forma de trabajar

Eres un desarrollador Android senior que acompaña a un equipo de estudiantes. El equipo debe **sustentar y modificar en vivo** este código, así que:

- Trabaja **por fases** (sección 12). Al terminar cada fase:
  1. compila con `./gradlew assembleDebug` y corrige los errores;
  2. resume en 5–10 líneas qué hiciste y en qué archivos;
  3. propón un mensaje de commit (formato `feat:`, `fix:`, `docs:`…);
  4. **detente y espera mi confirmación** antes de seguir.
- Código claro antes que ingenioso. Sin librerías innecesarias.
- Comentarios en español **solo** en la lógica clave: validaciones, manejo de imagen, guardado local y sincronización.
- Si algo de esta especificación es ambiguo o contradictorio, **pregunta antes de implementar**. No inventes requisitos.
- No ejecutes comandos destructivos (borrar carpetas, `git reset --hard`, `git push --force`) sin preguntarme.
- Las constantes que el docente puede pedir cambiar en vivo (reglas, mensajes, listas, límites) van centralizadas en archivos fáciles de encontrar (ver sección 11).

---

## 2. Contexto del caso

Nettsegur es una empresa de seguridad electrónica (cámaras, controles de acceso, cercados eléctricos, alarmas). Sus clientes son entidades del Estado, residencias, bodegas, oficinas y centros comerciales.

La app la usa el **técnico en campo** para registrar la **constancia de un servicio técnico** (basada en el formato físico "SERVICIO TÉCNICO – NETTSEGUR TECNOLOGÍA & SEGURIDAD ELECTRÓNICA S.A.S."). Escenario típico sin conexión: una finca con señal baja donde el técnico hace cableado y conexiones. La app debe permitir registrar todo sin Internet y enviar los registros a Firebase **solo cuando el técnico pulse SINCRONIZAR**.

---

## 3. Reglas NO negociables del taller

1. La app abre y permite capturar **sin Internet**, desde el inicio y durante toda la captura.
2. **GUARDAR solo escribe en Room.** Nunca toca Firebase.
3. Firebase RTDB solo se usa cuando el usuario pulsa **SINCRONIZAR**. **No hay sincronización automática** (ni WorkManager, ni reintentos en segundo plano, ni envío al reconectar).
4. Los registros y sus imágenes sobreviven a cerrar y reabrir la app.
5. Estados: `PENDIENTE`, `SINCRONIZADO` y `ERROR`.
6. Una segunda sincronización **no duplica** registros en RTDB.
7. Cada registro tiene **mínimo 15 datos**, **mínimo 8 diligenciados por el usuario** y **mínimo 1 imagen**.
8. La interfaz siempre refleja el estado de los registros y los errores.

---

## 4. Stack técnico

- Kotlin, Android Studio, **Jetpack Compose + Material 3**. Una sola Activity y Navigation Compose.
- Arquitectura: **MVVM** con capas `ui` → `ViewModel` (StateFlow) → `Repository` → `Room` / `Firebase`.
- **Room** con KSP; consultas que devuelven `Flow` para que la UI se actualice sola.
- **DataStore Preferences** para el perfil del técnico (nombre).
- **Coroutines**, incluido `kotlinx-coroutines-play-services` para hacer `await()` sobre las tareas de Firebase.
- **Firebase Realtime Database** usando el BoM de Firebase y el plugin `google-services`.
- **Coil** para mostrar imágenes desde archivos locales.
- Inyección de dependencias manual con un `AppContainer` en la clase `Application`. **No usar Hilt**, para mantenerlo simple.
- Usa las versiones compatibles con el AGP y el Kotlin que ya tiene el proyecto, declaradas en `gradle/libs.versions.toml`.
- `minSdk` 26.

---

## 5. Modelo de datos

### 5.1 Tabla `novedades` (entidad `NovedadEntity`)

| # | Campo | Tipo | Origen | Obligatorio | Notas |
|---|---|---|---|---|---|
| 1 | `idRegistro` | String (UUID v4) | Automático | Sí | PK en Room **y** llave en Firebase. No editable. |
| 2 | `proyectoId` | String | Usuario (selección) | Sí | Seleccionado del catálogo local de proyectos. |
| 3 | `nombreCliente` | String | Automático | Sí | Se llena a partir del proyecto seleccionado. |
| 4 | `nombreTecnico` | String | Automático | Sí | Se toma del perfil del técnico (DataStore). No editable. |
| 5 | `fechaServicio` | Long (epoch ms) | Automático | Sí | Fecha de creación del registro. |
| 6 | `horaInicio` | String `HH:mm` | Usuario | Sí | Selector de hora. |
| 7 | `horaFin` | String `HH:mm` | Automático | Sí | Hora del primer GUARDAR. |
| 8 | `tipoServicio` | String | Usuario (lista) | Sí | Lista en `Catalogos.kt`. |
| 9 | `tipoActividad` | String | Usuario (lista) | Sí | Lista en `Catalogos.kt`. |
| 10 | `serialEquipo` | String | Usuario | Sí | Regla R4. Se guarda en mayúsculas. |
| 11 | `descServAsignado` | String | Usuario | Sí | Lo que se pidió hacer (diagnóstico o asignación). |
| 12 | `estadoAtencion` | String (`RESUELTA` / `NO_RESUELTA`) | Usuario | Sí | Regla R1. |
| 13 | `descServRealizado` | String | Usuario | Sí | Reglas R1 y R5. |
| 14 | `motivoNoResuelta` | String? | Usuario | Condicional | Obligatorio si `NO_RESUELTA` (R1). |
| 15 | `observacionesRecomendaciones` | String? | Usuario | No | Opcional (ver nota de coherencia en sección 13). |
| 16 | `nombreRepresentanteCliente` | String | Usuario | Sí | |
| 17 | `cedulaRepresentanteCliente` | String | Usuario | Sí | Regla R3. |
| 18 | `rolRepresentanteCliente` | String | Usuario | Sí | Ej.: administrador, supervisor, portero. |
| 19 | `rutaFirmaTecnico` | String | Usuario (firma) | Sí | PNG en almacenamiento interno. |
| 20 | `rutaFirmaRepresentante` | String | Usuario (firma) | Sí | PNG en almacenamiento interno. |
| 21 | `estadoSync` | enum `EstadoSync` | Automático | Sí | `PENDIENTE` / `SINCRONIZADO` / `ERROR`. **No es Boolean.** |
| 22 | `fechaCreacion` | Long | Automático | Sí | |
| 23 | `fechaModificacion` | Long | Automático | Sí | |
| 24 | `fechaSincronizacion` | Long? | Automático | No | Se llena cuando Firebase confirma. |
| 25 | `mensajeError` | String? | Automático | No | Motivo del último fallo de envío. |

### 5.2 Tabla `evidencias` (entidad `EvidenciaEntity`)

Contiene la evidencia fotográfica: **mínimo 1 y máximo 3** fotos por registro.

Campos: `idEvidencia` (UUID), `idRegistro` (FK con borrado en cascada), `rutaArchivo`, `tamanoBytes`, `hashSha256`, `ancho`, `alto`, `fechaCaptura`.

### 5.3 Catálogo local de proyectos

Un archivo `assets/proyectos.json` con 5–6 proyectos de ejemplo (`proyectoId`, `nombreProyecto`, `nombreCliente`). Se carga en Room al primer inicio. Así la selección funciona sin Internet.

### 5.4 Catálogos (`Catalogos.kt`)

Son valores iniciales que el equipo puede ajustar al formato real de Nettsegur.

- `tipoServicio`: Instalación, Mantenimiento preventivo, Mantenimiento correctivo, Garantía, Visita técnica.
- `tipoActividad`: CCTV / cámaras, Control de acceso, Cercado eléctrico, Alarmas y sensores, Cableado y redes.

---

## 6. Reglas de validación

Las reglas corren en el **ViewModel al pulsar GUARDAR**. Todas se definen en `validacion/ReglasValidacion.kt`, cada una con su código (R1…R6) y su mensaje en `Mensajes.kt`.

Si una regla falla:
- no se escribe nada en Room;
- el campo se marca en error, con el mensaje debajo;
- se hace scroll al primer campo con error;
- **no se borran** los datos digitados.

| Regla | Condición | Mensaje |
|---|---|---|
| R1 | Si `estadoAtencion = NO_RESUELTA`, `motivoNoResuelta` es obligatorio y tiene ≥ 20 caracteres (sin contar espacios de los extremos). | «Indique por qué la novedad no quedó resuelta (mínimo 20 caracteres).» |
| R2 | `horaInicio` debe ser anterior a `horaFin` (hora actual al guardar). La fecha del servicio no puede ser futura. | «La hora de inicio debe ser anterior a la hora de finalización.» / «La fecha del servicio no puede ser posterior a hoy.» |
| R3 | `cedulaRepresentanteCliente`: solo dígitos, entre 6 y 10. | «La cédula debe contener solo números (entre 6 y 10 dígitos).» |
| R4 | `serialEquipo`: regex `^[A-Za-z0-9]{4,20}$`. Se guarda en mayúsculas. | «Serial inválido: use solo letras y números, sin espacios (4 a 20 caracteres).» |
| R5 | Si `estadoAtencion = RESUELTA`, `descServRealizado` tiene ≥ 30 caracteres y es distinto de `descServAsignado` (comparación ignorando mayúsculas y espacios). | «Describa la solución aplicada (mínimo 30 caracteres y diferente del servicio asignado).» |
| R6 | Hay al menos 1 evidencia, y cada archivo existe y pesa más de 0 bytes. Además, ambas firmas existen y no están vacías. | «Agregue una foto de evidencia antes de guardar.» / «La foto ya no está disponible; tómela de nuevo.» / «Falta la firma del técnico/representante.» |

Además de R1–R6, los campos obligatorios de la sección 5 se validan como requeridos.

---

## 7. Imágenes y firmas

- **Cámara**: `ActivityResultContracts.TakePicture` con un `FileProvider` que apunta a un archivo temporal en `cacheDir`.
- **Galería**: `PickVisualMedia`, sin pedir permisos de almacenamiento.
- **Al capturar o seleccionar**, se lee la imagen, se corrige la orientación EXIF, se redimensiona (lado mayor ≤ 1600 px), se comprime a JPEG calidad 80 y se guarda una **copia propia** en `filesDir/evidencias/tmp/`. Se calculan el tamaño y el SHA-256. **Nunca** se guarda la URI original: puede perder el permiso al cerrar la app.
- **Al GUARDAR**, los archivos temporales se mueven a `filesDir/evidencias/{idRegistro}_{n}.jpg` y la ruta se guarda en Room. Si el usuario cancela el formulario, se borran los temporales.
- **Firmas**: un pad de firma propio en Compose (`Canvas` con trazos), con botón "Limpiar". Se exporta a PNG en `filesDir/firmas/{idRegistro}_tecnico.png` y `..._representante.png`. Una firma sin trazos se considera vacía.
- **Para enviar a Firebase**, cada foto se reduce a una miniatura de lado mayor ≤ 640 px, JPEG calidad 60, en **Base64**. Las firmas se reducen a ≤ 400 px. Las imágenes originales **se quedan en el dispositivo**.
- Todo el trabajo de imagen corre en `Dispatchers.IO`.

---

## 8. Estados y edición

- `PENDIENTE`: se puede editar todo, excepto `idRegistro`, `nombreTecnico`, `fechaServicio`, `fechaCreacion` y `horaFin`. Se pueden agregar, quitar o reemplazar fotos y rehacer firmas. Las reglas se validan de nuevo al guardar los cambios.
- `ERROR`: se trata igual que `PENDIENTE`. Además, muestra el `mensajeError` y la opción "Reintentar" (que lanza la sincronización completa).
- `SINCRONIZADO`: **solo lectura**. El botón Editar no aparece. Si se intenta editar, se muestra: «Este registro ya fue enviado y no puede modificarse. Si hay un cambio, registre una nueva novedad.»
- Etiqueta de estado = **color + ícono + texto**: PENDIENTE (reloj, `#F5BF3B`), SINCRONIZADO (check, `#78C654`), ERROR (alerta, rojo de error de Material).

---

## 9. Sincronización manual

### 9.1 Estructura en Firebase RTDB

```
novedades/
  {idRegistro}/
    idRegistro, proyectoId, nombreCliente, nombreTecnico, fechaServicio,
    horaInicio, horaFin, tipoServicio, tipoActividad, serialEquipo,
    descServAsignado, estadoAtencion, descServRealizado, motivoNoResuelta,
    observacionesRecomendaciones, nombreRepresentanteCliente,
    cedulaRepresentanteCliente, rolRepresentanteCliente, fechaCreacion,
    evidencias/ { "0": {base64, ancho, alto, hashSha256}, "1": {...} }
    firmas/ { tecnico: base64, representante: base64 }
    fechaSincronizacionServidor: ServerValue.TIMESTAMP
```

- Escribir **siempre** con `ref.child("novedades").child(idRegistro).setValue(mapa)`. **Nunca `push()`.** Así, un reintento sobrescribe el mismo nodo y no se crean duplicados.
- **No** activar `setPersistenceEnabled(true)`.

### 9.2 Algoritmo (`SyncRepository` + `SyncViewModel`)

```
sincronizar():
  si sincronizando → ignorar                       // botón deshabilitado + bandera
  sincronizando = true
  si !conectividad.hayInternetValidado() → mensaje "Sin conexión. Tus registros siguen pendientes." ; fin
  lista = dao.obtenerNoSincronizados()             // estadoSync IN (PENDIENTE, ERROR) ORDER BY fechaCreacion
  si vacía → mensaje "No hay registros por sincronizar." ; fin
  para cada r en lista (UNO POR UNO):
     try:
        validar que existan sus archivos (si falta alguno → ERROR "Falta evidencia fotográfica", continuar)
        mapa = r.aMapaFirebase(miniaturasBase64, firmasBase64)
        withTimeout(15_000) { nodo(r.idRegistro).setValue(mapa).await() }
        dao.marcarSincronizado(r.idRegistro, ahora)    // solo tras la confirmación
     catch (e):
        dao.marcarError(r.idRegistro, mensajeLegible(e))   // timeout, permisos, red…
        continuar con el siguiente
  mostrar resumen "X sincronizados · Y con error"
  sincronizando = false
```

- Conectividad: `ConnectivityManager` con `NET_CAPABILITY_INTERNET` y `NET_CAPABILITY_VALIDATED`. Un `NetworkCallback` expone un `StateFlow<Boolean>` para el banner.
- Pon un comentario en el código explicando que el SDK de RTDB **encola** las escrituras sin conexión y no devuelve error. Por eso se verifica la conexión antes de enviar y se usa un timeout. Si una escritura llega tarde, cae sobre la misma llave y no duplica.
- **Solo en builds debug**: un interruptor "Simular fallo en el registro N.º __" en un menú de desarrollador, para la Prueba E. Lanza una excepción controlada en esa posición.

---

## 10. Interfaz

### 10.1 Sistema visual (Material 3, solo tema claro)

| Elemento | Valor |
|---|---|
| Primario | `#1F3C80` |
| Secundario | `#37A4CF` |
| Fondo / neutro claro | `#F0EFEB` |
| Texto / neutro oscuro | `#29282D` |
| Pendiente | `#F5BF3B` |
| Sincronizado | `#78C654` |
| Tipografía | **Plus Jakarta Sans**, empaquetada en `res/font/` (regular, medium, semibold, bold). **No usar fuentes descargables**: requieren Internet. Si los archivos `.ttf` no están, pídemelos. |
| Tarjetas | Bordes ligeramente redondeados (12 dp), borde fino de color, sombra mínima. |
| Íconos | Material Icons **Outlined** (estilo lineal, mismo grosor en toda la app). |

### 10.2 Pantallas

1. **Perfil del técnico (primer inicio)**: pide el nombre del técnico y lo guarda en DataStore. Después no vuelve a aparecer; se puede editar desde Inicio.
2. **Inicio / Resumen**:
   - banner de conexión;
   - contadores de locales, pendientes, sincronizados y con error;
   - botones Nuevo registro, Ver registros y **SINCRONIZAR**, con progreso durante el envío;
   - en debug, acceso al menú de desarrollador.
3. **Captura**: formulario agrupado en tarjetas por significado:
   - **Proyecto y servicio**: proyecto, cliente (solo lectura), tipo de servicio, tipo de actividad y serial;
   - **Atención**: hora de inicio, servicio asignado, estado de la atención, servicio realizado y motivo (solo aparece si es «No resuelta»);
   - **Observaciones**;
   - **Evidencia fotográfica**: miniaturas, cámara, galería y quitar;
   - **Representante del cliente**: nombre, cédula y rol;
   - **Firmas**: técnico y representante.

   Al final, el botón **GUARDAR EN EL DISPOSITIVO**. Tras guardar, se muestra el snackbar «Guardado en el dispositivo» y se vuelve al listado.
4. **Listado local**:
   - una tarjeta por registro con cliente, tipo de servicio, fecha, los primeros 8 caracteres del `idRegistro` y la etiqueta de estado;
   - filtro por estado con chips;
   - un `LazyColumn` alimentado por `Flow`.
5. **Detalle**:
   - muestra todos los datos, fotos ampliables, firmas, estado, fecha de la última sincronización y error, si lo hay;
   - Editar solo si el registro no está sincronizado;
   - **Eliminar** solo si está `PENDIENTE` o `ERROR`, con un diálogo de confirmación.

### 10.3 Mensajes (centralizados en `Mensajes.kt`)

- Banner sin conexión: «Sin conexión: tus registros se guardan en el dispositivo. Pulsa SINCRONIZAR cuando tengas Internet.»
- Banner con conexión: «Conectado. Tienes N registros pendientes por sincronizar.»
- Mensajes breves y orientados a la acción. Las acciones sensibles (eliminar, salir con cambios sin guardar) piden confirmación.

---

## 11. Estructura sugerida de paquetes

```
app/src/main/java/<paquete>/
  AppNettsegur.kt              // Application + AppContainer
  MainActivity.kt
  data/local/                  // AppDatabase, entidades, DAOs, converters
  data/remote/                 // FirebaseNovedadesDataSource
  data/repository/             // NovedadRepository, SyncRepository
  data/prefs/                  // PerfilTecnicoStore (DataStore)
  util/                        // ConectividadMonitor, ImagenUtils, FirmaUtils, Hash
  validacion/                  // ReglasValidacion.kt, ResultadoValidacion
  config/                      // Catalogos.kt, Mensajes.kt, Limites.kt (tamaños, timeout, mínimos)
  ui/theme/                    // Color, Type, Theme
  ui/inicio/  ui/captura/  ui/listado/  ui/detalle/  ui/perfil/  ui/componentes/
  navigation/
```

---

## 12. Fases de implementación

- **Fase 0 – Diagnóstico.**
  - Revisa el proyecto existente: Gradle, versión de AGP, Kotlin, si usa Compose y el nombre del paquete.
  - **No lo recrees.**
  - Verifica que exista `.gitignore` y que **no** se versionen `local.properties`, `/build` ni claves.
  - Dime qué falta para Firebase: el `google-services.json` en `app/` y el nombre de paquete registrado.
- **Fase 1 – Base.** Dependencias, tema visual (colores, tipografía, formas), navegación vacía entre pantallas y `AppContainer`.
- **Fase 2 – Persistencia.**
  - Room: entidades, DAOs con `Flow`, converters del enum y carga del catálogo de proyectos.
  - Perfil del técnico con DataStore.
  - Pruebas unitarias o instrumentadas básicas del DAO.
- **Fase 3 – Captura y validación.**
  - Formulario agrupado y reglas R1–R6.
  - Pruebas unitarias de **cada** regla.
  - GUARDAR en Room con estado PENDIENTE.
- **Fase 4 – Evidencias y firmas.** Cámara, galería, copia interna, hash, pad de firma y limpieza de temporales.
- **Fase 5 – Listado, detalle, edición y eliminación** según los estados.
- **Fase 6 – Conectividad y sincronización.**
  - Banner de conexión.
  - Algoritmo de la sección 9.2 y resumen final.
  - Simulador de fallo en debug.
- **Fase 7 – Pulido de UX.** Estados vacíos, carga, errores, accesibilidad (contraste y `contentDescription`) y revisión contra el sistema visual.
- **Fase 8 – Pruebas del taller.**
  - Guíame paso a paso para ejecutar y capturar las pruebas A a E en el emulador.
  - Indica las reglas de RTDB de prueba que debo poner en la consola.
  - Genera el APK debug.

---

## 13. Notas de coherencia con la documentación

Estas decisiones prevalecen sobre versiones anteriores del documento. El equipo debe actualizar el documento para que coincida.

- La sincronización es **solo manual**. No hay reintento automático ni envío al reconectar.
- No existe un "servidor" propio ni "HTTP 2xx". La confirmación es la tarea de Firebase RTDB completada con éxito. **No se implementa** el consecutivo asignado por servidor.
- `estadoSync` es un enum de tres estados, no Boolean.
- Se agregan al diccionario estos campos, que las reglas R1–R6 requieren: `estadoAtencion`, `motivoNoResuelta`, `serialEquipo` y `cedulaRepresentanteCliente`.
- `observacionesRecomendaciones` es **opcional**, como dice la matriz de decisiones. Ambas firmas son **obligatorias**, como dice el diccionario.
- La limpieza automática de datos sincronizados (30 días / 7 días) queda **fuera de alcance** en esta entrega.
