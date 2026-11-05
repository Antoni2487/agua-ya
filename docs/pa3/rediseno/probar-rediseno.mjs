// Pruebas del rediseño de EPSEL (PA3): axe (WCAG 2.2 AA + buenas prácticas) en escritorio y celular, y pruebas de teclado,
// buscador, acordeón, formulario, objetivos táctiles y tamaño de texto.
// Uso (con playwright-core y @axe-core/playwright instalados): node probar-rediseno.mjs <url de index.html> <carpeta-salida>
import { writeFileSync, mkdirSync } from 'node:fs';
import { chromium } from 'playwright-core';
import AxeBuilder from '@axe-core/playwright';
const [url, salida] = process.argv.slice(2);
mkdirSync(salida, { recursive: true });
const b = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe' });
const ok = (c, m) => console.log(`${c ? 'OK  ' : 'FALLA'} ${m}`);
const etiquetas = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa', 'best-practice'];
const axe = async (p, nombre) => {
  const r = await new AxeBuilder({ page: p }).withTags(etiquetas).analyze();
  writeFileSync(`${salida}/${nombre}.axe.json`, JSON.stringify({ url: r.url, violaciones: r.violations.map(v => ({ id: v.id, impacto: v.impact, ayuda: v.help, nodos: v.nodes.map(n => ({ target: n.target, resumen: n.failureSummary })) })), aprobadas: r.passes.length, incompletas: r.incomplete.map(v => ({ id: v.id, nodos: v.nodes.map(n => n.target.join(' ') + ' · ' + (n.any[0]?.message ?? '')) })) }, null, 2));
  console.log(`axe ${nombre}: ${r.violations.length} reglas violadas (${r.violations.reduce((s, v) => s + v.nodes.length, 0)} elementos), ${r.passes.length} aprobadas, ${r.incomplete.length} a revisar a mano`);
  r.violations.forEach(v => console.log(`   - [${v.impact}] ${v.id}: ${v.help} → ${v.nodes.slice(0, 3).map(n => n.target.join(' ')).join(' | ')}`));
};

// Escritorio
let p = await (await b.newContext({ viewport: { width: 1440, height: 900 }, reducedMotion: 'reduce', locale: 'es-PE' })).newPage();
await p.goto(url); await p.waitForTimeout(7000);
await axe(p, 'escritorio');

// Teclado: primer Tab = saltar al contenido
await p.keyboard.press('Tab');
ok(await p.evaluate(() => document.activeElement.textContent.includes('Ir al contenido')), 'el primer Tab enfoca "Ir al contenido principal"');
// Menú con teclado
await p.locator('.menu-boton').first().focus();
await p.keyboard.press('Enter');
ok(await p.locator('#sub-agua').isVisible(), 'Enter abre "Agua en tu zona"');
ok(await p.locator('.menu-boton').first().getAttribute('aria-expanded') === 'true', 'aria-expanded pasa a true');
await axe(p, 'escritorio-menu-abierto');
await p.keyboard.press('Escape');
ok(!(await p.locator('#sub-agua').isVisible()), 'Esc cierra el submenú');
ok(await p.evaluate(() => document.activeElement.classList.contains('menu-boton')), 'el foco vuelve al botón del menú');
await p.locator('.menu-boton').nth(1).focus(); await p.keyboard.press('Space');
ok(await p.locator('#sub-pagos').isVisible(), 'Espacio abre "Pagos y trámites"');
await p.keyboard.press('Tab');
ok(await p.evaluate(() => document.activeElement.closest('#sub-pagos') !== null), 'Tab entra a las opciones del submenú');
await p.keyboard.press('Escape');

// Buscador: error y resultado
await p.fill('#zona', 'xyz'); await p.press('#zona', 'Enter'); await p.waitForTimeout(300);
ok(await p.locator('#zona-error').isVisible() && await p.getAttribute('#zona', 'aria-invalid') === 'true', 'zona inexistente: mensaje de error y aria-invalid');
await p.fill('#zona', 'cerropon'); await p.press('#zona', 'Enter'); await p.waitForTimeout(500);
const tarjeta = await p.locator('.tarjeta-zona').first().textContent();
ok(/Cerropón/.test(tarjeta), `buscar "cerropon" (sin tilde) encuentra la zona: ${tarjeta.replace(/\s+/g, ' ').trim()}`);
ok(await p.getAttribute('#zona', 'aria-invalid') === null, 'el error se limpia al acertar');
await p.locator('.carpeta-frente').screenshot({ path: `${salida}/resultado-busqueda.png` });
await axe(p, 'escritorio-con-resultado');

// Acordeón
await p.getByRole('button', { name: 'Lagunas Boró' }).click(); await p.waitForTimeout(300);
ok(await p.getAttribute('[aria-controls="etapa-3"]', 'aria-expanded') === 'true' && await p.isVisible('#etapa-3') && !(await p.isVisible('#etapa-1')), 'el acordeón abre "Lagunas Boró" y cierra "Tinajones"');

// Formulario
await p.click('text=Abrir WhatsApp con mi mensaje');
ok(await p.locator('#r-nombre-error').isVisible() && await p.evaluate(() => document.activeElement.id === 'r-nombre'), 'formulario vacío: errores con texto y foco en el primer campo');
await p.close();

// Celular
p = await (await b.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, isMobile: true, hasTouch: true, reducedMotion: 'reduce', locale: 'es-PE' })).newPage();
await p.goto(url); await p.waitForTimeout(7000);
await axe(p, 'celular');
ok(await p.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'celular: sin desplazamiento horizontal');
await p.click('.boton-menu');
ok(await p.locator('#navegacion').isVisible(), 'celular: el botón "Menú" abre la navegación');
await p.click('text=Atención y reclamos >> nth=0');
ok(await p.locator('#sub-atencion').isVisible(), 'celular: el menú funciona como acordeón');
await p.screenshot({ path: `${salida}/celular-menu.png` });
await axe(p, 'celular-menu-abierto');
// Objetivos táctiles
const chicos = await p.evaluate(() => [...document.querySelectorAll('a[href], button, input, select, textarea, summary')].filter(e => { const r = e.getBoundingClientRect(); return r.width > 0 && r.height > 0 && (r.height < 44 || r.width < 44) && !e.closest('p'); }).map(e => `${e.tagName} "${(e.textContent || e.id).trim().slice(0, 30)}" ${Math.round(e.getBoundingClientRect().width)}×${Math.round(e.getBoundingClientRect().height)}`));
ok(chicos.length === 0, `controles menores de 44 px fuera de texto corrido: ${chicos.length}${chicos.length ? ' → ' + chicos.join('; ') : ''}`);
const textoChico = await p.evaluate(() => [...document.querySelectorAll('body *')].filter(el => [...el.childNodes].some(n => n.nodeType === 3 && n.textContent.trim().length > 1) && el.getBoundingClientRect().height > 0 && parseFloat(getComputedStyle(el).fontSize) < 14).length);
ok(textoChico === 0, `textos menores de 14 px: ${textoChico}`);
await p.close();
await b.close();
