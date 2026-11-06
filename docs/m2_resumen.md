# M2 – Funciones por rol · Resumen

Módulo M2 (MODULOS.md, secciones 1, 3 y 6). Rama `modulo/roles`, creada desde `master` (la rama
principal del repositorio; M1 y M3 ya estaban integrados ahí). Pantallas y lógica de **cliente,
ingeniero de proyectos, HCQ y RH y analista administrativo**. Contabilidad, gerencia y soporte
quedan como trabajo futuro y **no** se implementaron.

Todas las pantallas de estos roles funcionan **en línea** (sección 3): sin conexión muestran el
aviso «Esta sección necesita conexión a Internet» y un botón **Reintentar** (vuelve a suscribirse a
RTDB); nunca se cierran de golpe. Arquitectura MVVM: un ViewModel por pantalla con `StateFlow` y
estados de carga, vacío, error/sin conexión y contenido. No se tocó el flujo del técnico.

## Qué se construyó

### Fase 1 – Cliente (CU-03, CU-06; HU-02, 03, 06–10)
- **InicioCliente** (`ui/cliente/`): «Mis proyectos», tarjeta por proyecto con nombre, estado y
  barra de avance. Lee con el índice seguro `proyectosPorCliente` (el cliente no ve proyectos ajenos).
- **ProyectoDetalle** (`ui/compartido/`, compartido y adaptable por rol) con pestañas:
  - *Avance*: porcentaje y cronograma de hitos (fechas, completado/pendiente).
  - *Documentos*: agrupados por tipo; al tocar uno se descarga de RTDB (Base64 → cacheDir) y se abre
    con `ACTION_VIEW` + `FileProvider`, con progreso y manejo de error.
  - *Personal*: vista reducida (nombre, cédula, chip de cumplimiento) — HU-07.
  - *Novedades*: sincronizadas del proyecto, filtrables por estado de la atención; al tocar abre
    NovedadRemota.
  - *Cartas*: lista y descarga.
- **NovedadRemota** (`ui/compartido/`): detalle de solo lectura con datos, fotos (miniatura Base64)
  y firmas.
- Componentes compartidos nuevos (`ui/compartido/`): estados de pantalla (carga/vacío/sin conexión +
  Reintentar), `ChipCumplimiento`, `ImagenBase64`, `abrirArchivo`, `TarjetaProyecto`, `MensajesRoles`.

### Fase 2 – Ingeniero de proyectos (CU-05; HU-11, HU-12)
- **InicioIngeniero** (`ui/ingeniero/`): proyectos a su cargo (índice `proyectosPorIngeniero`).
- **ProyectoDetalle** versión ingeniero (mismas pestañas del cliente + acciones):
  - *Documentos*: botón **Cargar documento** con selector de tipo y archivo (`OpenDocument`
    PDF/JPG/PNG); valida tipo y tamaño (≤ 1,5 MB) y **rechaza con mensaje**; el diálogo es la
    confirmación y al terminar muestra «Documento publicado. El cliente fue notificado».
  - *Avance*: hitos marcables con `Switch`; el avance se recalcula.
  - *Técnicos* (pestaña exclusiva del ingeniero): asignar/quitar; **solo los habilitados**; los no
    habilitados salen deshabilitados con el motivo (p. ej. «ARL vencida el 12/10/2026»); un técnico
    ya asignado siempre se puede quitar.

### Fase 3 – HCQ y RH (CU-07; HU-15)
- **InicioRH** (`ui/rh/`): buscador (nombre/cédula), lista de empleados con chip de estado
  (Vigente/Por vencer/Vencido) **ordenada con los vencidos primero**, y la tarjeta resumen de
  vencimientos de M3. Botón **Nuevo empleado**.
- **EmpleadoFormDialog**: crear/editar datos básicos (nombre; cédula con la regla **R3** reutilizada;
  cargo; uid de técnico opcional).
- **EmpleadoDetalle**: datos, estado calculado y **habilitación para obra**; carga de ARL, seguridad
  social y certificado de alturas con fechas (valida que el vencimiento sea posterior a la expedición
  y que la expedición no sea futura); carga del carné.

### Fase 4 – Analista administrativo (CU-09; HU-13)
- **InicioAnalista** (`ui/analista/`) con pestañas:
  - *Carnés*: cargar el carné (imagen o PDF) de cada empleado.
  - *Cartas*: elegir proyecto y técnicos asignados **habilitados** (los no habilitados no se pueden
    incluir); genera el PDF en el dispositivo con `PdfDocument` (encabezado «NETTSEGUR TECNOLOGÍA &
    SEGURIDAD ELECTRÓNICA S.A.S.», fecha, cliente, proyecto, tabla de técnicos con nombre y cédula, y
    el nombre del analista); **vista previa** con `PdfRenderer` y guardado con
    `CartasRepository.guardarCarta` (notifica al cliente).

### Fase 5 – Integración
- `CampanaNotificaciones()` (M3) colocada en la `BarraRol` de las cuatro pantallas de inicio.
- Navegación revisada: login → inicio del rol usa `popUpTo(graph, inclusive=true)`, así el botón
  **Atrás nunca vuelve al inicio de sesión** estando dentro de la app.
- `NavGraph` (M1): se reemplazaron las pantallas de relleno que M1 había marcado como «M2 las
  reemplaza» (inicios de cliente/ingeniero/RH/analista, ProyectoDetalle, EmpleadoDetalle y
  NovedadRemota). `InicioRolRelleno`/`PantallaRellenoSimple` quedan sin uso (no se eliminan para no
  tocar M1 de más; eliminarlas es una limpieza menor pendiente).

## Decisiones

- **Un solo ProyectoDetalle adaptable por rol** en vez de dos pantallas: usa el `rol` de la sesión
  para mostrar u ocultar las acciones del ingeniero. Evita duplicar las cinco pestañas de lectura.
- **Pantallas reactivas sin romper el offline del técnico**: los datos de los roles se leen en línea
  de RTDB con `callbackFlow`; **no** se activa la persistencia. Como los flujos no emiten sin red, el
  estado «sin conexión» se decide con `ConectividadMonitor` y «Reintentar» fuerza una nueva
  suscripción (patrón `reintentos` + `flatMapLatest`).
- **Textos de M2 centralizados en `ui/compartido/MensajesRoles.kt`** (no en `config/Mensajes.kt`, de
  M1) para respetar la propiedad de archivos de la sección 6.1.
- **Sin rutas de navegación nuevas**: crear/editar empleado y cargar documentos/cumplimiento se
  resuelven con diálogos, reutilizando las rutas que M1 ya había declarado.
- **Habilitado se calcula, no se guarda**: la UI usa `Empleado.habilitado(hoy)` y el motivo del
  primer documento vencido/ausente.

## Problema técnico principal resuelto (punto 18)

Entregar, **en línea y sin servidor propio**, una experiencia por rol sobre la misma base de datos
que el técnico usa offline, sin activar la persistencia de RTDB ni romper su flujo: pantallas
reactivas que distinguen carga, vacío y falta de conexión (con reintento), un detalle de proyecto
único que se adapta a cliente e ingeniero, y el ciclo completo de documentos **sin Cloud Storage**
(descarga de Base64 a `cacheDir` y apertura con `FileProvider`; generación del PDF de la carta con
`PdfDocument` y previsualización con `PdfRenderer`).

## Cambios en M1 (confirmados por el equipo) y su impacto en el contrato

Estos cambios alteran el contrato de M1 y **deben llevarse a MODULOS.md** (parte 1):

1. **`ProyectosRepository.novedad(idRegistro): Flow<NovedadRemota?>`** — método **aditivo** (no cambia
   firmas existentes). Necesario para la pantalla NovedadRemota y para la Bandeja de M3
   (`NOVEDAD_SINCRONIZADA → NovedadRemota(refId)`). → Agregar a la sección 6.2.
2. **`ProyectosRepository.proyectosParaAnalista(): Flow<List<Proyecto>>`** — método **aditivo**. El
   analista no tenía forma de listar proyectos (no existe un índice por analista). → Agregar a 6.2.
3. **Reglas de seguridad (`firebase/database.rules.json`)**: se agregó `.read` de **ANALISTA** sobre
   el nodo `proyectos`. Sin esto, el analista no puede listar proyectos ni `CartasRepository.guardarCarta`
   resolver `clienteUid` (fallaría con permiso denegado). → Actualizar la nota de la sección 4.1.
   **IMPORTANTE: hay que RE-PUBLICAR las reglas en la consola de Firebase** para que la Fase 4 funcione.

### Infraestructura
- **`res/xml/file_paths.xml`**: rutas cache `descargas/` (archivos descargados que se abren con un
  visor) y `cartas/` (PDF de la carta antes de subirla). No toca la ruta `camara/` del técnico.

## Pruebas recomendadas (para Juan Diego)

1. **Cliente no ve proyectos ajenos**: inicia sesión con `cliente@nettsegur.test`. Debe ver solo sus
   proyectos (PRY-001, PRY-002, ambos de `UID_CLIENTE`). Captura del listado. Verifica en la consola
   que no existe un índice `proyectosPorCliente` con proyectos de otro cliente.
2. **Navegación por rol**: con cada cuenta de prueba, recorre inicio → detalle → pestañas → atrás.
   Comprueba que el botón Atrás en el inicio **no** regresa al login.
3. **Dos emuladores (ingeniero y cliente)** — prueba que M3 dejó pendiente:
   - Emulador A: `ingeniero@`. Abre un proyecto → *Documentos* → **Cargar documento** (elige un PDF
     ≤ 1,5 MB) → Publicar.
   - Emulador B: `cliente@` (del mismo proyecto). Debe recibir **un** aviso (campana/bandeja y, en
     segundo plano, notificación del sistema). Captura.
   - Repite con una **carta de presentación** generada por `analista@`: el cliente recibe un aviso.
   - Verifica en la consola que la notificación aparece **una sola vez** (ID determinista).
4. **Rechazo de archivo**: intenta cargar un archivo > 1,5 MB o de tipo no permitido → mensaje de
   rechazo, sin subir nada.
5. **Cumplimiento**: en RH, carga ARL con vencimiento anterior a la expedición → mensaje de error; con
   fechas válidas → el chip del empleado y la habilitación se actualizan.

> Antes de las pruebas de red: **publicar `firebase/database.rules.json` actualizado** e **importar el
> seed** con los uid reales (ver `firebase/LEEME.md`).
