# Guía de pruebas del taller — AppNettsegur

> Ajusten los nombres de las pruebas A–E al enunciado exacto del taller si difieren.
> Aquí se asume: A = captura sin Internet, B = persistencia al reiniciar, C = sincronización,
> D = no duplicar, E = fallo parcial (simulado).

## 0. Preparación (una sola vez)

1. **Firebase**
   - Consola de Firebase → crear proyecto → *Realtime Database* → *Crear base de datos* (modo bloqueado).
   - Registrar la app Android con el paquete **`com.cgro.appnettsegur`**.
   - Descargar `google-services.json` **después** de crear la base de datos (así trae `firebase_url`)
     y copiarlo en `app/google-services.json`.
   - El build aplica el plugin `google-services` automáticamente cuando el archivo existe.
2. **Reglas de prueba** (pestaña *Reglas* de Realtime Database → *Publicar*):

   ```json
   {
     "rules": {
       ".read": false,
       ".write": false,
       "novedades": {
         ".read": true,
         "$idRegistro": {
           ".write": true,
           ".validate": "newData.hasChildren(['idRegistro', 'proyectoId', 'evidencias', 'firmas']) && newData.child('idRegistro').val() === $idRegistro"
         }
       }
     }
   }
   ```

   Son reglas **solo para el taller**: cualquiera puede escribir en `novedades/`. La regla
   `.validate` exige que la llave del nodo sea el mismo `idRegistro` (refuerza el "no duplicar").
3. **Emulador**: en Windows, el emulador necesita aceleración por hardware. Instalen
   *Android Emulator hypervisor driver* desde Android Studio → *SDK Manager* → *SDK Tools*
   (o activen *Windows Hypervisor Platform* en "Características de Windows").
   También sirve un teléfono físico con depuración USB.
4. **Instalar**: `./gradlew installDebug`, o el botón ▶ de Android Studio.
   El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

Para quitar y poner Internet en el emulador: *Extended controls (⋯) → Cellular → Data status: Denied*
y apagar el Wi-Fi en Configuración rápida. También sirve el **modo avión**.

---

## Prueba A — Captura sin Internet

1. Activar el **modo avión** *antes* de abrir la app.
2. Abrir la app. Primer inicio: escribir el nombre del técnico → **Continuar**.
3. En Inicio, verificar el banner amarillo: «Sin conexión: tus registros se guardan en el dispositivo…».
4. **Nuevo registro** → llenar todo, tomar 1–3 fotos (cámara o galería) y firmar ambas casillas.
5. Probar las validaciones: dejar el serial como `AB 1` → GUARDAR → el campo queda en rojo con
   el mensaje R4, la pantalla hace scroll hasta él y no se borra nada de lo escrito.
6. Corregir y pulsar **GUARDAR EN EL DISPOSITIVO** → snackbar «Guardado en el dispositivo» → listado.
7. Comprobar la etiqueta **Pendiente** (reloj, amarillo). En Inicio, *Pendientes* = 1.

📸 Capturas: banner sin conexión, error de validación, registro Pendiente.

## Prueba B — Persistencia al cerrar y reabrir

1. Con 2 o más registros guardados, cerrar la app desde "Recientes" (deslizar).
2. Volver a abrir la app sin Internet.
3. Verificar que siguen los registros y, en el detalle, las fotos y las firmas.

📸 Capturas: listado y detalle después de reabrir.

## Prueba C — Sincronización manual

1. Quitar el modo avión. El banner cambia a «Conectado. Tienes N registros pendientes…».
2. Esperar unos segundos: **no debe enviarse nada solo**. En la consola de Firebase, `novedades` sigue vacío.
3. Pulsar **SINCRONIZAR** → barra de progreso «Enviando X de N…» → «N sincronizados · 0 con error».
4. Consola de Firebase → `novedades/{idRegistro}`: aparecen los campos, `evidencias/0..`, `firmas/` y
   `fechaSincronizacionServidor`.
5. En la app, el registro queda **Sincronizado** (check verde) y en solo lectura: no hay botón Editar ni Eliminar.

📸 Capturas: progreso, resumen, nodo en la consola y registro Sincronizado.

## Prueba D — Una segunda sincronización no duplica

1. Pulsar **SINCRONIZAR** otra vez → «No hay registros por sincronizar.»
2. En la consola, contar los nodos de `novedades`: el número no cambió.
3. Variante: provocar un error (Prueba E), corregir y reintentar. El nodo se **sobrescribe** con la
   misma llave `idRegistro`, porque se usa `setValue` y no `push()`.

📸 Capturas: mensaje «No hay registros…» y consola con el mismo número de nodos.

## Prueba E — Fallo parcial (simulador de debug)

1. Crear 3 registros nuevos (pueden ser offline).
2. Inicio → ícono 🐞 **Menú de desarrollador** → activar «Simular fallo en el registro N.º» → `2` → **Aplicar**.
   Aparece el aviso rojo «Modo prueba: el registro N.º 2 fallará…».
3. Con Internet, pulsar **SINCRONIZAR** → «2 sincronizados · 1 con error».
4. El 2.º registro (por fecha de creación) queda en **Error** y su detalle muestra
   «Último error: Fallo simulado desde el menú de desarrollador.». Los otros dos sí llegaron a Firebase.
5. Desactivar el simulador → en el detalle del registro con error pulsar **Reintentar** → queda Sincronizado.

Otros errores reales que se pueden mostrar:
- **Permiso denegado**: publicar reglas con `".write": false` → SINCRONIZAR → ERROR «Firebase rechazó el envío…».
- **Timeout**: conexión muy lenta o caída en medio del envío → ERROR «Tiempo de espera agotado…» (15 s).

📸 Capturas: menú de desarrollador, resumen con error, detalle en Error y el mismo registro tras Reintentar.

---

## Pruebas automáticas

| Comando | Qué prueba |
|---|---|
| `./gradlew testDebugUnitTest` | Reglas R1–R6 y validación completa (`ReglasValidacionTest`) |
| `./gradlew connectedDebugAndroidTest` | DAO de Room: estados, conteos, no duplicar, cascada (`NovedadDaoTest`). Requiere emulador o teléfono |
