# Rediseño del sitio de EPSEL (propuesta académica, PA3 de IHM)

Propuesta de rediseño de www.epsel.com.pe hecha a partir del [análisis del sitio actual](../docs/pa3/analisis-sitio-actual.md). El contenido es el real de EPSEL, tomado de su sitio el 5 de noviembre de 2026 y reorganizado; [contenido-epsel.md](../docs/pa3/rediseno/contenido-epsel.md) dice de dónde salió cada parte. **No es el sitio oficial de EPSEL** y lo dice en la cabecera y en el pie de cada página.

## Cómo abrirlo

Abre `index.html` con doble clic: no necesita instalar nada. Es HTML, CSS y JavaScript sin librerías, para que cargue rápido en un celular gama baja.

## Páginas

| Página | Archivo | Qué resuelve | Peso aprox. |
|---|---|---|---|
| Inicio | `index.html` | "¿Hay agua hoy en tu zona?", atajos a lo más buscado, Conoce AguaYa, comunicados de hoy, de dónde viene el agua, pagos, reclamos, información y nosotros | 1,1 MB |
| ¿Pasa hoy la cisterna? | `cisterna.html` | Buscador de zona; la semana día por día con turno, placa y avance; qué hacer cuando llega | 290 KB |
| Cortes y avisos | `avisos.html` | Los comunicados reales en texto, filtrables por localidad, y cómo prepararse para un corte | 270 KB |
| Atención y reclamos | `atencion.html` | Emergencias, 27 oficinas con buscador, cómo reclamar, WhatsApp y denuncias | 220 KB |
| Pagos y trámites | `tramites.html` | Bancos y agentes, oficina virtual, requisitos de conexión nueva, otros trámites y VMA | 200 KB |
| Información pública | `informacion.html` | Cifras de las cisternas por periodo, cumplimiento por zona, transparencia e integridad | 270 KB |
| Nosotros | `nosotros.html` | Misión, visión, historia, valores, alta dirección, datos de la empresa, cobertura y entidades relacionadas | 210 KB |

El peso cuenta HTML, CSS, JavaScript y fotos; la tipografía se carga aparte desde Google Fonts.

- **Enlace directo a una localidad:** `avisos.html?distrito=Motupe` abre los avisos de esa localidad.
- **Cabecera y pie:** son iguales en las siete páginas y los pone `node docs/pa3/rediseno/armar-paginas.mjs`. Si cambias el menú, el pie o las entidades relacionadas, cámbialos en ese script y vuelve a correrlo.

## De dónde salen los datos

| Qué | Fuente |
|---|---|
| Comunicados, oficinas, teléfonos, requisitos, historia y textos | Sitio actual de EPSEL (5 de noviembre de 2026). Los comunicados están en `AVISOS` dentro de `sitio.js` |
| Buscador de zona y "La cisterna esta semana" | Backend de AguaYa: `GET /api/public/transparencia/sectores` |
| La semana en `cisterna.html` | `GET /api/public/transparencia/cronograma` |
| Cifras y tabla en `informacion.html` | `GET /api/public/transparencia/resumen?desde=&hasta=` y `/sectores?desde=&hasta=` |

- **Backend:** `https://aguaya-backend.onrender.com`. Son datos agregados, sin nombres ni direcciones de vecinos.
- **Si el backend no responde en 6 s:** se usan datos de ejemplo y la página lo dice. Con `?datos=ejemplo` se fuerzan, lo que sirve para las pruebas con usuarios.
- **CORS:** el origen desde donde se sirva la página debe estar en `CORS_ORIGINS` del backend.

## Para cambiar en un solo lugar

- **`AGUAYA_WEB` en `sitio.js`:** la URL de la web del vecino de AguaYa. Mientras esté vacía, "Entrar a AguaYa" lleva a "¿Pasa hoy la cisterna?".
- **`WHATSAPP_EPSEL` en `sitio.js`:** el número al que abre el formulario de WhatsApp.
- **`AVISOS` en `sitio.js`:** los comunicados vigentes.

Lo que queda por confirmar con EPSEL está al final de [contenido-epsel.md](../docs/pa3/rediseno/contenido-epsel.md).

## Diseño

- **La forma:** la gota que cae del caño del logo de EPSEL. Es un cuadrado con tres esquinas redondas y una en punta, y se usa en botones, fotos, viñetas, filtros y bloques.
- **La portada:** una foto de una cuadrilla de EPSEL con un velo azul y, encima, una pila de cinco carpetas, una por cada menú. La del frente trae el buscador y los atajos.
- **Las páginas interiores:** migas "Estás en", título grande y una foto real de EPSEL con forma de gota. El menú marca dónde estás.
- **Colores:** cinco azules, de oscuro a claro. Cada carpeta presta su color a su sección.
- **Tipografía:** Schibsted Grotesk.
- **Pocas fotos por página interior:** son páginas de trámite que se abren en celulares gama baja, y cada foto más cuesta datos.

## Imágenes

| Archivo | Origen | Licencia |
|---|---|---|
| `assets/logo-epsel.webp` | Logo de EPSEL S.A. (fondo exterior quitado) | Marca de EPSEL |
| `assets/epsel-*.webp` (cuadrilla, cisterna, obra, equipo, grifo, conexión) | Fotos de la portada de www.epsel.com.pe | Imágenes de EPSEL |
| `assets/tinajones.webp` | «Reservorio Tinajones», EdsonFuentesMera | CC BY-SA 4.0 (Wikimedia Commons) |
| `assets/tinajones-orilla.webp` | «20250630 074913 Reservorio de Tinajones», Josefito123 | CC BY-SA 4.0 (Wikimedia Commons) |
| `assets/chiclayo-plaza.webp` | «Chiclayo, Plaza de Armas», Burkhard Mücke | CC BY-SA 4.0 (Wikimedia Commons) |

Las imágenes de EPSEL se usan solo dentro de esta propuesta académica, que rediseña su propio sitio. Todas se redimensionaron y se pasaron a WebP; los detalles están en `assets/credits.json`.
