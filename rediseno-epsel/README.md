# Rediseño del sitio de EPSEL (propuesta académica, PA3 de IHM)

Propuesta de rediseño de www.epsel.com.pe hecha a partir del [análisis del sitio actual](../docs/pa3/analisis-sitio-actual.md). **No es el sitio oficial de EPSEL** y lo dice en la cabecera y en el pie de cada página.

## Cómo abrirlo

Abre `index.html` con doble clic: no necesita instalar nada. Es HTML, CSS y JavaScript sin librerías, para que cargue rápido en un celular gama baja.

## Páginas

| Página | Archivo | Qué resuelve | Peso aprox. |
|---|---|---|---|
| Inicio | `index.html` | "¿Hay agua hoy en tu zona?", avisos de hoy, de dónde viene el agua, pagos, reclamos, información y nosotros | 730 KB |
| ¿Pasa hoy la cisterna? | `cisterna.html` | Buscador de zona, la semana día por día con turno, placa y avance, qué hacer cuando llega | 200 KB |
| Cortes y avisos | `avisos.html` | Avisos de hoy y de los próximos días, filtro por distrito, qué significa cada aviso, cómo prepararse | 160 KB |
| Información pública | `informacion.html` | Cifras de las cisternas por periodo, cumplimiento por zona, documentos y cómo pedir información | 240 KB |

El peso cuenta HTML, CSS, JavaScript y fotos; la tipografía se carga aparte desde Google Fonts.

- **Enlaces directos:** `avisos.html?distrito=Monsefú` abre los avisos de ese distrito, y es el enlace que da el buscador.
- **Cabecera y pie:** son iguales en las cuatro páginas y los pone `node docs/pa3/rediseno/armar-paginas.mjs`. Si cambias el menú, cámbialo en ese script y vuelve a correrlo.

## De dónde salen los datos

Las páginas consultan el panel público de transparencia del backend de AguaYa (`https://aguaya-backend.onrender.com`). Son datos agregados, sin nombres ni direcciones de vecinos.

| Dónde | Endpoint |
|---|---|
| Buscador, "La cisterna esta semana" de la portada | `GET /api/public/transparencia/sectores` |
| La semana en `cisterna.html` | `GET /api/public/transparencia/cronograma` |
| Cifras y tabla en `informacion.html` | `GET /api/public/transparencia/resumen?desde=&hasta=` y `/sectores?desde=&hasta=` |

- **Si el backend no responde en 6 s**, cada página usa datos de ejemplo y lo dice en pantalla.
- **Datos de ejemplo a la fuerza:** `?datos=ejemplo` en cualquier página. Sirve para las pruebas con usuarios.
- **CORS:** para que el navegador acepte los datos reales, el origen desde donde se sirva la página debe estar en `CORS_ORIGINS` del backend.
- **Los cortes y avisos son siempre de ejemplo** (lista `AVISOS` en `sitio.js`): EPSEL los publica como comunicados y AguaYa no los maneja.

## Datos por confirmar antes de mostrarlo como definitivo

- Teléfono, direcciones de oficinas y número de WhatsApp de EPSEL. Hoy el formulario abre WhatsApp sin un número fijo.
- Horario de atención (lunes a viernes, 8:00 a.m. a 4:30 p.m.).
- Enlaces a los documentos de información pública (metas, rendición de cuentas, gestión) y el formulario o correo de mesa de partes.
- El enlace "Acceso del personal".

## Diseño

- **La forma:** la gota que cae del caño del logo de EPSEL, que también es el pin de "tu zona". Es un cuadrado con tres esquinas redondas y una en punta, y se usa en botones, fotos, viñetas, pestañas, filtros y bloques de cifras.
- **La portada:** una pila de cinco carpetas, una por cada menú. La del frente, "Agua en tu zona", trae el buscador.
- **Las páginas interiores:** migas "Estás en", título grande y una foto con forma de gota. El menú marca dónde estás.
- **Colores:** cinco azules, de oscuro a claro. Cada carpeta presta su color a su sección.
- **Tipografía:** Schibsted Grotesk.
- **Una foto por página interior:** son páginas de trámite que se abren en celulares gama baja, y cada foto más cuesta datos.

## Fotos

| Archivo | Título y autor | Licencia |
|---|---|---|
| `assets/tinajones.webp` | «Reservorio Tinajones», EdsonFuentesMera | CC BY-SA 4.0 (Wikimedia Commons) |
| `assets/tinajones-orilla.webp` | «20250630 074913 Reservorio de Tinajones», Josefito123 | CC BY-SA 4.0 (Wikimedia Commons) |
| `assets/cano.webp` | «Zapallal - water tap», SuSanA Secretariat (Lima, no Lambayeque) | CC BY 2.0 (Wikimedia Commons) |
| `assets/chiclayo-plaza.webp` | «Chiclayo, Plaza de Armas», Burkhard Mücke | CC BY-SA 4.0 (Wikimedia Commons) |
| `assets/monsefu-plaza.webp` | «Plaza de Armas de Monsefú», LLs | CC BY-SA 4.0 (Wikimedia Commons) |
| `assets/cisterna-baldes.webp` | «Ministerio de Defensa y Ejército distribuyen agua en San Juan de Lurigancho», Ministerio de Defensa del Perú (Lima, no Lambayeque) | CC BY 2.0 (Flickr) |

Las fotos se redimensionaron a 1100 px o menos y se pasaron a WebP. Las fuentes completas están en `assets/credits.json`. Si EPSEL tiene fotos propias de sus cisternas en Chiclayo, conviene reemplazar las de Lima.
