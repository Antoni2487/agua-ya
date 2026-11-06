// Línea base PA3: corre axe-core (WCAG 2.0/2.1 A y AA + buenas prácticas) sobre páginas públicas de EPSEL.
import { mkdirSync, writeFileSync } from 'node:fs';
import AxeBuilder from '@axe-core/playwright';
import { chromium } from 'playwright-core';

const [salida, ...urls] = process.argv.slice(2);
mkdirSync(salida, { recursive: true });
const navegador = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe' });
const resumen = [];
for (const url of urls) {
  const contexto = await navegador.newContext({ viewport: { width: 1366, height: 900 }, locale: 'es-PE' });
  const pagina = await contexto.newPage();
  const nombre = url.replace(/^https?:\/\/[^/]+\//, '').replace(/[^a-z0-9]+/gi, '_').replace(/_$/, '') || 'portada';
  try {
    await pagina.goto(url, { waitUntil: 'load', timeout: 180000 });
    for (let i = 0; i < 10; i++) { await pagina.waitForTimeout(3000); try { await pagina.content(); break; } catch { /* navegando */ } }
    await pagina.waitForLoadState('load');
    const r = await new AxeBuilder({ page: pagina }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'best-practice']).analyze();
    await pagina.screenshot({ path: `${salida}/${nombre}.png` });
    writeFileSync(`${salida}/${nombre}.axe.json`, JSON.stringify(r, null, 2));
    const porImpacto = { critical: 0, serious: 0, moderate: 0, minor: 0 };
    let nodos = 0;
    for (const v of r.violations) { porImpacto[v.impact]++; nodos += v.nodes.length; }
    resumen.push({ url: pagina.url(), nombre, reglasVioladas: r.violations.length, nodos, porImpacto,
      reglas: r.violations.map((v) => `${v.impact} · ${v.id} (${v.nodes.length}) · ${v.help}`),
      fecha: r.timestamp, axe: r.testEngine.version, navegador: navegador.version() });
    console.log(nombre, r.violations.length, 'reglas', nodos, 'elementos', JSON.stringify(porImpacto));
  } catch (e) {
    resumen.push({ url, nombre, error: e.message.split('\n')[0] });
    console.log(nombre, 'ERROR', e.message.split('\n')[0]);
  }
  await contexto.close();
}
writeFileSync(`${salida}/resumen.json`, JSON.stringify(resumen, null, 2));
await navegador.close();
