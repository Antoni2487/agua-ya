// Línea base PA3 (complemento de axe): pruebas que axe no hace sobre el sitio actual de EPSEL.
// - Menú: estructura completa (para el inventario de arquitectura de información).
// - Teclado: recorre con Tab y registra cada elemento enfocado y si el foco se ve.
// - Menús desplegables: si se abren con el teclado.
// - Objetivos táctiles: controles menores de 44×44 px (WCAG 2.5.5) y de 24×24 px (WCAG 2.2, 2.5.8).
// - Texto pequeño: textos visibles menores de 14 px.
// - Celular (390 px): desplazamiento horizontal y captura.
// Uso: node auditar.mjs <carpeta-salida> <url> [<url> ...]
import { mkdirSync, writeFileSync } from 'node:fs';
import { chromium } from 'playwright-core';

const [salida, ...urls] = process.argv.slice(2);
mkdirSync(salida, { recursive: true });
const navegador = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe' });

async function abrir(contexto, url) {
  const pagina = await contexto.newPage();
  await pagina.goto(url, { waitUntil: 'load', timeout: 180000 });
  // La portada redirige a /sue/ después de cargar: espera a que el contenido deje de cambiar
  for (let i = 0; i < 10; i++) {
    await pagina.waitForTimeout(3000);
    try { await pagina.content(); break; } catch { /* sigue navegando */ }
  }
  await pagina.waitForLoadState('load');
  return pagina;
}

const resultados = [];
for (const url of urls) {
  const nombre = url.replace(/^https?:\/\/[^/]+\//, '').replace(/[^a-z0-9]+/gi, '_').replace(/_$/, '') || 'portada';
  const r = { url, nombre };

  // ---------------------------------------------------------------- escritorio
  const escritorio = await navegador.newContext({ viewport: { width: 1366, height: 900 }, locale: 'es-PE' });
  const pagina = await abrir(escritorio, url);
  r.urlFinal = pagina.url();

  r.menu = await pagina.evaluate(() => {
    const menu = [];
    document.querySelectorAll('li.dropdown').forEach((li) => {
      const titulo = li.querySelector(':scope > a')?.textContent.replace(/\s+/g, ' ').trim();
      const items = [...li.querySelectorAll('ul a')].map((a) => a.textContent.replace(/\s+/g, ' ').trim()).filter(Boolean);
      if (titulo) menu.push({ titulo, items });
    });
    return menu;
  });

  r.idioma = await pagina.evaluate(() => document.documentElement.lang || null);
  r.saltarAlContenido = await pagina.evaluate(() =>
    [...document.querySelectorAll('a[href^="#"]')].slice(0, 3).some((a) => /saltar|ir al contenido|skip/i.test(a.textContent)));

  // Teclado: 40 tabulaciones desde el inicio
  await pagina.evaluate(() => { document.activeElement?.blur(); window.scrollTo(0, 0); });
  const recorrido = [];
  for (let i = 0; i < 40; i++) {
    await pagina.keyboard.press('Tab');
    recorrido.push(await pagina.evaluate(() => {
      const el = document.activeElement;
      if (!el || el === document.body) return null;
      const cs = getComputedStyle(el);
      const texto = (el.getAttribute('aria-label') || el.textContent || el.getAttribute('title') || el.getAttribute('alt') || '')
        .replace(/\s+/g, ' ').trim().slice(0, 60);
      const rect = el.getBoundingClientRect();
      const focoVisible = (cs.outlineStyle !== 'none' && parseFloat(cs.outlineWidth) > 0) || cs.boxShadow !== 'none';
      return { etiqueta: el.tagName.toLowerCase(), texto, focoVisible, visibleEnPantalla: rect.width > 0 && rect.height > 0 };
    }));
  }
  r.teclado = {
    pasos: recorrido,
    enfocados: recorrido.filter(Boolean).length,
    sinFocoVisible: recorrido.filter((p) => p && !p.focoVisible).length,
    sinTexto: recorrido.filter((p) => p && !p.texto).length,
    invisibles: recorrido.filter((p) => p && !p.visibleEnPantalla).length,
  };

  // Menú desplegable con teclado: enfocar el primero y pulsar Enter
  r.menuConTeclado = await (async () => {
    const primero = pagina.locator('li.dropdown > a').first();
    if (!(await primero.count())) return null;
    await primero.focus();
    await pagina.keyboard.press('Enter');
    await pagina.waitForTimeout(500);
    return pagina.evaluate(() => {
      const ul = document.querySelector('li.dropdown ul');
      return !!ul && getComputedStyle(ul).display !== 'none' && ul.getBoundingClientRect().height > 0;
    });
  })();

  // Objetivos táctiles y texto pequeño (solo lo visible)
  Object.assign(r, await pagina.evaluate(() => {
    const visibles = (el) => { const b = el.getBoundingClientRect(); const cs = getComputedStyle(el);
      return b.width > 0 && b.height > 0 && cs.visibility !== 'hidden' && cs.display !== 'none'; };
    const controles = [...document.querySelectorAll('a[href], button, input:not([type=hidden]), select, textarea')].filter(visibles);
    const medidas = controles.map((el) => { const b = el.getBoundingClientRect(); return { w: b.width, h: b.height }; });
    const textos = [...document.querySelectorAll('body *')].filter((el) =>
      [...el.childNodes].some((n) => n.nodeType === 3 && n.textContent.trim().length > 1) && visibles(el));
    const pequenos = textos.filter((el) => parseFloat(getComputedStyle(el).fontSize) < 14);
    return {
      controles: controles.length,
      controlesMenores44: medidas.filter((m) => m.w < 44 || m.h < 44).length,
      controlesMenores24: medidas.filter((m) => m.w < 24 || m.h < 24).length,
      textos: textos.length,
      textosMenores14: pequenos.length,
      ejemplosTextoPequeno: [...new Set(pequenos.slice(0, 40).map((el) =>
        `${getComputedStyle(el).fontSize} · ${el.textContent.replace(/\s+/g, ' ').trim().slice(0, 50)}`))].slice(0, 8),
    };
  }));
  await escritorio.close();

  // ---------------------------------------------------------------- celular
  const celular = await navegador.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, isMobile: true,
    hasTouch: true, locale: 'es-PE' });
  const movil = await abrir(celular, url);
  r.celular = await movil.evaluate(() => ({
    anchoContenido: document.documentElement.scrollWidth,
    desplazamientoHorizontal: document.documentElement.scrollWidth > window.innerWidth + 1,
    altoPagina: document.documentElement.scrollHeight,
  }));
  await movil.screenshot({ path: `${salida}/${nombre}.celular.png` });
  await celular.close();

  resultados.push(r);
  console.log(nombre, JSON.stringify({ teclado: { ...r.teclado, pasos: undefined }, menuConTeclado: r.menuConTeclado,
    controles: r.controles, menores44: r.controlesMenores44, menores24: r.controlesMenores24,
    textosMenores14: `${r.textosMenores14}/${r.textos}`, celular: r.celular, idioma: r.idioma, saltar: r.saltarAlContenido }));
}
writeFileSync(`${salida}/auditoria.json`, JSON.stringify({ fecha: new Date().toISOString(), resultados }, null, 2));
await navegador.close();
