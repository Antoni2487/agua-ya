// Pruebas de las páginas interiores del rediseño de EPSEL (PA3): axe (WCAG 2.2 AA + buenas prácticas)
// en escritorio y celular, y pruebas de filtros, buscador, periodo de cifras, tabla y tamaños.
// Uso (con playwright-core y @axe-core/playwright instalados):
//   node probar-paginas.mjs <url de la carpeta rediseno-epsel/> <carpeta-salida>
// Se abre cada página con ?datos=ejemplo para que el resultado no dependa del backend.
import { writeFileSync, mkdirSync } from 'node:fs';
import { chromium } from 'playwright-core';
import AxeBuilder from '@axe-core/playwright';
import { existsSync, readdirSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const [base, salida] = process.argv.slice(2);
mkdirSync(salida, { recursive: true });

// Enlaces e imágenes locales que no existen (assets/… y página.html#ancla)
const carpeta = fileURLToPath(base);
const paginasHtml = readdirSync(carpeta).filter((f) => f.endsWith('.html'));
const rotos = [];
for (const f of paginasHtml) {
  const html = readFileSync(`${carpeta}/${f}`, 'utf8');
  for (const [, ref] of html.matchAll(/(?:src|href)="([^"#:?]+\.(?:html|webp|svg|css|js))(?:#[^"]+)?"/g)) if (!existsSync(`${carpeta}/${ref}`)) rotos.push(`${f} → ${ref}`);
  for (const [, ref, ancla] of html.matchAll(/href="([a-z]+\.html)#([^"]+)"/g)) if (existsSync(`${carpeta}/${ref}`) && !readFileSync(`${carpeta}/${ref}`, 'utf8').includes(`id="${ancla}"`)) rotos.push(`${f} → ${ref}#${ancla}`);
  for (const [, ancla] of html.matchAll(/href="#([^"]+)"/g)) if (!html.includes(`id="${ancla}"`)) rotos.push(`${f} → #${ancla}`);
}
const b = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe' });
let fallas = 0;
const ok = (c, m) => { if (!c) fallas++; console.log(`${c ? 'OK  ' : 'FALLA'} ${m}`); };
ok(rotos.length === 0, `enlaces e imágenes locales que no existen en las ${paginasHtml.length} páginas: ${rotos.length}${rotos.length ? ` → ${rotos.join(' | ')}` : ''}`);
const etiquetas = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];
const axe = async (p, nombre) => {
  const r = await new AxeBuilder({ page: p }).withTags(etiquetas).analyze();
  writeFileSync(`${salida}/${nombre}.axe.json`, JSON.stringify({ url: r.url, violaciones: r.violations.map((v) => ({ id: v.id, impacto: v.impact, ayuda: v.help, nodos: v.nodes.map((n) => ({ target: n.target, resumen: n.failureSummary })) })), aprobadas: r.passes.length, incompletas: r.incomplete.map((v) => ({ id: v.id, nodos: v.nodes.map((n) => `${n.target.join(' ')} · ${n.any[0]?.message ?? ''}`) })) }, null, 2));
  const elementos = r.violations.reduce((s, v) => s + v.nodes.length, 0);
  if (r.violations.length) fallas++;
  console.log(`axe ${nombre}: ${r.violations.length} reglas violadas (${elementos} elementos), ${r.passes.length} aprobadas, ${r.incomplete.length} a revisar a mano`);
  r.violations.forEach((v) => console.log(`   - [${v.impact}] ${v.id}: ${v.help} → ${v.nodes.slice(0, 3).map((n) => n.target.join(' ')).join(' | ')}`));
  r.incomplete.forEach((v) => console.log(`   · a revisar: ${v.id} (${v.nodes.length})`));
};
const abrir = async (pagina, ancho, extra = '') => {
  const ctx = await b.newContext({ viewport: { width: ancho, height: ancho > 800 ? 900 : 844 }, reducedMotion: 'reduce', locale: 'es-PE' });
  const p = await ctx.newPage();
  const errores = [];
  p.on('pageerror', (e) => errores.push(e.message));
  await p.goto(`${base}${pagina}?datos=ejemplo${extra}`);
  await p.waitForTimeout(600);
  p.errores = errores;
  return p;
};
// Mediciones de celular: desplazamiento horizontal, controles chicos y texto chico
const medir = (p) => p.evaluate(() => {
  const visibles = [...document.querySelectorAll('a, button, input, select, textarea, summary')].filter((e) => {
    const r = e.getBoundingClientRect(); return r.width > 0 && r.height > 0 && getComputedStyle(e).visibility !== 'hidden';
  });
  // Los enlaces dentro de un párrafo son la excepción "en línea" de WCAG 2.5.8
  const chicos = visibles.filter((e) => !(e.tagName === 'A' && e.closest('p, td, dd, .pasos li, .lista-simple li'))
    && (e.getBoundingClientRect().height < 44 || e.getBoundingClientRect().width < 44));
  const textos = [...document.querySelectorAll('body *')].filter((e) => [...e.childNodes].some((n) => n.nodeType === 3 && n.textContent.trim())
    && !e.closest('.solo-lectores, svg') && parseFloat(getComputedStyle(e).fontSize) < 14);
  return { scroll: document.documentElement.scrollWidth > innerWidth, chicos: chicos.map((e) => e.outerHTML.slice(0, 80)), textos: textos.length };
});

for (const [pagina, nombre] of [['cisterna.html', 'cisterna'], ['avisos.html', 'avisos'], ['informacion.html', 'informacion'], ['atencion.html', 'atencion'], ['tramites.html', 'tramites'], ['nosotros.html', 'nosotros']]) {
  console.log(`\n== ${pagina}`);
  const p = await abrir(pagina, 1440);
  await axe(p, `${nombre}-escritorio`);
  await p.screenshot({ path: `${salida}/${nombre}-escritorio.png`, fullPage: true });
  ok(await p.locator('h1').count() === 1, 'un solo h1');
  ok(await p.locator('.migas [aria-current="page"]').count() === 1, 'las migas marcan la página actual');
  const actual = await p.locator('#navegacion [aria-current="page"]').count();
  ok(actual === 1, `el menú marca la página actual (${actual})`);
  await p.keyboard.press('Tab');
  ok(await p.evaluate(() => document.activeElement.textContent.includes('Ir al contenido')), 'el primer Tab enfoca "Ir al contenido principal"');

  if (nombre === 'cisterna') {
    ok(await p.locator('#semana-dias .dia').count() === 7, 'la semana muestra 7 días');
    ok(await p.locator('#semana-dias .dia-hoy h3').innerText().then((t) => t.startsWith('Hoy')), 'el primer día dice "Hoy"');
    ok(await p.locator('.placa').first().innerText().then((t) => /^[A-Z0-9]{3}-\d{3}$/.test(t)), 'cada visita muestra la placa');
    await p.getByRole('button', { name: 'Monsefú' }).click();
    const distritos = await p.locator('#semana-dias .aviso-titulo span').allInnerTexts();
    ok(distritos.length > 0 && distritos.every((d) => d === 'Monsefú'), 'el filtro deja solo las visitas de Monsefú');
    ok(await p.getByRole('button', { name: 'Monsefú' }).getAttribute('aria-pressed') === 'true', 'el botón elegido queda con aria-pressed="true"');
    ok((await p.locator('#semana-cuenta').innerText()).includes('Monsefú'), 'la cuenta anuncia el filtro (role=status)');
    await p.fill('#zona', 'atusparias'); await p.keyboard.press('Enter'); await p.waitForTimeout(200);
    ok((await p.locator('#resultado').innerText()).includes('Atusparias'), 'el buscador encuentra Atusparias');
    ok(await p.locator('#resultado a[href^="avisos.html"]').count() > 0, 'el resultado enlaza a los comunicados');
    await p.fill('#zona', 'xx'); await p.keyboard.press('Enter');
    ok(await p.locator('#zona').getAttribute('aria-invalid') === 'true', 'menos de 3 letras: error con aria-invalid');
    await axe(p, 'cisterna-escritorio-resultado');
  }
  if (nombre === 'avisos') {
    const todos = await p.locator('#lista-avisos .aviso, #lista-proximos .aviso').count();
    await p.getByRole('button', { name: 'Lambayeque', exact: true }).click();
    const lamb = await p.locator('#lista-avisos .aviso-titulo span, #lista-proximos .aviso-titulo span').allInnerTexts();
    ok(lamb.length > 0 && lamb.length < todos && lamb.every((d) => d.includes('Lambayeque')), 'el filtro deja solo los avisos de Lambayeque');
    ok((await p.locator('#lista-avisos').innerText()).includes('tercer turno'), 'el comunicado real de Lambayeque está en texto');
    await p.getByRole('button', { name: 'Ferreñafe' }).click();
    ok((await p.locator('#lista-proximos').innerText()).includes('No hay cortes programados en Ferreñafe'), 'sin avisos: lo dice con texto');
    const q = await abrir(pagina, 1440, '&distrito=Motupe');
    ok(await q.getByRole('button', { name: 'Motupe' }).getAttribute('aria-pressed') === 'true', 'avisos.html?distrito=Motupe abre con ese filtro');
    await q.context().close();
  }
  if (nombre === 'informacion') {
    ok(await p.locator('#tabla-zonas tr').count() === 5, 'la tabla muestra las 5 zonas');
    ok(await p.locator('#tabla-zonas th[scope="row"]').count() === 5, 'cada fila tiene su encabezado de fila');
    const antes = await p.locator('[data-cifra="litros"] .numero-cifra').innerText();
    await p.getByRole('button', { name: 'Últimos 7 días' }).click(); await p.waitForTimeout(300);
    const despues = await p.locator('[data-cifra="litros"] .numero-cifra').innerText();
    ok(antes !== despues, `cambiar el periodo cambia las cifras (${antes} → ${despues})`);
    ok((await p.locator('#cifras-estado').innerText()).includes('7 días'), 'el cambio de periodo se anuncia (role=status)');
    ok(!(await p.locator('.numero-cifra').allInnerTexts()).some((t) => t.includes('…')), 'no queda ninguna cifra cargando');
  }
  if (nombre === 'atencion') {
    const total = await p.locator('#directorio [data-oficina]').count();
    ok(total === 27, `el directorio tiene las 27 oficinas reales (${total})`);
    await p.fill('#buscar-oficina', 'reque');
    ok(await p.locator('#directorio [data-oficina]:visible').count() === 1, 'buscar «reque» deja solo la oficina de Reque');
    ok((await p.locator('#oficinas-cuenta').innerText()).includes('1 oficina'), 'la cuenta se anuncia (role=status)');
    await p.fill('#buscar-oficina', 'ferrenafe');
    ok(await p.locator('#directorio [data-oficina]:visible').count() === 4, 'buscar «ferrenafe» (sin tilde) muestra la zonal completa');
    await p.fill('#buscar-oficina', 'xyz');
    ok(await p.locator('#oficinas-vacio').isVisible(), 'sin resultados: lo dice con texto y da un teléfono');
    await p.fill('#buscar-oficina', '');
    ok(await p.locator('a[href="tel:+51932070554"]').count() > 0, 'el teléfono de emergencias se puede tocar para llamar');
    await p.evaluate(() => { window.open = (url) => { window.abierto = url; }; });
    await p.fill('#r-nombre', 'Ana'); await p.selectOption('#r-motivo', 'Fuga o tubería rota'); await p.click('#escribenos button[type=submit]');
    ok((await p.evaluate(() => window.abierto ?? '')).startsWith('https://wa.me/51942813797?text='), 'el formulario abre WhatsApp con el número oficial de EPSEL');
  }
  if (nombre === 'tramites') {
    ok(await p.locator('#oficina-virtual a[href*="epsel.com.pe/sue/Sicdesa"]').count() === 4, 'la oficina virtual enlaza a los 4 servicios reales');
    ok(await p.locator('#tramites details[open] summary').innerText().then((s) => s.includes('Conexión nueva')), 'los requisitos de conexión nueva se ven abiertos');
  }
  if (nombre === 'nosotros') {
    ok(await p.locator('.linea-tiempo li').count() === 7, 'la historia tiene 7 hitos');
    ok(await p.locator('#entidades a').count() === 5, 'las 5 entidades relacionadas enlazan a su sitio');
  }
  ok(p.errores.length === 0, `sin errores de JavaScript${p.errores.length ? `: ${p.errores.join(' | ')}` : ''}`);
  await p.context().close();

  const m = await abrir(pagina, 390);
  await axe(m, `${nombre}-celular`);
  await m.screenshot({ path: `${salida}/${nombre}-celular.png`, fullPage: true });
  const med = await medir(m);
  ok(!med.scroll, 'celular: sin desplazamiento horizontal de la página');
  ok(med.chicos.length === 0, `celular: controles menores de 44 px = ${med.chicos.length}${med.chicos.length ? ` → ${med.chicos.slice(0, 3).join(' | ')}` : ''}`);
  ok(med.textos === 0, `celular: textos menores de 14 px = ${med.textos}`);
  await m.locator('.boton-menu').click();
  await m.getByRole('button', { name: 'Agua en tu zona' }).click();
  await axe(m, `${nombre}-celular-menu`);
  await m.context().close();
}
await b.close();
console.log(`\n${fallas === 0 ? 'Todo pasa.' : `${fallas} fallas.`}`);
process.exit(fallas ? 1 : 0);
