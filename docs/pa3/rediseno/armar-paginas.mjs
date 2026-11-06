// Pone la misma cabecera (con el menú) y el mismo pie en las cuatro páginas del rediseño,
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
    ['cisterna.html#semana', 'La cisterna esta semana'],
    ['index.html#recorrido', 'De dónde viene tu agua'],
    ['index.html#limpieza', 'Cómo limpiamos el agua'],
  ]],
  ['pagos', 'Pagos y trámites', [
    ['index.html#donde-pagar', 'Dónde pagar tu recibo'],
    ['index.html#oficina-virtual', 'Oficina virtual'],
    ['index.html#tramites', 'Trámites: nueva conexión y otros'],
    ['index.html#vma', 'Para empresas: desagüe no doméstico (VMA)'],
  ]],
  ['atencion', 'Atención y reclamos', [
    ['index.html#reclamo', 'Cómo hacer un reclamo'],
    ['index.html#oficinas', 'Oficinas y horarios'],
    ['index.html#escribenos', 'Escríbenos por WhatsApp'],
    ['index.html#denuncias', 'Denuncias y sugerencias'],
  ]],
  ['informacion', 'Información pública', [
    ['informacion.html', 'Las cisternas en cifras'],
    ['informacion.html#zonas', 'Cumplimiento por zona'],
    ['informacion.html#metas', 'Metas y resultados'],
    ['informacion.html#rendicion', 'Rendición de cuentas'],
    ['informacion.html#pedir-informacion', 'Pedir información pública'],
    ['informacion.html#integridad', 'Integridad y denuncias de corrupción'],
  ]],
  ['nosotros', 'Nosotros', [
    ['index.html#quienes-somos', 'Quiénes somos'],
    ['index.html#directivos', 'Directivos y organización'],
    ['index.html#cobertura', 'Dónde damos servicio'],
    ['index.html#noticias', 'Noticias'],
  ]],
];

const PIE = [
  ['avisos.html', 'Cortes y avisos'],
  ['cisterna.html', '¿Pasa hoy la cisterna?'],
  ['index.html#donde-pagar', 'Dónde pagar'],
  ['index.html#reclamo', 'Hacer un reclamo'],
  ['informacion.html', 'Información pública'],
  ['index.html#nosotros', 'Nosotros'],
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
        <svg class="marca-gota" viewBox="0 0 32 32" aria-hidden="true" focusable="false"><path d="M16 2C10 10 5 15.5 5 21a11 11 0 0 0 22 0C27 15.5 22 10 16 2z"/></svg>
        <span class="marca-nombre">EPSEL</span>
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
        <p><strong>EPSEL S.A.</strong> Empresa Prestadora de Servicios de Saneamiento de Lambayeque.</p>
        <p>Supervisada por SUNASS.</p>
      </div>
    </div>
    <div class="pie-final">
      <p>Propuesta académica de rediseño, curso Interacción Humano-Computador, UTP Chiclayo. No es el sitio oficial de EPSEL. Avisos, horarios y cifras de ejemplo cuando el sistema de cisternas no responde.</p>
      <a class="boton boton-pie" href="#">Acceso del personal</a>
    </div>
    <p class="creditos">Fotos: «Reservorio Tinajones», EdsonFuentesMera, CC BY-SA 4.0 · «Reservorio de Tinajones», Josefito123, CC BY-SA 4.0 · «Zapallal: water tap», SuSanA Secretariat, CC BY 2.0 · «Chiclayo, Plaza de Armas», Burkhard Mücke, CC BY-SA 4.0 · «Plaza de Armas de Monsefú», LLs, CC BY-SA 4.0, vía Wikimedia Commons · «Ministerio de Defensa y Ejército distribuyen agua en San Juan de Lurigancho», Ministerio de Defensa del Perú, CC BY 2.0, vía Flickr.</p>
  </footer>`;
}

for (const pagina of ['index.html', 'cisterna.html', 'avisos.html', 'informacion.html']) {
  const ruta = join(carpeta, pagina);
  const piePagina = pie(pagina);
  const html = readFileSync(ruta, 'utf8')
    .replace(/<header class="cabecera">[\s\S]*?<\/header>/, cabecera(pagina))
    .replace(/<footer class="pie">[\s\S]*?<\/footer>/, piePagina);
  writeFileSync(ruta, html);
  console.log(`listo: ${pagina}`);
}
