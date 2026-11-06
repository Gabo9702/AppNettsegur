# M1 — Datos y Firebase · Resumen

Módulo **M1** (MODULOS.md, secciones 1, 4, 5, 6 y 9). Rama `modulo/datos-firebase`.
Desarrollo: Gabriel Rodríguez. Diseño: Santiago Torres. Pruebas/documentación: Juan Diego Rojas.

---

## 1. Qué se construyó

**Fase A — Contrato y sesión**
- `config/Roles.kt` (enum `Rol`) y `config/RutasFirebase.kt` (todas las rutas de la sección 4 como funciones).
- `data/model/Modelos.kt`: `Usuario`, `Proyecto`, `Hito`, `Documento`, `Archivo`, `Empleado`,
  `DocumentoCumplimiento`, `Carta`, `Notificacion`, `NovedadRemota`, `PersonalAsignado`.
  `Empleado.estadoCumplimiento(hoy)` y `habilitado(hoy)` se **calculan**, no se guardan.
- Interfaces de la sección 6.2 (`data/repository/Contratos.kt`), que M2 y M3 consumen.
- `notificaciones/NotificacionFactory.kt`: tres funciones con **IDs deterministas**
  (`nov_{id}`, `doc_{id}`, `carta_{id}`).
- Inicio de sesión real: `SesionRepositoryImpl` (Firebase Auth + caché en DataStore `SesionStore`),
  `ui/login/`. Entra **sin Internet** con la sesión en caché; mensajes claros de error.
- Navegación por rol (`Rutas.kt`, `NavGraph.kt`), `ui/compartido/BarraRol.kt` (espacio para la
  campana de M3) y pantallas de relleno. Se **retiró la pantalla de perfil del técnico**: el nombre
  sale de la sesión. Cerrar sesión advierte si hay registros pendientes (5.1).

**Fase B — Datos de prueba y reglas**
- `firebase/database.rules.json` (reglas de la sección 4.1), `firebase/seed_plantilla.json`
  (5 usuarios, 2 proyectos, 3 empleados, índices, con marcadores `UID_*`) y `firebase/LEEME.md`.

**Fase C — Repositorios reales**
- `data/remote/FirebaseFlows.kt` (callbackFlow genérico que quita el listener al cerrar),
  `data/remote/Mapeos.kt` (DataSnapshot → modelos), `util/ArchivoUtils.kt` (valida MIME/tamaño,
  Base64, decodifica a cacheDir).
- Implementaciones reales de `Proyectos`, `Documentos`, `Archivos`, `Empleados` y `Cartas`.
  Las subidas que notifican (documento, carta) y la asignación de técnicos usan **una** escritura
  `updateChildren` multi-ruta.
- Pruebas unitarias: `EmpleadoTest` (estadoCumplimiento/habilitado) y `NotificacionFactoryTest`
  (IDs deterministas, no duplica).

**Fase D — Flujo del técnico**
- Room **v2** con migración aditiva 1→2 (sin `fallbackToDestructiveMigration`):
  `novedades.uidTecnico` y `proyectos.clienteUid/ingenieroUid/fechaActualizacion`.
- Proyectos del técnico desde `indices/proyectosPorTecnico`, en Room; se eliminó
  `assets/proyectos.json`. Refresco al iniciar sesión, al abrir la captura con red y con el botón
  "Actualizar proyectos".
- `SyncRepository`: la escritura de cada registro pasó de `setValue` a **`updateChildren`
  multi-ruta** (novedad + índice + 2 notificaciones), **atómica e idempotente**. El resto del
  algoritmo del taller NO cambió.

---

## 2. Decisiones tomadas

- **Solo Firebase (plan Spark):** Auth + RTDB. Documentos/cartas en Base64 (≤ 1,5 MB) en
  `archivos/{id}`, con metadatos aparte. Sin Cloud Storage/Functions/FCM.
- **Rol en `usuarios/{uid}/rol`** (no custom claims): lo leen las reglas.
- **IDs deterministas de notificación** para que un reintento sobrescriba el mismo nodo y no duplique.
- **Escritura atómica multi-ruta** en la sincronización: o entra todo (novedad + índice +
  notificaciones) o nada; reintentar no duplica.
- **Sesión en caché (DataStore)** para abrir sin Internet tras el primer inicio de sesión.
- **Migración aditiva de Room** para no perder registros PENDIENTES/ERROR.
- **Acceso protegido a Firebase:** si falta `google-services.json`, la app compila y el técnico
  trabaja offline; el login y las secciones en línea muestran un mensaje claro en vez de reventar.

---

## 3. Problema técnico principal resuelto (punto 18)

**Avisar al cliente y al ingeniero cuando el técnico sincroniza, SIN servidor (plan gratuito) y sin
duplicar ni romper el flujo offline del taller.**

La solución es una **escritura `updateChildren` multi-ruta, atómica e idempotente**: en una sola
operación se escriben la novedad, su índice por proyecto y las notificaciones (con IDs
deterministas `nov_{id}`). Así el aviso sale **solo cuando Firebase confirma** el registro (nunca al
pulsar GUARDAR), un segundo SINCRONIZAR no crea duplicados, y la sincronización sigue siendo
**manual** (sin WorkManager ni reintentos en segundo plano). Se respeta la regla número uno: el
flujo offline del técnico queda intacto.

Limitación conocida y documentada: si un reintento sobrescribe una notificación ya leída, vuelve a
aparecer como no leída (aceptable).

---

## 4. Cambios que hay que llevar a la documentación (MODULOS.md, parte 1, sección 10)

1. **Sincronización (punto 11 del taller / sección 5.3):** ya usa `updateChildren` multi-ruta
   (novedad + índice + notificaciones), no `setValue`.
2. **Diccionario de datos:** agregar `uidTecnico` a `novedades`.
3. **Punto 5 del taller:** el inicio de sesión requiere Internet la **primera vez**; después la app
   abre sin conexión.
4. **Índices nuevos (extensión del contrato, sección 4):** además de `proyectosPorTecnico`, M1
   agregó `indices/proyectosPorCliente`, `indices/proyectosPorIngeniero` e
   `indices/cartasPorProyecto`. Son necesarios porque las reglas protegen `proyectos` y `cartas`
   **por nodo**: listar con un query `orderByChild` exigiría abrir la lectura de todo el árbol y
   permitiría que un cliente viera proyectos ajenos. **Hay que reflejar estos índices en la
   sección 4 de MODULOS.md** y en el diagrama de la estructura de RTDB.
5. **Lectura de `archivos` (trabajo futuro, sección 4.1):** hoy es por rol (distinto de TECNICO).
   La regla fina que deje al técnico leer solo las cartas de sus proyectos queda pendiente.

---

## 5. Pendiente para poder probar y para M3/M2

- **Falta `app/google-services.json`** (paquete `com.cgro.appnettsegur`). Sin él no se puede iniciar
  sesión ni probar en línea. Los pasos de configuración y las pruebas A–E están en `firebase/LEEME.md`.
- M3 coloca `CampanaNotificaciones()` en el slot de `BarraRol`; M2 reemplaza las pantallas de relleno
  de cada rol usando las interfaces de la sección 6.2.
