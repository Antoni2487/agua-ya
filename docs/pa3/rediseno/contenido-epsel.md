# Contenido real de EPSEL en el rediseño

**Qué es:** de dónde salió cada contenido del rediseño y dónde quedó. El texto se tomó de www.epsel.com.pe el 5 de noviembre de 2026, recorriendo las 76 páginas enlazadas desde su portada.

## Cómo se reorganizó

| Menú del sitio actual (6 menús, 35 opciones) | Dónde quedó en el rediseño (5 menús) |
|---|---|
| Aviso emergente "Central de atención virtual" (DANA, en `eps.center`) | Anuncio fijo **Conoce AguaYa** en la portada, sin tapar la página |
| Carrusel de comunicados (imágenes sin texto alternativo) | **Cortes y avisos** (`avisos.html`): cada comunicado en texto, con lugar, fechas y qué hacer, filtrable por localidad |
| Orientación al Cliente › Centros de Atención | **Atención y reclamos › Oficinas y teléfonos** (`atencion.html`): 27 oficinas con buscador |
| Orientación al Cliente › Requisitos para Realizar Reclamo / Procedimiento del Reclamo | **Atención y reclamos › Cómo hacer un reclamo** (4 pasos) |
| Botón "Tu gerente te escucha #TeEscucha" | **Escríbenos por WhatsApp** (formulario que abre WhatsApp con el 942 813 797) |
| Consultas y trámite online › Denuncias o sugerencias | **Atención y reclamos › Denuncias y sugerencias** |
| Orientación al Cliente › Centro de Recaudación / Pago virtual | **Pagos y trámites › Dónde pagar** (`tramites.html`) |
| Consultas y trámite online › Oficina Virtual | **Pagos y trámites › Oficina virtual** (enlaces a los servicios reales) |
| Orientación al Cliente › Requisitos para Trámite | **Pagos y trámites › Conexión nueva y otros trámites** |
| VMA (4 páginas) | **Pagos y trámites › Desagüe no doméstico (VMA)** |
| Ciclo Operacional (Agua Potable, Alcantarillado, Potabilización) | **Agua en tu zona › De dónde viene tu agua** (acordeón de la portada, con las cifras reales) |
| Nosotros (Historia, Misión y Visión, Compromiso, Valores, Alta Dirección, Información General, Área de Cobertura) | **Nosotros** (`nosotros.html`) |
| Bloque de OTASS y "Entidades relacionadas" | **Nosotros › Entidades relacionadas** y el pie de todas las páginas |
| Transparencia (botón, menú Gobernanza y pie) | **Información pública** (`informacion.html`): un solo lugar, con enlace al Portal de Transparencia Estándar |
| Integridad | **Información pública › Integridad** |
| Intranet / Iniciar sesión | **Acceso del personal** en el pie |

**Se quitó:**
- los cinco cuadritos de redes sociales sin texto, que enlazan a las portadas genéricas de Facebook, Instagram, Twitter, YouTube y Gmail;
- "Aplicaciones Web" (un instalador `.exe` de SICDESA y el correo interno), que es para el personal;
- los carruseles automáticos.

## Hallazgos nuevos para el informe

Estos se suman a los del [análisis del sitio actual](../analisis-sitio-actual.md):

| Hallazgo | Dónde |
|---|---|
| "Procedimiento del Reclamo" y "Área de Cobertura" están **vacías**: solo tienen el título | `PortalProcedimiento.aspx`, `PortalAmbito.aspx` |
| Las páginas de VMA copian texto de SEDAPAL (Lima) sin adaptarlo: dicen "los costos de mantenimiento… que asume SEDAPAL" | `PortalVMAControl.aspx` |
| "Transparencia Estándar" del pie da **error 404** con la versión de .NET a la vista | `PortalTransparenciaEstandar.aspx` |
| Las direcciones y teléfonos de las oficinas están dentro de paneles cerrados: solo se ven si se abre cada uno | `PortalAtencion.aspx` |
| El teléfono de la oficina de Íllimo tiene un dígito de más (9424615606) | `PortalAtencion.aspx` |
| Los horarios por turnos y los cortes se publican **solo como imágenes** | Portada, carrusel de comunicados |

## Lo que queda por confirmar con EPSEL

- Teléfono correcto de la oficina de Íllimo.
- Requisitos de cada trámite distinto de la conexión nueva. En el sitio actual están en paneles que no se pudieron leer.
- URL de la web del vecino de AguaYa, para el botón "Entrar a AguaYa" (`AGUAYA_WEB` en `sitio.js`).
- Formulario o correo de mesa de partes para pedir información pública.
- Permiso para usar el logo y las fotos fuera del curso. El rediseño es académico y lo dice en cada página.
