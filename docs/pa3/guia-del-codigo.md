# Guía para estudiar el código de AguaYa

La rúbrica dice: *"El uso de IA es permitido solo si el grupo puede explicar cada línea y justificar cada decisión de accesibilidad. La docente podrá pedir modificaciones en vivo."* Esta guía sirve para eso. Recorre cada pieza en el orden en que conviene leerla y termina con ejercicios de práctica.

**Cómo estudiar:**
1. Lee la pieza aquí.
2. Abre los archivos que se nombran.
3. Haz el ejercicio de esa pieza sin mirar la solución.

Con la web rediseñada y la app basta para IHM. El backend se explica en SIE.

---

## 0. El mapa general

```
Celular (app Expo) ──┐
Panel del admin ─────┼──HTTP/JSON──▶ Backend Spring Boot ──▶ PostgreSQL
Web de EPSEL ────────┘  (solo lectura,       │
                         sin login)           └──WebSocket──▶ Panel (mapa en vivo, alertas)
```

- **Una sola fuente de verdad: el backend.** Ni la web ni la app guardan datos propios. Piden al backend y lo muestran.
- **Tres clientes con tres públicos:**
  - la **web rediseñada** es para cualquiera y no tiene login;
  - el **panel** es para el administrador de EPSEL;
  - la **app** es para el vecino y el conductor. Según quién inicia sesión, muestra unas pantallas u otras.
- **El GPS sale del celular del conductor.** Cada ~15 s manda su posición. El backend la guarda y la reenvía al panel por WebSocket. El vecino la consulta cada 15 s.

---

## 1. Web de EPSEL rediseñada (`rediseno-epsel/`): el corazón del PA3

HTML, CSS y JavaScript **sin librerías**, porque el vecino usa un celular de gama baja con pocos datos (antes la página pesaba 85 MB, ahora 0,8 MB).

### 1.1 HTML (`index.html` y las otras 6 páginas)

Léelo de arriba abajo; la estructura es el criterio 10:

| Línea aprox. | Qué es | Por qué |
|---|---|---|
| 2 | `<html lang="es-PE">` | El lector de pantalla pronuncia en español |
| 18 | `<a class="saltar" href="#contenido">` | Skip link: es lo primero que alcanza Tab y salta el menú |
| 20 | `<header>` | Logo y menú |
| 28 | `<button class="boton-menu" aria-expanded="false" aria-controls="navegacion">` | Es un **botón** real (no un `div`), así que funciona con Enter y Espacio sin código extra. `aria-expanded` le dice al lector si está abierto |
| 32 | `<nav aria-label="Principal">` | Hay dos `nav` (arriba y en el pie). El nombre los distingue |
| 90 | `<main id="contenido" tabindex="-1">` | Destino del skip link. `tabindex="-1"` permite enfocarlo por código sin que entre en el orden de Tab |
| 98 | Un solo `<h1>` | Los lectores navegan por títulos |
| 320–335 | `<label for>` + `<input id>` + `<p class="campo-error">` | La etiqueta queda unida al campo. El error se une con `aria-describedby` |
| 418 | `<footer>` | Pie |

**Regla que conviene saber decir:** "Usamos el elemento HTML correcto antes que ARIA. ARIA solo agrega lo que el HTML no dice, como si un menú está abierto o qué botón del filtro está presionado".

### 1.2 CSS (`estilos.css`)

- **Líneas 5–40, variables (tokens).** Los colores llevan su contraste anotado; por ejemplo, `--tinta: #0b2545` da 14,6:1 sobre el fondo. Las tipografías usan `clamp()`, que crece con la pantalla pero nunca baja de 17 px en el cuerpo.
- **Líneas 82–86, `:focus-visible`.** Son dos anillos: uno oscuro de 3 px y uno amarillo de 6 px, para que el foco se vea sobre fondos claros y oscuros. `:focus-visible` (y no `:focus`) muestra el anillo con teclado, pero no al hacer clic con el mouse.
- **Líneas 91–96, `.saltar`.** El skip link está fuera de la pantalla y baja a la vista al recibir el foco. No se usa `display:none`, porque así no se podría enfocar.
- **Líneas 107 y 115, tamaño de los botones.** `min-height: 48px` en todos los botones y 56 px en los grandes, por encima del mínimo de 44 px de la rúbrica.
- **`@media (prefers-reduced-motion: reduce)`.** Si la persona pidió menos movimiento en su sistema, se apagan las animaciones.

### 1.3 JavaScript (`sitio.js`)

Un solo archivo para las 7 páginas. Cada bloque revisa si su elemento existe antes de arrancar (`if (!form) return;`). Sin JavaScript el sitio sigue mostrando el menú completo y los textos (mejora progresiva).

| Bloque (línea aprox.) | Qué hace | Qué explicar |
|---|---|---|
| 1–20, Configuración | `API` = backend (en `localhost` usa el local); `?datos=ejemplo` usa datos de prueba | Por qué hay un respaldo: si el backend no responde en 6 s, se muestran datos de ejemplo y se avisa |
| 24, Utilidades | `sinTildes` hace que "cerropon" encuentre "Cerropón" | Error de tipeo = barrera cognitiva |
| 49, `pedir()` | `fetch` con `AbortController` y tiempo límite | No dejar al usuario esperando para siempre |
| 73, Menú | Abre con clic (que incluye Enter y Espacio en un `<button>`), cambia `aria-expanded` y **Esc** cierra y **devuelve el foco** al botón | El foco nunca se pierde (2.4.3) |
| 274, Estado de una zona | El estado siempre en palabras: "Repartiendo ahora", "Programada" | 1.4.1: el color solo acompaña |
| 308, Buscador | `mostrarError` escribe el texto, pone `aria-invalid="true"` y devuelve el foco al campo | Criterio 7 |
| 362, Semana de la cisterna | Pide `/api/public/transparencia/cronograma` y arma 7 días con su placa | Datos reales del backend |
| 410, Información pública | Cifras de cumplimiento; al cambiar el periodo lo anuncia con `role="status"` | 4.1.3 |
| 514, Formulario | Valida, marca los vacíos y abre WhatsApp con el mensaje listo | No hay backend de reclamos: se usa el canal real de EPSEL |

**Función `elemento()` (línea ~41).** Crea nodos del DOM con `createElement` y `append`, nunca con `innerHTML`. Así un dato que llega del servidor no puede inyectar HTML (seguridad).

---

## 2. App AguaYa (`mobile/`)

React Native con Expo y TypeScript. **Una sola app:** `App.tsx` decide qué mostrar según el rol de la sesión.

### 2.1 Orden de lectura

1. **`src/tema.ts`.** Los colores, tamaños y fuentes del Figma. `medida.toque = 56` es el alto de los botones. La fuente del cuerpo es **Atkinson Hyperlegible**, diseñada para personas con baja visión.
2. **`src/componentes/ui.tsx`.** Los ladrillos de todas las pantallas:
   - `Boton`: siempre lleva `accessibilityRole="button"` y su etiqueta, y avisa si está desactivado u ocupado.
   - `Campo`: la etiqueta siempre está visible arriba y el error se escribe debajo y se anuncia (`accessibilityLiveRegion`).
   - `Aviso`: usa `accessibilityRole="alert"`, así que el lector lo lee apenas aparece.
3. **`src/api/cliente.ts`.** Hace un `fetch` con el token JWT; si vence, lo renueva con el refresh token.
4. **`App.tsx`.** Carga las fuentes y luego muestra Acceso → (Registro | Restablecer) → la app del vecino o del conductor.
5. **`pantallas/Acceso.tsx`.** Una sola pantalla de ingreso. El vecino entra con DNI + clave de 4 números; "Soy conductor de cisterna" cambia a contraseña.
6. **`pantallas/vecino/VecinoApp.tsx`.** Arma las 4 pestañas (Inicio, Mapa, Reportar, Mi cuenta), pide `/api/vecino/proxima-atencion` cada 30 s y maneja el botón atrás de Android.
7. **`pantallas/vecino/InicioVecino.tsx`.** Muestra "Te toca el viernes" o "Faltan 3 paradas", la placa y la última entrega con "Verificada por GPS".
8. **`pantallas/vecino/MapaVecino.tsx`.** El camión y tu casa en `react-native-maps`. El mapa tiene una descripción en texto para el lector.
9. **`pantallas/Reportar.tsx` y `vecino/Calificar.tsx`.** Las opciones son `accessibilityRole="radio"` con `checked`, y cada estrella dice "4 de 5: Buena".

### 2.2 Qué explicar sobre accesibilidad en la app

- En el celular no hay Tab ni `:focus`. El equivalente es **TalkBack o VoiceOver**, que lee `accessibilityLabel`, `accessibilityRole` y `accessibilityState`.
- Los íconos SVG llevan `importantForAccessibility="no-hide-descendants"`: son decorativos y el texto de al lado ya dice lo mismo.
- La **gota de avance** también va oculta para el lector, porque el texto "Faltan 3 paradas" ya da esa información.
- **La clave de 4 dígitos** va con teclado numérico, se puede mostrar con el botón del ojo y se bloquea tras 5 intentos.

---

## 3. Panel del admin (`web/`)

React + Vite + TypeScript. Corre con `npm run dev`.

| Archivo | Qué hace |
|---|---|
| `src/App.tsx` | Rutas de la página (`/login`, `/admin/...`). `/transparencia` redirige a la web rediseñada |
| `src/api/cliente.ts` | `fetch` con token y manejo de errores uniforme |
| `src/api/tiempoReal.tsx` | Conexión STOMP por WebSocket: mapa en vivo y alertas de incidencias |
| `src/componentes/LayoutAdmin.tsx` | Menú lateral |
| `src/paginas/admin/Capacidad.tsx` | "¿Alcanzan las cisternas?" (fórmula de SUNASS) |
| `src/paginas/admin/Rutas.tsx`, `RutaDetalle.tsx` | Programar una ruta, asignar cisterna y conductor, ver las paradas |
| `src/paginas/admin/Monitoreo.tsx` | Mapa en vivo (Leaflet + OpenStreetMap) |
| `src/paginas/admin/Incidencias.tsx`, `IncidenciaDetalle.tsx` | Reportes y reasignación manual |

Cada página tiene su `.test.tsx` (Vitest + Testing Library): las pruebas buscan los elementos **por su rol y su nombre accesible** (`getByRole('button', { name: 'Simular' })`). Si un botón no tiene nombre, la prueba falla.

---

## 4. Backend (`backend/`), para las preguntas de "¿cómo sabe que…?"

Spring Boot 3 (Java 21), con un paquete por funcionalidad. Cada paquete sigue el mismo flujo: `controller` → `service` (aquí van las reglas) → `repository` → la base de datos. Las reglas completas están en [especificacion.md](../especificacion.md#8-reglas-de-negocio-obligatorias).

| Pregunta | Respuesta corta | Dónde |
|---|---|---|
| ¿Cómo sabe que la cisterna llegó? | **Geocerca:** la última posición del conductor tiene que estar a ≤ 50 m de la parada y tener ≤ 120 s de antigüedad. Si no, se rechaza y el intento queda registrado (evidencia ante desvíos) | `monitoreo/service/GeocercaService.java` |
| ¿Cómo se ordenan las paradas? | Primero las instituciones críticas (postas, colegios). Después, la parada más cercana aún no visitada, partiendo del punto de llenado | `ruta/service/OrdenadorParadas.java` |
| ¿Cuántas cisternas hacen falta? | `viajes = ⌈viviendas × 80 L × días ÷ capacidad⌉` y `cisternas = ⌈viajes ÷ viajes por día⌉`, contra las operativas con autorización vigente | `capacidad/service/CalculadoraViajes` |
| ¿Por qué la ruta pide vecinos? | Las paradas son las casas de los vecinos registrados, o los puntos de entrega si el sector es por punto | `ruta/service/RutaService.java` |
| ¿Cómo se protege la clave? | BCrypt (nunca en texto) y bloqueo de 15 min tras 5 fallos | `auth/service/AuthService.java` |
| ¿Cómo llega el camión al mapa del admin? | El conductor hace POST de su posición. El backend la guarda y la publica por WebSocket en `/topic/rutas/{id}/posicion` | `monitoreo/service/MonitoreoService.java`, `PublicadorTiempoReal.java` |

---

## 5. Ejercicios para practicar los cambios en vivo

Hazlos en una rama de práctica y luego descarta los cambios (`git checkout .`).

1. **Color del foco.** Cambia `--foco` en `estilos.css` a otro color y verifica que se vea con Tab. *Luego explica por qué hay dos anillos.*
2. **Nuevo botón accesible.** En `index.html`, agrega un botón que solo tenga un ícono y ponle un `aria-label`. Comprueba con axe que, si quitas el `aria-label`, aparece `button-name`.
3. **Nuevo mensaje de error.** En `sitio.js`, en el buscador, haz que una búsqueda con números dé el error "Escribe el nombre de tu zona, no un número".
4. **Quitar el skip link.** Bórralo y presiona Tab: ahora hay que pasar por todo el menú para llegar al contenido. Cuenta cuántos Tab hacen falta antes y después. Luego vuelve a ponerlo.
5. **Contraste.** Cambia `--tinta-suave` a `#9aa5b1` y mira cómo WAVE marca error de contraste. Calcula la razón en WebAIM.
6. **App.** En `mobile/src/tema.ts`, cambia `medida.toque` a 40 y explica por qué rompe el criterio 6.
7. **Etiqueta en la app.** En `componentes/ui.tsx`, quita el `accessibilityLabel` del botón Volver y escucha con TalkBack qué lee ahora ("botón", sin nombre).
