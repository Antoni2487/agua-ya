# Rediseño del sitio de EPSEL: resultados de axe y pruebas

**Qué se probó:** las cuatro páginas del rediseño ([rediseno-epsel/](../../../rediseno-epsel/): portada, ¿Pasa hoy la cisterna?, Cortes y avisos e Información pública) con axe-core 4.10 (reglas WCAG 2.0, 2.1 y 2.2 A/AA + buenas prácticas), en Chrome, el 5 de noviembre de 2026. Es la misma herramienta y el mismo navegador de la [línea base del sitio actual](../linea-base/README.md).

**Cómo repetirlo:** `node probar-rediseno.mjs file:///…/rediseno-epsel/index.html <carpeta-salida>` (portada) y `node probar-paginas.mjs file:///…/rediseno-epsel/ <carpeta-salida>` (las otras tres). Los reportes de axe están en [axe/](axe/).

## Antes y después (portada)

| Medida | Sitio actual (línea base) | Rediseño |
|---|---|---|
| Reglas de axe violadas | 9 | **0** |
| Elementos con fallas | 100 | **0** |
| Fallas críticas / graves | 1 / 6 | **0 / 0** |
| Enlace "Ir al contenido principal" | No | **Sí** (primer Tab) |
| Idioma de la página (`lang`) | No | **Sí** (`es-PE`) |
| Menú se abre con el teclado | No | **Sí** (Enter o Espacio; Esc cierra y devuelve el foco) |
| Controles menores de 44 px (celular) | 34 de 54 | **0** |
| Textos menores de 14 px | 0 | **0** (el cuerpo es de 17–19 px) |
| Peso de la página | ≈ 85 MB | **≈ 0,8 MB** (fotos WebP + tipografía) |
| Opciones del menú | 35 en 6 menús | **23 en 5 menús** |

axe se corrió en cinco estados: escritorio (1440 px), escritorio con el menú abierto, escritorio con un resultado de búsqueda, celular (390 px) y celular con el menú abierto. En los cinco: **0 violaciones**.

### Lo que axe deja "a revisar a mano" (revisado)

- **Contraste del formulario del cupón (6 elementos):** axe no pudo calcular el fondo por los círculos de corte del cupón. Revisado: texto `#0b2545` sobre `#e1eef9`, **13,5:1**.
- **Letras del logo del pie (5):** letras sueltas dibujadas en SVG. Revisado: `#f4f8fb` sobre azules de `#0b2545` a `#0a58a8`, **6,7:1 o más**.

## Pruebas funcionales de la portada (todas pasan)

- El primer Tab enfoca "Ir al contenido principal".
- El menú abre con Enter y con Espacio, marca `aria-expanded`, deja entrar con Tab a sus opciones y se cierra con Esc devolviendo el foco al botón.
- Buscador: una zona que no existe muestra un error con texto y `aria-invalid`; "cerropon" (sin tilde) encuentra Cerropón; el resultado se anuncia con `role="status"`.
- El acordeón "De dónde viene tu agua" abre una etapa y cierra la anterior (`aria-expanded` + `hidden`).
- Formulario vacío: mensajes de error con texto junto a cada campo y foco en el primero.
- Celular: sin desplazamiento horizontal; el botón "Menú" abre la navegación y cada menú funciona como acordeón.

## Criterios WCAG que se pueden mostrar en la portada

| Criterio | Dónde se ve |
|---|---|
| 1.1.1 Contenido no textual | Cada foto con `alt` que la describe; el dibujo de las lagunas con `title`; viñetas decorativas con `aria-hidden` |
| 1.3.1 Información y relaciones | `header`, `nav` con nombre, `main`, `footer`; títulos en orden; listas reales; `label` en cada campo |
| 1.4.1 Uso del color | Cada aviso dice su tipo con texto ("Corte de agua", "Poca presión"); el color de la gota solo acompaña |
| 1.4.3 Contraste | Todos los pares ≥ 4,5:1 (anotados en `estilos.css`) |
| 1.4.4 / 1.4.10 Cambio de tamaño y reajuste | Unidades `rem`, sin desplazamiento horizontal a 390 px |
| 2.1.1 Teclado | Menú, acordeón, buscador y formulario funcionan solo con teclado |
| 2.4.1 Saltar bloques | "Ir al contenido principal" |
| 2.4.7 Foco visible | Anillo amarillo con borde oscuro en todo lo que se puede tocar |
| 2.5.8 Tamaño del objetivo | Controles de 48 px o más (botones grandes de 56 px) |
| 3.1.1 Idioma de la página | `lang="es-PE"` |
| 3.3.1 / 3.3.2 Errores y etiquetas | Etiquetas visibles, campos obligatorios marcados, errores con texto y `aria-describedby` |
| 2.3.3 Movimiento | Con "reducir movimiento" activado no hay animaciones |

## Páginas interiores

| Página | Estados probados con axe | Violaciones | Peso aprox. |
|---|---|---|---|
| ¿Pasa hoy la cisterna? (`cisterna.html`) | escritorio, escritorio con resultado de búsqueda, celular, celular con menú abierto | **0** | 200 KB |
| Cortes y avisos (`avisos.html`) | escritorio, celular, celular con menú abierto | **0** | 160 KB |
| Información pública (`informacion.html`) | escritorio, celular, celular con menú abierto | **0** | 240 KB |

Las tres se abren con `?datos=ejemplo` para que el resultado no dependa del backend.

### Pruebas funcionales (todas pasan)

- **En las tres:**
  - un solo `h1`;
  - las migas "Estás en" y el menú marcan la página actual con `aria-current="page"`;
  - el primer Tab enfoca "Ir al contenido principal";
  - no hay errores de JavaScript.
- **¿Pasa hoy la cisterna?:**
  - la semana muestra 7 días, empezando por "Hoy";
  - cada visita trae su placa;
  - el filtro por distrito deja solo ese distrito, marca `aria-pressed="true"` y anuncia la cuenta con `role="status"`;
  - el buscador encuentra "atusparias" y enlaza a los avisos de su distrito;
  - con menos de 3 letras muestra un error con `aria-invalid`.
- **Cortes y avisos:**
  - el filtro deja solo los avisos del distrito;
  - si no hay avisos, lo dice con texto;
  - `avisos.html?distrito=Monsefú` abre ya filtrado.
- **Información pública:**
  - la tabla muestra las 5 zonas, cada fila con su encabezado (`th scope="row"`);
  - cambiar el periodo (7, 30 o 90 días) cambia las cifras y se anuncia a los lectores de pantalla.
- **Celular (390 px), en las tres:**
  - sin desplazamiento horizontal de la página;
  - 0 controles menores de 44 px;
  - 0 textos menores de 14 px.

### Lo que axe deja "a revisar a mano" (revisado)

- **Con el menú abierto en celular:** el panel tapa el contenido de abajo y axe no puede medir su contraste. Es el mismo contenido que pasa con el menú cerrado.
- **Letras del logo del pie:** igual que en la portada, **6,7:1 o más**.

### Criterios WCAG que suman las páginas interiores

| Criterio | Dónde se ve |
|---|---|
| 1.3.1 Información y relaciones | Tabla con `caption`, `th scope="col"` y `th scope="row"`; cada día de la semana es una sección con su título |
| 1.4.1 Uso del color | El estado de cada visita va en texto ("Repartiendo ahora", "Programada"); la barra de avance y el color de la gota solo acompañan |
| 1.4.10 Reajuste | En celular, la tabla se desliza dentro de su caja (región con nombre y enfocable con Tab), nunca la página |
| 2.4.8 Ubicación | Migas "Estás en" y menú con la página actual marcada |
| 4.1.2 Nombre, función y valor | Filtros como botones con `aria-pressed` |
| 4.1.3 Mensajes de estado | Cantidad de avisos y visitas, y periodo de las cifras, con `role="status"` |
