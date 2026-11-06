# Capturas de código para el informe y la exposición

Cada fila es una captura que va en el informe, en la sección 3 ("Accesibilidad implementada"), en lugar del recuadro gris con código que tiene ahora. Debajo de cada tabla está **qué decir** si la docente pregunta.

**Cómo capturar:**
1. Abre el archivo en VS Code y presiona **Ctrl+G** para ir a la primera línea.
2. Selecciona las líneas indicadas para que se vean resaltadas.
3. Toma la captura con **Win+Shift+S**, incluyendo los números de línea y el nombre del archivo en la pestaña.

Los números de línea corresponden a la rama `develop` después de mezclar `fix/rediseno-cabecera`. Si cambian, busca el texto con **Ctrl+F**.

---

## Web de EPSEL rediseñada (`rediseno-epsel/`)

### C1. Skip link: criterio 9 (WCAG 2.4.1)

| # | Archivo | Líneas | Qué se ve |
|---|---|---|---|
| C1a | `rediseno-epsel/index.html` | **18** | `<a class="saltar" href="#contenido">Ir al contenido principal</a>` |
| C1b | `rediseno-epsel/index.html` | **90** | `<main id="contenido" tabindex="-1">` |
| C1c | `rediseno-epsel/estilos.css` | **91–96** | `.saltar` fuera de la pantalla (`top: -100px`) y `.saltar:focus { top: 0.75rem; }` |

**Qué decir:** "Es el primer enlace de la página. Está escondido arriba y baja cuando recibe el foco con Tab. Lleva al `<main>`, que tiene `tabindex="-1"` para poder recibir el foco sin entrar en el orden del Tab. Así, quien navega con teclado o lector de pantalla se salta el menú de 23 opciones."

### C2. Foco visible: criterio 4 (WCAG 2.4.7)

| # | Archivo | Líneas | Qué se ve |
|---|---|---|---|
| C2 | `rediseno-epsel/estilos.css` | **82–86** | `:focus-visible { outline: 3px solid var(--tinta); outline-offset: 3px; box-shadow: 0 0 0 6px var(--foco); }` |

**Qué decir:** "El anillo tiene dos colores: azul oscuro por dentro y amarillo por fuera. Así se ve sobre fondos claros y oscuros. Usamos `:focus-visible` y no `:focus` para que el anillo aparezca al navegar con teclado y no al hacer clic con el mouse."

### C3. Teclado en el menú: criterio 3 (WCAG 2.1.1)

| # | Archivo | Líneas | Qué se ve |
|---|---|---|---|
| C3a | `rediseno-epsel/index.html` | **35** | `<button class="menu-boton" type="button" aria-expanded="false" aria-controls="sub-agua">` |
| C3b | `rediseno-epsel/sitio.js` | **98–103** | Al hacer clic, se cambia `aria-expanded` y se abre el submenú |
| C3c | `rediseno-epsel/sitio.js` | **105–111** | **Esc** cierra el submenú y devuelve el foco al botón (`abierto.focus()`) |

**Qué decir:** "El menú usa `<button>` de verdad, no un `<div>`. Un botón ya funciona con Enter y Espacio sin código extra. `aria-expanded` le dice al lector de pantalla si el menú está abierto, y con Esc se cierra y el foco vuelve al botón, así el usuario no se pierde."

### C4. Errores con texto: criterio 7 (WCAG 1.4.1 / 3.3.1)

| # | Archivo | Líneas | Qué se ve |
|---|---|---|---|
| C4a | `rediseno-epsel/sitio.js` | **317–322** | `mostrarError`: escribe el texto, pone `aria-invalid="true"` y devuelve el foco al campo |
| C4b | `rediseno-epsel/sitio.js` | **329** | Si la búsqueda tiene menos de 3 letras: "Escribe al menos 3 letras de tu zona o distrito." |

**Qué decir:** "El error no es solo el borde rojo. Hay un texto que explica qué hacer, el campo queda marcado como inválido para el lector de pantalla y el foco vuelve al campo para corregirlo."

### C5. Labels y autocompletar: criterio 8 (WCAG 1.3.5)

| # | Archivo | Líneas | Qué se ve |
|---|---|---|---|
| C5 | `rediseno-epsel/index.html` | **321–323** | `<label for="r-nombre">`, `<input id="r-nombre" autocomplete="name" aria-describedby="r-nombre-error">` y el `<p>` del error |

**Qué decir:** "`for` e `id` unen la etiqueta con el campo: al tocar el texto se enfoca el campo y el lector lo anuncia. `autocomplete="name"` deja que el celular rellene el nombre. `aria-describedby` hace que, si hay error, el lector lo lea junto con el campo."

### C6. HTML semántico, idioma y texto alternativo: criterios 1, 5 y 10

| # | Archivo | Líneas | Qué se ve |
|---|---|---|---|
| C6a | `rediseno-epsel/index.html` | **2** | `<html lang="es-PE">` |
| C6b | `rediseno-epsel/index.html` | **20–32** | `<header>`, el enlace del logo con `aria-label="EPSEL, página de inicio"` y `alt=""` en la imagen, el botón Menú y `<nav aria-label="Principal">` |
| C6c | `rediseno-epsel/index.html` | **95–98** | La foto con `alt="Una cuadrilla de EPSEL con cascos naranjas…"` y el único `<h1>` |

**Qué decir:**
- **Idioma:** "`lang` hace que el lector pronuncie en español."
- **Logo:** "El logo tiene `alt=""` porque el enlace ya tiene nombre. Si no, el lector diría 'EPSEL' dos veces."
- **Foto:** "La foto describe lo que se ve, no dice 'imagen'."
- **Estructura:** "Hay un solo `h1` y regiones `header`, `nav`, `main` y `footer`, para que el lector pueda saltar entre ellas."

### C7. Contraste y tamaño de letra: criterios 2 y 11

| # | Archivo | Líneas | Qué se ve |
|---|---|---|---|
| C7a | `rediseno-epsel/estilos.css` | **10–19** | Cada color con su contraste anotado: `--tinta` 14,6:1, `--c3` 6,7:1… |
| C7b | `rediseno-epsel/estilos.css` | **39–40** | `--t-pequeno` (15 px) y `--t-cuerpo` (17 a 19 px con `clamp`) |

**Qué decir:** "Calculamos el contraste de cada par texto/fondo antes de usarlo; el más bajo es 6,7:1 y la norma pide 4,5:1. La letra del cuerpo nunca baja de 17 px y la más chica, solo para notas, es de 15 px."

### C8. Áreas táctiles: criterio 6 (WCAG 2.5.5)

| # | Archivo | Líneas | Qué se ve |
|---|---|---|---|
| C8 | `rediseno-epsel/estilos.css` | **105–115** | `.boton` con `min-height: 48px` y `.boton-grande` con `min-height: 56px` |

**Qué decir:** "La norma pide 44 px y Android recomienda 48. Todos los botones y enlaces del menú miden 48 px o más, y los principales 56, para tocarlos sin error con el dedo."

### C9. Correcciones por WAVE (sección 4.3 del informe)

| # | Archivo | Líneas | Qué se ve |
|---|---|---|---|
| C9a | `rediseno-epsel/estilos.css` | **212–216** | `.portada-banda` con `background-color: var(--c1)` debajo de la foto |
| C9b | `rediseno-epsel/estilos.css` | **555–556** | `.js .aparece` solo con `transform`, sin `opacity: 0` |
| C9c | `rediseno-epsel/estilos.css` | **564–568** | Con "reducir movimiento" activado no hay animaciones (criterio 2.3.3) |

**Qué decir:** "WAVE marcaba texto blanco sobre fondo claro porque no ve el velo azul de la foto, y texto invisible porque la animación empezaba con opacidad 0. Pusimos un fondo azul real debajo de la foto y la animación ahora solo desliza. Además, quien pide menos movimiento en su sistema no ve animaciones."

---

## App AguaYa (`mobile/src/`)

### C10. Etiquetas para el lector de pantalla (VoiceOver/TalkBack)

| # | Archivo | Líneas | Qué se ve |
|---|---|---|---|
| C10a | `mobile/src/componentes/ui.tsx` | **27** | Botón Volver con `accessibilityRole="button"` y `accessibilityLabel="Volver"` |
| C10b | `mobile/src/componentes/vecino.tsx` | **90–93** | Pestañas con `accessibilityRole="tab"` y `accessibilityState={{ selected }}` |
| C10c | `mobile/src/pantallas/vecino/Calificar.tsx` | **55–57** | Cada estrella como `radio`, con la etiqueta "4 de 5: Buena" |
| C10d | `mobile/src/componentes/iconos.tsx` | **27** | Los íconos ocultos al lector (`importantForAccessibility="no-hide-descendants"`) |

**Qué decir:**
- **Volver:** "El botón Volver es solo una flecha, así que le damos nombre; si no, VoiceOver diría solo 'botón'."
- **Pestañas:** "Dicen 'pestaña, seleccionada'."
- **Estrellas:** "Se anuncian con su significado."
- **Íconos:** "Son decorativos: siempre hay un texto al lado que dice lo mismo, por eso los ocultamos al lector."

### C11. Errores y avisos que se anuncian

| # | Archivo | Líneas | Qué se ve |
|---|---|---|---|
| C11a | `mobile/src/componentes/ui.tsx` | **97** | El error del campo con `accessibilityLiveRegion="polite"` |
| C11b | `mobile/src/componentes/ui.tsx` | **139** | El aviso con `accessibilityRole="alert"` |

**Qué decir:** "Cuando aparece un error, el lector lo lee solo, sin que la persona tenga que buscarlo."

### C12. Tamaño de toque y letra en la app

| # | Archivo | Líneas | Qué se ve |
|---|---|---|---|
| C12 | `mobile/src/tema.ts` | **36–47** | `toque: 56` (alto de los botones) y la escala de letra (cuerpo 17) |

**Qué decir:** "Todos los botones salen del mismo tema y miden 56 puntos de alto. La fuente del cuerpo es Atkinson Hyperlegible, diseñada para personas con baja visión."

---

## Para la diapositiva 03 ("Top 5 criterios")

Usa **C1a, C2, C3c, C4a y C10c**. Son las más fáciles de explicar y se ven bien en grande.
