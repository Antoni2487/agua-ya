#!/usr/bin/env node
// Simula el recorrido de una cisterna para las demos, sin manejar de verdad. Ingresa como el conductor y usa
// la misma API que la app: inicia la ruta, registra un viaje, "maneja" de parada en parada enviando posiciones
// GPS y marca cada entrega (la geocerca la valida porque la última posición está en la parada). Todo se ve en
// vivo en el panel del admin (Mapa en vivo) y en la app del vecino (Mapa).
//
// Uso (Node 20+, sin dependencias):
//   node scripts/simular-ruta.mjs                         ruta de hoy del conductor 20000001 en Render
//   node scripts/simular-ruta.mjs --ruta 1 --cada 2       una ruta concreta, posición cada 2 s
//   node scripts/simular-ruta.mjs --sin-atender           solo mueve el camión (las entregas se marcan en la app)
//   node scripts/simular-ruta.mjs --api http://localhost:8080 --dni 20000002
//
// Opciones: --api URL · --dni DNI · --clave CONTRASEÑA · --ruta ID · --cada SEGUNDOS (def. 3) · --paso METROS
// (def. 60, avance entre posiciones) · --espera SEGUNDOS (def. 4, parado en cada parada) · --litros (def. 8000)
// · --sin-atender · --sin-finalizar

const args = Object.fromEntries(process.argv.slice(2).reduce((acc, a, i, todos) => {
  if (!a.startsWith('--')) return acc;
  const siguiente = todos[i + 1];
  acc.push([a.slice(2), siguiente && !siguiente.startsWith('--') ? siguiente : true]);
  return acc;
}, []));

const API = String(args.api ?? process.env.API_URL ?? 'https://aguaya-backend.onrender.com').replace(/\/$/, '');
const DNI = String(args.dni ?? '20000001');
const CLAVE = String(args.clave ?? 'Conductor2026!');
const CADA_MS = Number(args.cada ?? 3) * 1000;
const PASO_M = Number(args.paso ?? 60);
const ESPERA_MS = Number(args.espera ?? 4) * 1000;
const LITROS = Number(args.litros ?? 8000);

const dormir = (ms) => new Promise((r) => setTimeout(r, ms));
const hora = () => new Date().toLocaleTimeString('es-PE');
const log = (...m) => console.log(`[${hora()}]`, ...m);

let token = null;

/** fetch con reintentos: el backend gratis de Render a veces corta la conexión ("fetch failed") o tarda en despertar. */
async function pedir(url, opciones, intentos = 5) {
  for (let i = 1; ; i++) {
    try {
      return await fetch(url, { ...opciones, signal: AbortSignal.timeout(60_000) });
    } catch (e) {
      if (i >= intentos) throw new Error(`Sin conexión con ${API} (${e.cause?.code ?? e.name}). Revisa tu internet o el estado de Render.`);
      const espera = 2 ** i * 1000;
      process.stdout.write(`
[${hora()}] Se cortó la conexión (${e.cause?.code ?? e.name}); reintento ${i}/${intentos - 1} en ${espera / 1000} s…
`);
      await dormir(espera);
    }
  }
}

async function ingresar() {
  const r = await pedir(`${API}/api/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ dni: DNI, password: CLAVE }),
  });
  if (!r.ok) throw new Error(`No se pudo ingresar como ${DNI}: ${r.status} ${await r.text()}`);
  const sesion = await r.json();
  if (sesion.usuario.rol !== 'CONDUCTOR') throw new Error(`El DNI ${DNI} no es de un conductor`);
  token = sesion.accessToken;
  return sesion.usuario;
}

/** Llama a la API como el conductor; si el token venció (15 min), vuelve a ingresar una vez. */
async function api(metodo, ruta, cuerpo, reintento = true) {
  const r = await pedir(`${API}${ruta}`, {
    method: metodo,
    headers: { Authorization: `Bearer ${token}`, ...(cuerpo ? { 'Content-Type': 'application/json' } : {}) },
    body: cuerpo ? JSON.stringify(cuerpo) : undefined,
  });
  if (r.status === 401 && reintento) { await ingresar(); return api(metodo, ruta, cuerpo, false); }
  const texto = await r.text();
  const datos = texto ? JSON.parse(texto) : null;
  if (!r.ok) {
    const error = new Error(datos?.mensaje ?? `${r.status}`);
    error.codigo = datos?.codigo;
    throw error;
  }
  return datos;
}

function distancia(a, b) {
  const rad = (g) => (g * Math.PI) / 180;
  const dLat = rad(b.lat - a.lat);
  const dLng = rad(b.lng - a.lng);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.lat)) * Math.cos(rad(b.lat)) * Math.sin(dLng / 2) ** 2;
  return 2 * 6371000 * Math.asin(Math.sqrt(h));
}

async function enviarPosicion(rutaId, p) {
  await api('POST', `/api/conductor/rutas/${rutaId}/posicion`, { latitud: p.lat.toFixed(6), longitud: p.lng.toFixed(6) });
}

/** Avanza en línea recta de `desde` a `hasta`, una posición cada CADA_MS. */
async function manejar(rutaId, desde, hasta) {
  const total = distancia(desde, hasta);
  const pasos = Math.max(1, Math.ceil(total / PASO_M));
  for (let i = 1; i <= pasos; i++) {
    const t = i / pasos;
    const p = { lat: desde.lat + (hasta.lat - desde.lat) * t, lng: desde.lng + (hasta.lng - desde.lng) * t };
    await enviarPosicion(rutaId, p);
    process.stdout.write(`\r   ${Math.round(total * (1 - t))} m para llegar…   `);
    if (i < pasos) await dormir(CADA_MS);
  }
  process.stdout.write('\r');
  return hasta;
}

async function main() {
  log(`Backend: ${API}`);
  const conductor = await ingresar();
  log(`Conductor: ${conductor.nombres} ${conductor.apellidos}`);

  const rutas = await api('GET', '/api/conductor/rutas/hoy');
  const elegida = args.ruta
    ? rutas.find((d) => d.ruta.id === Number(args.ruta))
    : rutas.find((d) => d.ruta.estado === 'EN_CURSO') ?? rutas.find((d) => d.ruta.estado === 'PROGRAMADA');
  if (!elegida) {
    log('No hay una ruta de hoy para simular. Prográmala en el panel del admin (fecha de hoy) y asígnale este conductor y una cisterna.');
    process.exit(1);
  }
  const rutaId = elegida.ruta.id;
  log(`Ruta ${rutaId}: ${elegida.ruta.sectorNombre}, ${elegida.ruta.turno === 'MANANA' ? 'turno mañana' : 'turno tarde'} (${elegida.ruta.estado})`);

  if (elegida.ruta.estado === 'PROGRAMADA') {
    await api('PATCH', `/api/conductor/rutas/${rutaId}/iniciar`);
    log('Ruta iniciada: los vecinos del sector reciben el aviso.');
  } else if (elegida.ruta.estado !== 'EN_CURSO') {
    log(`La ruta está ${elegida.ruta.estado}: no se puede simular.`);
    process.exit(1);
  }

  let viaje = elegida.viajes.find((v) => v.estado === 'EN_REPARTO');
  if (!viaje) {
    viaje = await api('POST', `/api/conductor/rutas/${rutaId}/viajes`, { volumenCargadoLitros: LITROS });
    log(`Viaje ${viaje.nroViaje} registrado: sale cargada con ${LITROS} L.`);
  }

  const pendientes = elegida.paradas.filter((p) => p.estado === 'PENDIENTE').sort((a, b) => a.orden - b.orden);
  if (pendientes.length === 0) log('No quedan paradas pendientes.');

  // Arranca a ~800 m de la primera parada (el camión viene desde el punto de llenado)
  const primera = pendientes[0];
  let aqui = primera ? { lat: Number(primera.latitud) + 0.005, lng: Number(primera.longitud) - 0.005 } : null;
  if (aqui) await enviarPosicion(rutaId, aqui);

  for (const parada of pendientes) {
    const destino = { lat: Number(parada.latitud), lng: Number(parada.longitud) };
    log(`→ Parada ${parada.orden}: ${parada.nombre}${parada.prioridad === 'CRITICA' ? ' (crítica)' : ''} a ${Math.round(distancia(aqui, destino))} m`);
    aqui = await manejar(rutaId, aqui, destino);
    if (!args['sin-atender']) {
      await dormir(ESPERA_MS);
      try {
        await api('PATCH', `/api/conductor/paradas/${parada.id}/atender`, {});
        log(`   ✓ Entrega registrada y verificada por GPS en la parada ${parada.orden}.`);
      } catch (e) {
        log(`   ✗ No se pudo marcar la parada ${parada.orden}: ${e.message} (${e.codigo ?? 'error'})`);
      }
    } else {
      log(`   En la parada ${parada.orden}. Márcala desde la app del conductor.`);
      await dormir(ESPERA_MS);
    }
  }

  if (!args['sin-finalizar'] && !args['sin-atender']) {
    await api('PATCH', `/api/conductor/viajes/${viaje.id}/finalizar`);
    log('Viaje finalizado.');
    await api('PATCH', `/api/conductor/rutas/${rutaId}/finalizar`);
    log('Ruta completada. El conductor vuelve a quedar disponible.');
  } else {
    log('Fin del recorrido (la ruta sigue en curso).');
  }
}

main().catch((e) => { console.error(`\n[${hora()}] Error: ${e.message}`); process.exit(1); });
