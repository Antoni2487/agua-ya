// Pone la misma cabecera (con el menú) y el mismo pie en las siete páginas del rediseño,
// marcando en el menú la página actual. El sitio es HTML estático sin compilación: este script
// solo evita copiar la cabecera a mano. Uso: node docs/pa3/rediseno/armar-paginas.mjs
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const carpeta = join(dirname(fileURLToPath(import.meta.url)), '..', '..', '..', 'rediseno-epsel');

const MENU = [
  ['agua', 'Agua en tu zona', [
    ['avisos.html', 'Cortes y avisos'],
    ['cisterna.html', '¿Pasa hoy la cisterna?'],
    ['index.html#aguaya', 'AguaYa: la cisterna en tu celular'],
    ['index.html#recorrido', 'De dónde viene tu agua'],
    ['index.html#limpieza', 'Cómo limpiamos el agua'],
  ]],
  ['pagos', 'Pagos y trámites', [
    ['tramites.html', 'Dónde pagar tu recibo'],
    ['tramites.html#oficina-virtual', 'Oficina virtual: duplicado y saldo'],
    ['tramites.html#tramites', 'Conexión nueva y otros trámites'],
    ['tramites.html#vma', 'Para empresas: desagüe no doméstico (VMA)'],
  ]],
  ['atencion', 'Atención y reclamos', [
    ['atencion.html', 'Oficinas y teléfonos'],
    ['atencion.html#emergencias', 'Emergencias: fugas y atoros'],
    ['atencion.html#reclamo', 'Cómo hacer un reclamo'],
    ['atencion.html#escribenos', 'Escríbenos por WhatsApp'],
    ['atencion.html#denuncias', 'Denuncias y sugerencias'],
  ]],
  ['informacion', 'Información pública', [
    ['informacion.html', 'Las cisternas en cifras'],
    ['informacion.html#zonas', 'Cumplimiento por zona'],
    ['informacion.html#transparencia', 'Portal de Transparencia'],
    ['informacion.html#pedir-informacion', 'Pedir información pública'],
    ['informacion.html#integridad', 'Integridad y denuncias de corrupción'],
  ]],
  ['nosotros', 'Nosotros', [
    ['nosotros.html', 'Quiénes somos'],
    ['nosotros.html#historia', 'Historia'],
    ['nosotros.html#direccion', 'Alta dirección'],
    ['nosotros.html#cobertura', 'Dónde damos servicio'],
    ['nosotros.html#entidades', 'Entidades relacionadas'],
  ]],
];

const PIE = [
  ['avisos.html', 'Cortes y avisos'],
  ['cisterna.html', '¿Pasa hoy la cisterna?'],
  ['tramites.html', 'Dónde pagar'],
  ['atencion.html', 'Oficinas y teléfonos'],
  ['atencion.html#reclamo', 'Hacer un reclamo'],
  ['informacion.html', 'Información pública'],
  ['nosotros.html', 'Nosotros'],
];

// Entidades que el sitio actual muestra en "Entidades relacionadas", con una línea que dice qué hace cada una
const ENTIDADES = [
  ['https://www.sunass.gob.pe/', 'SUNASS', 'Regula y supervisa el servicio de agua'],
  ['https://web.otass.gob.pe/', 'OTASS', 'Conduce a EPSEL en el Régimen de Apoyo Transitorio'],
  ['https://www.gob.pe/vivienda', 'Ministerio de Vivienda', 'Dirige el sector saneamiento'],
  ['https://www.ana.gob.pe/', 'ANA', 'Autoridad Nacional del Agua'],
  ['https://www.gob.pe/', 'gob.pe', 'Trámites y servicios del Estado'],
];

// En la portada los enlaces a la misma página quedan como "#ancla"
const enlace = (href, pagina) => (pagina === 'index.html' && href.startsWith('index.html#') ? href.slice('index.html'.length) : href);
const actual = (href, pagina) => (href === pagina ? ' aria-current="page"' : '');

function cabecera(pagina) {
  const menus = MENU.map(([id, titulo, opciones]) => {
    const aqui = opciones.some(([href]) => href === pagina);
    const items = opciones.map(([href, texto]) => `<li><a href="${enlace(href, pagina)}"${actual(href, pagina)}>${texto}</a></li>`).join('\n              ');
    return `          <li class="menu-item">
            <button class="menu-boton${aqui ? ' menu-actual' : ''}" type="button" aria-expanded="false" aria-controls="sub-${id}">${titulo}</button>
            <ul class="submenu" id="sub-${id}">
              ${items}
            </ul>
          </li>`;
  }).join('\n');
  const cisterna = pagina === 'cisterna.html' ? '#buscar' : 'cisterna.html';
  return `<header class="cabecera">
    <p class="aviso-propuesta">Propuesta académica de rediseño (UTP). No es el sitio oficial de EPSEL.</p>
    <div class="cabecera-fila">
      <a class="marca" href="index.html" aria-label="EPSEL, página de inicio">
        <img class="marca-logo" src="assets/logo-epsel.webp" width="358" height="247" alt="">
        <span class="marca-detalle">Agua potable y alcantarillado de Lambayeque</span>
      </a>

      <button class="boton-menu" type="button" aria-expanded="false" aria-controls="navegacion">
        <span class="boton-menu-icono" aria-hidden="true"></span>Menú
      </button>

      <nav id="navegacion" class="navegacion" aria-label="Principal">
        <ul class="menu">
${menus}
        </ul>
      </nav>

      <a class="boton boton-cisterna" href="${cisterna}">¿Pasa hoy la cisterna?</a>
    </div>
  </header>`;
}

function pie(pagina) {
  const marca = readFileSync(join(carpeta, 'index.html'), 'utf8').match(/<svg class="pie-marca"[\s\S]*?<\/svg>/)[0];
  const enlaces = PIE.map(([href, texto]) => `<li><a href="${enlace(href, pagina)}"${actual(href, pagina)}>${texto}</a></li>`).join('\n          ');
  return `<footer class="pie">
    ${marca}
    <div class="pie-columnas">
      <nav aria-label="Pie de página">
        <ul class="pie-enlaces">
          ${enlaces}
        </ul>
      </nav>
      <div class="pie-contacto">
        <p><strong>EPSEL S.A.</strong> Empresa Prestadora de Servicios de Saneamiento de Lambayeque. RUC 20103448591.</p>
        <p>Atención al cliente: Av. Grau 451, Chiclayo · <a href="tel:+5174273609">(074) 273609</a></p>
        <p>Emergencias: <a href="tel:+51932070554">932 070 554</a> · <a href="tel:+51942413905">942 413 905</a></p>
        <p>WhatsApp #TeEscucha: <a href="https://api.whatsapp.com/send/?phone=51942813797" rel="noopener">942 813 797</a></p>
      </div>
    </div>
    <nav class="pie-entidades" aria-labelledby="pie-entidades-titulo">
      <h2 id="pie-entidades-titulo">Entidades relacionadas</h2>
      <ul>
        ${ENTIDADES.map(([href, nombre, texto]) => `<li><a href="${href}" rel="noopener"><strong>${nombre}</strong><span>${texto}</span></a></li>`).join('\n        ')}
      </ul>
    </nav>
    <div class="pie-final">
      <p>Propuesta académica de rediseño, curso Interacción Humano-Computador, UTP Chiclayo. No es el sitio oficial de EPSEL. Contenido tomado de www.epsel.com.pe el 5 de noviembre de 2026.</p>
      <a class="boton boton-pie" href="https://www.epsel.com.pe/Intranet/WFrmLogueo.aspx" rel="noopener">Acceso del personal</a>
    </div>
    <p class="creditos">Fotos de obras, cisternas y equipo: EPSEL S.A. · «Reservorio Tinajones», EdsonFuentesMera, y «Reservorio de Tinajones», Josefito123, CC BY-SA 4.0 · «Chiclayo, Plaza de Armas», Burkhard Mücke, CC BY-SA 4.0; todas vía Wikimedia Commons.</p>
  </footer>`;
}

for (const pagina of ['index.html', 'cisterna.html', 'avisos.html', 'informacion.html', 'atencion.html', 'tramites.html', 'nosotros.html']) {
  const ruta = join(carpeta, pagina);
  const piePagina = pie(pagina);
  const html = readFileSync(ruta, 'utf8')
    .replace(/<header class="cabecera">[\s\S]*?<\/header>/, cabecera(pagina))
    .replace(/<footer class="pie">[\s\S]*?<\/footer>/, piePagina);
  writeFileSync(ruta, html);
  console.log(`listo: ${pagina}`);
}
