# Firebase — datos de prueba y reglas (M1)

Esta carpeta prepara Firebase para los módulos nuevos (MODULOS.md, sección 9). Usa **solo** el
plan gratuito **Spark**: Firebase Authentication (correo y contraseña) y Realtime Database.

> **Falta `app/google-services.json`.** Sin ese archivo la app no inicializa Firebase y el inicio
> de sesión no funciona. Descárgalo desde la consola del proyecto (con el paquete
> `com.cgro.appnettsegur` registrado) y colócalo en `app/`. No se versiona (está en `.gitignore`).

---

## 1. Crear las 5 cuentas (Authentication)

En **Authentication → Users → Add user**, crea estas cuentas (la contraseña la eliges tú, usa la
misma para todas en pruebas, p. ej. `Nettsegur123`):

| Correo | Rol |
|---|---|
| `tecnico@nettsegur.test` | TECNICO |
| `cliente@nettsegur.test` | CLIENTE |
| `ingeniero@nettsegur.test` | INGENIERO |
| `rh@nettsegur.test` | RH |
| `analista@nettsegur.test` | ANALISTA |

Después copia el **UID** de cada usuario (columna *User UID*). Los vas a necesitar en el paso 2.

> El rol NO se asigna en Authentication: se guarda en `usuarios/{uid}/rol` (paso 2), porque los
> *custom claims* necesitarían un servidor (plan Blaze).

---

## 2. Importar los datos de prueba (Realtime Database)

1. Abre `seed_plantilla.json` y **reemplaza los marcadores** por los UID reales del paso 1:
   - `UID_TECNICO`, `UID_CLIENTE`, `UID_INGENIERO`, `UID_RH`, `UID_ANALISTA`.
   - Aparecen tanto como **llave** (bajo `usuarios/`) como **valor** (`clienteUid`, `ingenieroUid`,
     `tecnicos/…`, `uid`, `indices/proyectosPorTecnico/…`). Reemplázalos **todos**
     (un buscar‑y‑reemplazar por cada marcador).
2. Guarda el archivo (puedes llamarlo `seed.json`).
3. En **Realtime Database → (menú ⋮) → Import JSON**, selecciona el archivo.
   - Impórtalo en la **raíz** de la base de datos.
   - ⚠️ La importación **reemplaza** todo el contenido actual. Hazlo en una base de pruebas.

Qué incluye el seed:
- 5 `usuarios` con su rol.
- 2 `proyectos` (`PRY-001`, `PRY-002`) con hitos, técnicos asignados y la **vista reducida de
  personal** (`proyectos/{pid}/personal`) que el cliente sí puede leer.
- 3 `empleados` con cumplimiento. `EMP-002` tiene el certificado de **alturas vencido** a
  propósito, para ver un empleado **no habilitado**.
- `indices/proyectosPorTecnico` para que el técnico cargue sus proyectos sin Internet después.

> Las `novedades` y sus `indices/novedadesPorProyecto` NO van en el seed: las crea el técnico al
> **SINCRONIZAR** (fase D).

---

## 3. Publicar las reglas de seguridad

1. Abre `database.rules.json`.
2. En **Realtime Database → Rules**, pega todo el contenido y pulsa **Publicar**.

Qué garantizan las reglas (MODULOS.md, sección 4.1):
- Todo exige sesión iniciada (`auth != null`).
- El rol se lee de `usuarios/{uid}/rol`; **nadie puede cambiar su propio rol** desde la app.
- `novedades`: solo las escribe un TECNICO y solo con su propio `uidTecnico`; las leen el técnico
  autor, el CLIENTE y el INGENIERO del proyecto.
- `proyectos`: los lee el cliente, el ingeniero o un técnico asignado; solo el INGENIERO del
  proyecto escribe `hitos`, `avancePorcentaje`, `tecnicos` y la vista `personal` (RH también
  actualiza `personal`).
- `documentos`: los escribe el INGENIERO del proyecto y los lee también el CLIENTE.
- `archivos`: los escriben INGENIERO, RH y ANALISTA; los leen los roles distintos de TECNICO.
- `empleados`: los escribe RH (ANALISTA solo el nodo `carne`); los leen RH, ANALISTA e INGENIERO.
- `notificaciones/{uid}`: el destinatario las lee y **solo** puede cambiar `leida`; las crean el
  TECNICO (`NOVEDAD_SINCRONIZADA`), el INGENIERO (`DOCUMENTO_NUEVO`) y el ANALISTA (`CARTA_NUEVA`).
- `.indexOn` en `notificaciones/{uid}` (`fecha`) y en `cartas` (`proyectoId`).

> **Trabajo futuro documentado (sección 4.1):** la lectura de `archivos` es por rol, no por archivo.
> Falta la regla fina que deje al técnico leer **solo** los archivos de las cartas de sus proyectos;
> queda como trabajo futuro.

---

## 4. Probar las reglas con el Simulador (Realtime Database → Rules → Simulador)

El simulador deja fijar la operación (Get/Set), la ubicación, el `Auth` (UID) y los datos. Usa los
UID reales. Casos mínimos a verificar:

### 4.1 La escritura multi‑ruta del técnico SÍ pasa (sección 5.3)
- **Set**, autenticado como `UID_TECNICO`, en `/novedades/NOV-DEMO` con datos que incluyan
  `"uidTecnico": "UID_TECNICO"`, `"proyectoId": "PRY-001"` → **Permitido**.
- **Set** como `UID_TECNICO` en `/indices/novedadesPorProyecto/PRY-001/NOV-DEMO` con valor `true`
  → **Permitido**.
- **Set** como `UID_TECNICO` en `/notificaciones/UID_CLIENTE/nov_NOV-DEMO` con
  `"tipo": "NOVEDAD_SINCRONIZADA"` → **Permitido** (igual para `/notificaciones/UID_INGENIERO/...`).

### 4.2 Rechazos obligatorios
1. **Técnico con `uidTecnico` ajeno** → debe **rechazar**.
   - **Set** como `UID_TECNICO` en `/novedades/NOV-X` con `"uidTecnico": "UID_INGENIERO"`,
     `"proyectoId": "PRY-001"` → **Denegado**.
2. **Cliente que lee un proyecto ajeno** → debe **rechazar**.
   - **Get** como `UID_CLIENTE` en `/proyectos/PRY-999` (un proyecto cuyo `clienteUid` NO sea
     `UID_CLIENTE`) → **Denegado**. (Para probarlo, crea un `PRY-999` con `clienteUid` distinto,
     o cambia temporalmente el `clienteUid` de `PRY-001`.)
3. **Usuario que cambia su propio rol** → debe **rechazar**.
   - **Set** como `UID_CLIENTE` en `/usuarios/UID_CLIENTE/rol` con valor `"INGENIERO"`
     → **Denegado**. (La regla exige que el `rol` nuevo sea igual al actual.)

### 4.3 Comprobaciones útiles adicionales
- **Notificación**: un **Set** como `UID_CLIENTE` en `/notificaciones/UID_CLIENTE/x` con
  `"tipo": "NOVEDAD_SINCRONIZADA"` → **Denegado** (el destinatario no crea notificaciones).
- **Marcar leída**: un **Set** como `UID_CLIENTE` en `/notificaciones/UID_CLIENTE/nov_NOV-DEMO/leida`
  con valor `true` → **Permitido** (solo el campo `leida`).
- **Empleados**: un **Get** como `UID_CLIENTE` en `/empleados/EMP-001` → **Denegado** (el cliente ve
  el personal por la vista reducida del proyecto, no por `empleados`).
