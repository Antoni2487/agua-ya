# Rediseño del sitio de EPSEL (propuesta académica, PA3 de IHM)

Propuesta de rediseño de la portada de www.epsel.com.pe hecha a partir del [análisis del sitio actual](../docs/pa3/analisis-sitio-actual.md). **No es el sitio oficial de EPSEL** y lo dice en la cabecera y en el pie.

## Cómo abrirlo

Abre `index.html` con doble clic: no necesita instalar nada. Es HTML, CSS y JavaScript sin librerías, para que cargue rápido en un celular gama baja (≈ 0,8 MB).

## De dónde salen los datos

- **"¿Hay agua hoy en tu zona?" y "La cisterna esta semana"** consultan el backend de AguaYa (`GET /api/public/transparencia/sectores`, en `https://aguaya-backend.onrender.com`).
- Si no responde en 6 s, se usan **datos de ejemplo** y la página lo dice. Con `index.html?datos=ejemplo` se fuerzan los de ejemplo, útil para las pruebas con usuarios.
- Para que el navegador acepte los datos reales, el origen desde donde se sirva la página debe estar en `CORS_ORIGINS` del backend.
- **Los "Cortes y avisos de hoy" son de ejemplo:** EPSEL los publica como comunicados y AguaYa no los maneja.

## Datos por confirmar antes de mostrarlo como definitivo

- Teléfono, direcciones de oficinas y número de WhatsApp de EPSEL. Hoy el formulario abre WhatsApp sin un número fijo.
- Horario de atención (lunes a viernes, 8:00 a.m. a 4:30 p.m.).
- El enlace "Acceso del personal".

## Diseño

- **La forma:** la gota que cae del caño del logo de EPSEL, que también es el pin de "tu zona". Es un cuadrado con tres esquinas redondas y una en punta, y se usa en botones, fotos, viñetas, pestañas y bloques de cifras.
- **La portada:** una pila de cinco carpetas, una por cada menú. La del frente, "Agua en tu zona", trae el buscador.
- **Colores:** cinco azules, de oscuro a claro, y cada carpeta presta su color a su sección.
- **Tipografía:** Schibsted Grotesk.

## Fotos (todas vía Wikimedia Commons)

| Archivo | Título y autor | Licencia |
|---|---|---|
| `assets/tinajones.webp` | «Reservorio Tinajones», EdsonFuentesMera | CC BY-SA 4.0 |
| `assets/tinajones-orilla.webp` | «20250630 074913 Reservorio de Tinajones», Josefito123 | CC BY-SA 4.0 |
| `assets/cano.webp` | «Zapallal - water tap», SuSanA Secretariat (Lima, no Lambayeque) | CC BY 2.0 |
| `assets/chiclayo-plaza.webp` | «Chiclayo, Plaza de Armas», Burkhard Mücke | CC BY-SA 4.0 |
| `assets/monsefu-plaza.webp` | «Plaza de Armas de Monsefú», LLs | CC BY-SA 4.0 |

Las fotos se redimensionaron a 1100 px y se pasaron a WebP. Las fuentes completas están en `assets/credits.json`.
