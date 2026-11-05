# Línea base de accesibilidad: sitio actual de EPSEL

Evidencia del **antes** para el PA3 de IHM. El **después** se medirá igual sobre el rediseño.

## Método

- **Herramienta:** axe-core 4.10.3 vía `@axe-core/playwright`. El script está en [`analizar.mjs`](analizar.mjs).
- **Navegador:** Chrome 156 sin interfaz, ventana de 1366×900, idioma es-PE.
- **Reglas:** WCAG 2.0 y 2.1, niveles A y AA, más las buenas prácticas de axe.
- **Fecha:** 4 nov 2026, 20:55 (hora de Lima).
- **Archivos en [`axe/`](axe/):** un `.axe.json` resumido por página (regla, impacto, criterio WCAG y elementos afectados) y una captura `.png` de lo que se ve al cargar. [`axe/resumen.json`](axe/resumen.json) tiene los totales.
- **WAVE:** lo corre el equipo desde el navegador. Sus capturas van en `wave/`.

## Resultados

| Página | URL | Reglas violadas | Elementos | Críticas | Serias | Moderadas |
|---|---|---:|---:|---:|---:|---:|
| Portada | `/sue/` | 9 | 100 | 1 | 6 | 2 |
| Portal de transparencia | `/sue/PortalTransparencia.aspx` | 7 | 48 | 1 | 3 | 3 |
| Rendición de cuentas | `/sue/PortalTransparenciaRendicion.aspx` | 8 | 46 | 2 | 4 | 2 |
| Indicadores de gestión | `/sue/PortalTransparenciaIndicador.aspx` | 7 | 46 | 1 | 4 | 2 |
| Servicio de agua potable | `/sue/PortalAguaPotable.aspx` | 7 | 73 | 1 | 3 | 3 |
| Centros de atención | `/sue/PortalAtencion.aspx` | 7 | 65 | 1 | 3 | 3 |

**Fallas que se repiten en todas las páginas:**

| Regla de axe | Criterio WCAG | Qué significa |
|---|---|---|
| `image-alt` (crítica) | 1.1.1 | Imágenes sin texto alternativo; el lector de pantalla no puede leerlas |
| `color-contrast` (seria) | 1.4.3 | De 24 a 53 elementos por página con contraste insuficiente: menú, pie y tarjetas |
| `link-name` (seria) | 2.4.4 / 4.1.2 | Enlaces sin texto; entre ellos los cuadritos de redes sociales |
| `html-has-lang` (seria) | 3.1.1 | La página no declara que está en español; el lector la pronuncia mal |
| `region` / `landmark-one-main` (moderada) | 1.3.1 | Sin estructura de `main` ni regiones; no se puede saltar al contenido |
| `heading-order` (moderada) | 1.3.1 | Saltos en los niveles de títulos |

**Fallas que solo aparecen en algunas páginas:**

| Regla de axe | Criterio WCAG | Dónde |
|---|---|---|
| `select-name` (crítica) | 4.1.2 | Rendición de cuentas: el selector de año no tiene nombre |
| `frame-title` (seria) | 4.1.2 | Rendición, Indicadores y portada |
| `aria-command-name` y `aria-prohibited-attr` (serias) | 4.1.2 | Portada |

## Peso de la página

Medido con `curl` el 4 nov 2026.

- **La portada (`/sue/`) descarga unos 85 MB y tardó entre 37 y 45 s** con buena conexión.
- **El motivo:** el campo oculto `__VIEWSTATE` de ASP.NET ocupa unos 47 MB dentro del HTML. Es texto base64, así que la compresión gzip casi no lo reduce.
- **Para el vecino de AA.HH. con plan prepago,** cada visita gasta una parte importante de sus datos.

## Hallazgos manuales

- **Error del servidor con la traza de la pila a la vista** en Indicadores de gestión (captura del equipo). Expone rutas internas y la versión de .NET.
- **Carruseles que avanzan solos** (WCAG 2.2.2) y **horarios publicados como imagen** (1.4.5).
- **Jerga institucional en el menú:** VMA, PMO, "Ciclo Operacional".
- **Seguimiento de cisternas (DANA):** pendiente de analizar con axe; falta su URL.
