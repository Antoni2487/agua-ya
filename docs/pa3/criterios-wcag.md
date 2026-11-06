# PA3: criterios de accesibilidad implementados

La rúbrica pide al menos 8 de 11 criterios. **Están los 11** en la web de EPSEL rediseñada (`rediseno-epsel/`), que es donde se prueba con Tab y Enter. La app AguaYa cumple los que aplican a una app móvil; se muestran con TalkBack.

Para el informe (sección 4), cada fila necesita tres cosas: dónde se aplica, una **captura del código** (abrir el archivo en la línea indicada) y una **evidencia visual** (captura de pantalla haciendo lo que dice la última columna).

Las líneas corresponden a la rama `develop` del 6 nov 2026. Si cambian, búscalas por el texto del código.

## Web de EPSEL rediseñada

| # | Criterio (WCAG) | Dónde está en el código | Cómo se demuestra |
|---|---|---|---|
| 1 | Alt text descriptivo (1.1.1 A) | `index.html:96`: `alt="Una cuadrilla de EPSEL con cascos naranjas trabaja en un buzón…"`. El logo (`index.html:24`) tiene `alt=""` porque el enlace ya se llama "EPSEL, página de inicio" (`index.html:23`) | axe: 0 fallas `image-alt`. Con el lector de pantalla, la foto se describe |
| 2 | Contraste ≥ 4,5:1 (1.4.3 AA) | `estilos.css:10-19`: cada color con su contraste anotado (`--tinta` 14,6:1, `--c3` 6,7:1…) | WAVE: 0 errores de contraste. WebAIM Contrast Checker con `#0b2545` sobre el fondo |
| 3 | Navegación por teclado (2.1.1 A) | `index.html:28` y `:35`: el menú usa `<button>` reales con `aria-expanded`. `sitio.js:98-110`: abrir y cerrar, y **Esc** devuelve el foco | Recorrer la portada solo con Tab, Enter y Esc |
| 4 | Foco visible (2.4.7 AA) | `estilos.css:82-86`: `:focus-visible` con borde oscuro de 3 px y anillo amarillo `--foco` de 6 px | Presionar Tab: cada control se rodea de amarillo |
| 5 | ARIA en íconos sin texto (4.1.2 A) | `index.html:23`: `aria-label="EPSEL, página de inicio"`. `index.html:29`: el ícono del menú con `aria-hidden="true"`. `index.html:32`: `nav aria-label="Principal"`. Los filtros con `aria-pressed` | Inspeccionar el DOM. axe: 0 fallas `button-name` y `link-name` |
| 6 | Áreas táctiles ≥ 44 × 44 px (2.5.5 AA) | `estilos.css:107`: botón `min-height: 48px`. `estilos.css:115`: `.boton-grande` de 56 px. `estilos.css:135` y `:142`: logo y opciones del menú de 48 px | DevTools a 390 px: 0 controles menores de 44 px (antes, 34 de 54) |
| 7 | Errores con texto, no solo color (1.4.1 A) | `sitio.js:317-322`: `mostrarError` escribe el mensaje, pone `aria-invalid` y devuelve el foco. `index.html:322-323`: `aria-describedby` une el campo con su error | Buscar `xx`: "Escribe al menos 3 letras de tu zona o distrito." Enviar el formulario de reclamo vacío |
| 8 | Labels visibles y autocomplete (1.3.5 AA) | `index.html:321-322`: `<label for="r-nombre">` + `autocomplete="name"` | Clic en el texto "Tu nombre" enfoca el campo. El navegador ofrece autocompletar |
| 9 | Skip link (2.4.1 A) | `index.html:18`: `<a class="saltar" href="#contenido">Ir al contenido principal</a>`. `estilos.css:91-96`: oculto hasta recibir el foco. `index.html:90`: `<main id="contenido" tabindex="-1">` | Primer Tab en cualquier página |
| 10 | HTML semántico (1.3.1 A) | `index.html:20` `<header>`, `:32` `<nav>`, `:90` `<main>`, `:98` un solo `<h1>`, `:418` `<footer>`. Tablas con `caption` y `th scope` en `informacion.html` | WAVE, pestaña "Structure". axe: 0 fallas `landmark-*` y `heading-order` |
| 11 | Texto ≥ 14 px (P7 Diseño Universal) | `estilos.css:39-40`: cuerpo `--t-cuerpo` de 17 a 19 px; lo más chico (`--t-pequeno`) es de 15 px | DevTools → Computed → `font-size` |

**Además (suma puntos en la exposición):**
- `lang="es-PE"` (3.1.1).
- "Reducir movimiento" respetado (2.3.3).
- Mensajes con `role="status"` (4.1.3).
- Sin desplazamiento horizontal a 390 px ni con zoom al 200 % (1.4.10).

## App AguaYa (vecino y conductor)

En la app no hay Tab ni `:focus`: el equivalente es **TalkBack** (Android) o **VoiceOver** (iPhone), que recorren la pantalla deslizando el dedo.

| # | Criterio | Dónde está en el código | Cómo se demuestra |
|---|---|---|---|
| 2 | Contraste | `mobile/src/tema.ts`: colores del Figma. El texto `#1c2433` y el suave `#4e5a6e` sobre `#f6f3ee` superan 4,5:1 | Contrast Checker con los colores del tema |
| 5 | Etiquetas para el lector de pantalla | `componentes/ui.tsx:27`: botón Volver con `accessibilityLabel="Volver"`. `componentes/vecino.tsx:90`: pestañas con `accessibilityRole="tab"` y `selected`. `vecino/Calificar.tsx:55`: cada estrella dice "4 de 5: Buena". Los íconos SVG van ocultos (`componentes/iconos.tsx:27`) | Con TalkBack, deslizar por Inicio y Calificar |
| 6 | Áreas táctiles | `tema.ts:36`: `toque: 56` (botones de 56 dp). Campos de 52 dp y enlaces de 48 dp (`pantallas/Acceso.tsx:83`) | Captura con la regla de Figma o "Mostrar límites de diseño" en Android |
| 7 | Errores con texto | `componentes/ui.tsx:97`: el error se escribe debajo del campo y se anuncia (`accessibilityLiveRegion="polite"`). Los avisos usan `accessibilityRole="alert"` (`ui.tsx:139`) | Ingresar con una clave equivocada: "DNI o clave incorrectos" |
| 8 | Etiquetas visibles y autocompletar | `componentes/ui.tsx:71-72`: `Campo` siempre muestra su etiqueta arriba (nunca solo un placeholder) y acepta `autoCompletar` | Pantalla de registro |
| 11 | Texto grande | `tema.ts`, objeto `letra`: el cuerpo es de 17 px o más. La app respeta el tamaño de letra del sistema | Ajustes de Android → Tamaño de fuente al máximo |
| — | Estado sin depender del color | Inicio dice en texto "Faltan 3 paradas" y "Verificada por GPS". La gota y la tira de paradas son decorativas y el lector las salta (`componentes/vecino.tsx:25` y `:50`) | TalkBack lee el estado completo |

**Pendiente de probar en la app.** Hay que hacerlo en la prueba con usuarios y anotar lo que salga:
- **TalkBack de punta a punta.** Hasta ahora solo se revisó el código.
- **Fuente del sistema al máximo.** Puede que algún texto se corte.
- **El mapa.** Es visual por naturaleza. Para el lector, el mapa tiene una descripción en texto y la hoja de abajo dice cuántas paradas faltan. Si alguien no ve el mapa, no pierde información.

### Cómo analizar la app (WAVE y axe no sirven en una app nativa)

WAVE y axe analizan páginas web. La app no es una página: se dibuja con componentes nativos de Android. Su equivalente es **Accessibility Scanner**, la app gratuita de Google en Play Store, que revisa:
- el tamaño de los toques (48 dp);
- el contraste;
- las etiquetas que faltan.

**Pasos:**
1. Instalar Accessibility Scanner y darle permiso.
2. Abrir AguaYa y tocar el botón azul del Scanner en cada pantalla: Acceso, Inicio del vecino, Mapa, Reportar y Calificar, y en el conductor Mis rutas y la ruta.
3. Hacer una captura del resultado de cada una.

Esas capturas son la evidencia de la app. **Las 3 pantallas mínimas de la rúbrica igual se cumplen con la web**: la web rediseñada tiene 7 páginas y el panel del admin también se puede analizar con axe.

## Reporte automatizado (deliverable 4: mínimo 3 pantallas)

Ya existe el reporte de axe sobre el sistema real: [rediseno/README.md](rediseno/README.md), con los JSON en `rediseno/axe/`. Da **0 violaciones en las 7 páginas** y en sus estados de escritorio, celular y menú abierto.

Falta la **captura de pantalla**. La rúbrica pide la captura del análisis, no el JSON. En Chrome, para cada página:
1. Instalar la extensión **axe DevTools** (gratis) y/o **WAVE**.
2. Abrir `http://localhost:5500/` → F12 → pestaña axe DevTools → "Scan all of my page" → captura.
3. Repetir en `cisterna.html` y `informacion.html` (las 3 pantallas mínimas), y si se puede en `atencion.html`.
4. Guardar las capturas en `docs/pa3/rediseno/capturas/` y ponerlas en el informe junto a las de la línea base ([linea-base/axe/](linea-base/axe/)) para el antes y después.
