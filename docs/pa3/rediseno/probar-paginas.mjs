// Pruebas de las páginas interiores del rediseño de EPSEL (PA3): axe (WCAG 2.2 AA + buenas prácticas)
// en escritorio y celular, y pruebas de filtros, buscador, periodo de cifras, tabla y tamaños.
// Uso (con playwright-core y @axe-core/playwright instalados):
//   node probar-paginas.mjs <url de la carpeta rediseno-epsel/> <carpeta-salida>
// Se abre cada página con ?datos=ejemplo para que el resultado no dependa del backend.
import { writeFileSync, mkdirSync } from 'node:fs';
import { chromium } from 'playwright-core';
import AxeBuilder from '@axe-core/playwright';

const [base, salida] = process.argv.slice(2);
mkdirSync(salida, { recursive: true });
const b = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe' });
let fallas = 0;
const ok = (c, m) => { if (!c) fallas++; console.log(`${c ? 'OK  ' : 'FALLA'} ${m}`); };
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
  const chicos = visibles.filter((e) => !(e.tagName === 'A' && e.closest('p, td, li > p'))
    && (e.getBoundingClientRect().height < 44 || e.getBoundingClientRect().width < 44));
  const textos = [...document.querySelectorAll('body *')].filter((e) => [...e.childNodes].some((n) => n.nodeType === 3 && n.textContent.trim())
    && !e.closest('.solo-lectores, svg') && parseFloat(getComputedStyle(e).fontSize) < 14);
  return { scroll: document.documentElement.scrollWidth > innerWidth, chicos: chicos.map((e) => e.outerHTML.slice(0, 80)), textos: textos.length };
});

for (const [pagina, nombre] of [['cisterna.html', 'cisterna'], ['avisos.html', 'avisos'], ['informacion.html', 'informacion']]) {
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
    ok(await p.locator('#resultado a[href^="avisos.html?distrito="]').count() > 0, 'el resultado enlaza a los avisos del distrito');
    await p.fill('#zona', 'xx'); await p.keyboard.press('Enter');
    ok(await p.locator('#zona').getAttribute('aria-invalid') === 'true', 'menos de 3 letras: error con aria-invalid');
    await axe(p, 'cisterna-escritorio-resultado');
  }
  if (nombre === 'avisos') {
    const todos = await p.locator('#lista-avisos .aviso, #lista-proximos .aviso').count();
    await p.getByRole('button', { name: 'Chiclayo' }).click();
    const chiclayo = await p.locator('#lista-avisos .aviso-titulo span, #lista-proximos .aviso-titulo span').allInnerTexts();
    ok(chiclayo.length > 0 && chiclayo.length < todos && chiclayo.every((d) => d.includes('Chiclayo')), 'el filtro deja solo los avisos de Chiclayo');
    await p.getByRole('button', { name: 'Ferreñafe' }).click();
    ok((await p.locator('#lista-proximos').innerText()).includes('No hay cortes programados en Ferreñafe'), 'sin avisos: lo dice con texto');
    const q = await abrir(pagina, 1440, '&distrito=Monsef%C3%BA');
    ok(await q.getByRole('button', { name: 'Monsefú' }).getAttribute('aria-pressed') === 'true', 'avisos.html?distrito=Monsefú abre con ese filtro');
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
