// Rediseño EPSEL (propuesta académica PA3). Sin librerías: el sitio debe cargar rápido en un celular gama baja.
// Un solo script para las cuatro páginas; cada parte revisa si su elemento existe antes de arrancar.
// Sin JavaScript se ve el menú completo, los textos y los enlaces; el script agrega el menú desplegable,
// los datos de las zonas, los filtros, el acordeón y el movimiento.

// URL del backend de AguaYa (panel público de transparencia). Con ?datos=ejemplo se usan datos de ejemplo.
const API = window.EPSEL_API ?? 'https://aguaya-backend.onrender.com';
const ESPERA_MS = 6000;
const SOLO_EJEMPLO = new URLSearchParams(location.search).get('datos') === 'ejemplo';

const hoy = new Date();
const fechaCorta = new Intl.DateTimeFormat('es-PE', { weekday: 'long', day: 'numeric', month: 'long' });
const fechaSinDia = new Intl.DateTimeFormat('es-PE', { day: 'numeric', month: 'long' });
const numero = new Intl.NumberFormat('es-PE');
const TURNO = { MANANA: 'turno mañana', TARDE: 'turno tarde' };

/* ---------------------------------------------------------------- Utilidades */
const iso = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
const desdeIso = (t) => { const [a, m, d] = t.split('-').map(Number); return new Date(a, m - 1, d); };
const sumarDias = (n, base = hoy) => { const d = new Date(base.getFullYear(), base.getMonth(), base.getDate()); d.setDate(d.getDate() + n); return d; };
const diasHasta = (t) => Math.round((desdeIso(t) - sumarDias(0)) / 86400000);
const mayuscula = (t) => t[0].toUpperCase() + t.slice(1);
const sinTildes = (t) => t.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
  .replace(/\bp\.?\s?j\.?\s*/g, '').replace(/\s+/g, ' ').trim();

// "Hoy, jueves 5 de noviembre" / "Mañana, viernes 6…" / "Sábado 7 de noviembre"
function nombreDelDia(t) {
  const n = diasHasta(t);
  const texto = fechaCorta.format(desdeIso(t));
  if (n === 0) return `Hoy, ${texto}`;
  if (n === 1) return `Mañana, ${texto}`;
  return mayuscula(texto);
}

function elemento(etiqueta, atributos = {}, ...hijos) {
  const el = document.createElement(etiqueta);
  Object.entries(atributos).forEach(([k, v]) => (k === 'class' ? (el.className = v) : el.setAttribute(k, v)));
  hijos.flat().filter((h) => h !== null && h !== undefined && h !== false).forEach((h) => el.append(h));
  return el;
}

// Pide un endpoint público de AguaYa; si no responde a tiempo, usa los datos de ejemplo.
async function pedir(ruta, ejemplo) {
  if (SOLO_EJEMPLO) return { datos: ejemplo(), ejemplo: true };
  try {
    const control = new AbortController();
    const reloj = setTimeout(() => control.abort(), ESPERA_MS);
    const r = await fetch(`${API}/api/public/transparencia/${ruta}`, { signal: control.signal });
    clearTimeout(reloj);
    if (!r.ok) throw new Error(r.status);
    const datos = await r.json();
    if (Array.isArray(datos) && !datos.length) throw new Error('vacío');
    return { datos, ejemplo: false };
  } catch {
    return { datos: ejemplo(), ejemplo: true };
  }
}

function mostrarNota(id, ejemplo, texto) {
  const nota = document.getElementById(id);
  if (!nota) return;
  nota.hidden = !ejemplo;
  nota.textContent = ejemplo ? texto : '';
}

/* ---------------------------------------------------------------- Menú */
function iniciarMenu() {
  const botonMenu = document.querySelector('.boton-menu');
  const nav = document.getElementById('navegacion');
  if (!botonMenu || !nav) return;
  const botones = [...document.querySelectorAll('.menu-boton')];
  const escritorio = matchMedia('(min-width: 85em)');

  const cerrarSubmenus = (excepto) => botones.forEach((b) => {
    if (b === excepto) return;
    b.setAttribute('aria-expanded', 'false');
    document.getElementById(b.getAttribute('aria-controls')).classList.remove('abierto');
  });
  const cerrarTodo = () => {
    cerrarSubmenus();
    if (!escritorio.matches) { nav.classList.remove('abierta'); botonMenu.setAttribute('aria-expanded', 'false'); }
  };

  botonMenu.addEventListener('click', () => {
    const abrir = botonMenu.getAttribute('aria-expanded') !== 'true';
    botonMenu.setAttribute('aria-expanded', String(abrir));
    nav.classList.toggle('abierta', abrir);
    if (!abrir) cerrarSubmenus();
  });

  botones.forEach((boton) => boton.addEventListener('click', () => {
    const abrir = boton.getAttribute('aria-expanded') !== 'true';
    if (escritorio.matches) cerrarSubmenus(boton);
    boton.setAttribute('aria-expanded', String(abrir));
    document.getElementById(boton.getAttribute('aria-controls')).classList.toggle('abierto', abrir);
  }));

  // Esc cierra el submenú abierto y devuelve el foco a su botón
  document.addEventListener('keydown', (e) => {
    if (e.key !== 'Escape') return;
    const abierto = botones.find((b) => b.getAttribute('aria-expanded') === 'true');
    if (abierto) { cerrarSubmenus(); abierto.focus(); return; }
    if (nav.classList.contains('abierta')) { cerrarTodo(); botonMenu.focus(); }
  });
  // Clic fuera o al elegir una opción: se cierra
  document.addEventListener('click', (e) => { if (!e.target.closest('.cabecera')) cerrarTodo(); });
  nav.addEventListener('click', (e) => { if (e.target.closest('a')) cerrarTodo(); });
  // En escritorio, si el foco sale de un submenú con Tab, se cierra
  nav.addEventListener('focusout', (e) => {
    if (escritorio.matches && !e.currentTarget.contains(e.relatedTarget)) cerrarSubmenus();
  });
  escritorio.addEventListener('change', cerrarTodo);
}

/* ---------------------------------------------------------------- Avisos (de ejemplo: EPSEL los publica como comunicados) */
const TIPO_AVISO = {
  corte: 'Corte de agua',
  baja: 'Poca presión',
  cisterna: 'Cisterna de apoyo',
  normal: 'Servicio normal',
};
const AVISOS = [
  { tipo: 'corte', distritos: ['José Leonardo Ortiz'], dia: 0, texto: 'De 8:00 a.m. a 4:00 p.m. en el P.J. Urrunaga y alrededores, por cambio de tubería.', consejo: 'En la tarde pasa una cisterna.' },
  { tipo: 'baja', distritos: ['Chiclayo'], dia: 0, texto: 'De 2:00 p.m. a 8:00 p.m. en las partes altas de Cerropón, por limpieza del reservorio.', consejo: 'Guarda agua en la mañana.' },
  { tipo: 'cisterna', distritos: ['Monsefú'], dia: 0, texto: 'Valle Hermoso recibe cisterna mientras se repara el pozo.', consejo: 'Revisa el día y el turno en «¿Pasa hoy la cisterna?».' },
  { tipo: 'normal', distritos: ['Lambayeque', 'Ferreñafe'], dia: 0, texto: 'No hay cortes programados para hoy.' },
  { tipo: 'corte', distritos: ['La Victoria'], dia: 1, texto: 'De 9:00 a.m. a 1:00 p.m. en la parte baja de la Urb. Santa Victoria, por conexión de la red nueva.', consejo: 'Llena tus baldes y tu tanque la noche anterior.' },
  { tipo: 'baja', distritos: ['Pimentel'], dia: 2, texto: 'De 6:00 a.m. a 10:00 a.m. cerca del malecón, por mantenimiento de la bomba.' },
  { tipo: 'corte', distritos: ['Chiclayo'], dia: 3, texto: 'De 8:00 a.m. a 2:00 p.m. en la Urb. Los Parques, por limpieza de buzones.', consejo: 'Cierra la llave de paso para que no entre aire a tu tubería.' },
];
const DISTRITOS_AVISOS = ['Chiclayo', 'José Leonardo Ortiz', 'La Victoria', 'Pimentel', 'Monsefú', 'Lambayeque', 'Ferreñafe'];
const avisoDe = (a, distrito) => a.distritos.some((d) => sinTildes(d) === sinTildes(distrito));

function itemAviso(a, conFecha) {
  return elemento('li', { class: 'aviso', 'data-distritos': a.distritos.join('|') },
    elemento('span', { class: `gota-vineta gota-${a.tipo}`, 'aria-hidden': 'true' }),
    elemento('div', {},
      elemento('p', { class: 'aviso-titulo' }, elemento('strong', {}, TIPO_AVISO[a.tipo]), ' ', elemento('span', {}, a.distritos.join(' y '))),
      conFecha && elemento('p', { class: 'aviso-dia' }, nombreDelDia(iso(sumarDias(a.dia)))),
      elemento('p', {}, a.consejo ? `${a.texto} ${a.consejo}` : a.texto)));
}

// Portada: solo los avisos de hoy
function pintarAvisosDeHoy() {
  const lista = document.getElementById('lista-avisos');
  if (!lista || document.body.dataset.pagina === 'avisos') return;
  lista.replaceChildren(...AVISOS.filter((a) => a.dia === 0).map((a) => itemAviso(a, false)));
}

/* ---------------------------------------------------------------- Filtro por distrito (botones que se quedan presionados) */
function crearFiltro(contenedor, distritos, alCambiar, inicial = '') {
  const opciones = [['', 'Todos'], ...distritos.map((d) => [d, d])];
  const botones = opciones.map(([valor, texto]) => elemento('button',
    { type: 'button', class: 'filtro-boton', 'aria-pressed': String(sinTildes(valor) === sinTildes(inicial)), 'data-valor': valor }, texto));
  if (!botones.some((b) => b.getAttribute('aria-pressed') === 'true')) botones[0].setAttribute('aria-pressed', 'true');
  contenedor.replaceChildren(...botones);
  botones.forEach((b) => b.addEventListener('click', () => {
    botones.forEach((o) => o.setAttribute('aria-pressed', String(o === b)));
    alCambiar(b.dataset.valor);
  }));
  return botones.find((b) => b.getAttribute('aria-pressed') === 'true').dataset.valor;
}

/* ---------------------------------------------------------------- Página de avisos */
function iniciarPaginaAvisos() {
  if (document.body.dataset.pagina !== 'avisos') return;
  const listaHoy = document.getElementById('lista-avisos');
  const listaProximos = document.getElementById('lista-proximos');
  const cuenta = document.getElementById('avisos-cuenta');

  const pintar = (distrito) => {
    const elegidos = AVISOS.filter((a) => !distrito || avisoDe(a, distrito));
    const deHoy = elegidos.filter((a) => a.dia === 0);
    const proximos = elegidos.filter((a) => a.dia > 0).sort((a, b) => a.dia - b.dia);
    const vacio = (texto) => elemento('li', { class: 'aviso aviso-vacio' },
      elemento('span', { class: 'gota-vineta gota-normal', 'aria-hidden': 'true' }), elemento('p', {}, texto));
    const donde = distrito ? ` en ${distrito}` : '';
    listaHoy.replaceChildren(...(deHoy.length ? deHoy.map((a) => itemAviso(a, false)) : [vacio(`No hay avisos para hoy${donde}. El servicio es normal.`)]));
    listaProximos.replaceChildren(...(proximos.length ? proximos.map((a) => itemAviso(a, true)) : [vacio(`No hay cortes programados${donde} en los próximos días.`)]));
    const total = deHoy.filter((a) => a.tipo !== 'normal').length + proximos.length;
    cuenta.textContent = `${total === 1 ? '1 aviso' : `${total} avisos`}${donde || ' en todos los distritos'}.`;
  };
  const inicial = new URLSearchParams(location.search).get('distrito') ?? '';
  pintar(crearFiltro(document.getElementById('filtro-avisos'), DISTRITOS_AVISOS, pintar, inicial));
}

/* ---------------------------------------------------------------- Datos de ejemplo de las zonas (fechas relativas a hoy) */
function sectoresDeEjemplo() {
  const s = (sectorId, nombre, distrito, unidadesUso, proxima, ultima, cumplimientoPorcentaje, calificacionPromedio) => ({
    sectorId, nombre, distrito, esZonaAlta: true, unidadesUso,
    proximaAtencion: proxima && { fecha: iso(sumarDias(proxima[0])), turno: proxima[1], estado: proxima[2] },
    ultimaAtencion: ultima === null ? null : iso(sumarDias(ultima)),
    cumplimientoPorcentaje, calificacionPromedio,
  });
  return [
    s(1, 'Cerropón', 'Chiclayo', 420, [0, 'MANANA', 'EN_CURSO'], -2, 94.1, 4.4),
    s(2, 'P.J. Atusparias', 'José Leonardo Ortiz', 380, [0, 'TARDE', 'PROGRAMADA'], -3, 88.6, 4.1),
    s(3, 'P.J. Urrunaga', 'José Leonardo Ortiz', 310, [0, 'TARDE', 'PROGRAMADA'], -1, 91.0, 4.0),
    s(4, 'Valle Hermoso', 'Monsefú', 260, [1, 'MANANA', 'PROGRAMADA'], -2, 76.3, 3.6),
    s(5, 'Callanca', 'Monsefú', 190, [2, 'MANANA', 'PROGRAMADA'], -5, 97.2, 4.6),
  ];
}

function cronogramaDeEjemplo() {
  const r = (rutaId, dia, turno, estado, sectorId, sectorNombre, distrito, cisternaPlaca, totalParadas, paradasAtendidas) =>
    ({ rutaId, fecha: iso(sumarDias(dia)), turno, estado, sectorId, sectorNombre, distrito, cisternaPlaca, totalParadas, paradasAtendidas });
  return [
    r(101, 0, 'MANANA', 'EN_CURSO', 1, 'Cerropón', 'Chiclayo', 'T5K-842', 34, 19),
    r(102, 0, 'TARDE', 'PROGRAMADA', 2, 'P.J. Atusparias', 'José Leonardo Ortiz', 'M2B-317', 29, 0),
    r(103, 0, 'TARDE', 'PROGRAMADA', 3, 'P.J. Urrunaga', 'José Leonardo Ortiz', 'T5K-842', 24, 0),
    r(104, 1, 'MANANA', 'PROGRAMADA', 4, 'Valle Hermoso', 'Monsefú', 'C8F-205', 6, 0),
    r(105, 2, 'MANANA', 'PROGRAMADA', 5, 'Callanca', 'Monsefú', 'M2B-317', 5, 0),
    r(106, 2, 'TARDE', 'PROGRAMADA', 1, 'Cerropón', 'Chiclayo', 'T5K-842', 34, 0),
    r(107, 4, 'MANANA', 'PROGRAMADA', 2, 'P.J. Atusparias', 'José Leonardo Ortiz', 'C8F-205', 29, 0),
    r(108, 5, 'MANANA', 'PROGRAMADA', 4, 'Valle Hermoso', 'Monsefú', 'M2B-317', 6, 0),
  ];
}

// Un periodo de ejemplo: las cantidades crecen con los días para que el cambio de periodo se note
function resumenDeEjemplo(dias) {
  const f = dias / 30;
  const completadas = Math.round(46 * f);
  const interrumpidas = Math.max(1, Math.round(3 * f));
  const total = Math.round(1260 * f);
  const atendidas = Math.round(total * 0.912);
  return {
    desde: iso(sumarDias(-(dias - 1))), hasta: iso(sumarDias(0)),
    sectoresActivos: 5, sectoresAtendidos: 5, distritos: 3,
    rutas: { total: completadas + interrumpidas + 3, programadas: 2, enCurso: 1, completadas, interrumpidas },
    paradas: { total, atendidas, noAtendidas: Math.round(total * 0.06), sinAtender: total - atendidas - Math.round(total * 0.06), cumplimientoPorcentaje: 91.2 },
    litrosDistribuidos: Math.round(1180000 * f),
    calificacion: { promedio: 4.2, total: Math.round(310 * f) },
    incidencias: { reportadas: Math.round(24 * f), resueltas: Math.round(21 * f) },
    flota: { total: 5, disponiblesHoy: 3 },
  };
}

/* ---------------------------------------------------------------- Estado de una zona, siempre en palabras */
function estadoZona(s) {
  const p = s.proximaAtencion;
  if (!p) return { clase: 'sin-visita', corto: 'Sin visita de cisterna programada', largo: 'Por ahora no hay una visita de cisterna programada para esta zona.' };
  const n = diasHasta(p.fecha);
  const turno = TURNO[p.turno] ?? p.turno.toLowerCase();
  if (p.estado === 'EN_CURSO') return { clase: 'en-camino', corto: `En camino ahora, ${turno}`, largo: `La cisterna está repartiendo ahora, en el ${turno}.` };
  if (n === 0) return { clase: '', corto: `Hoy, ${turno}`, largo: `Hoy pasa la cisterna, en el ${turno}.` };
  if (n === 1) return { clase: '', corto: `Mañana, ${turno}`, largo: `Mañana pasa la cisterna, en el ${turno}.` };
  const dia = fechaCorta.format(desdeIso(p.fecha));
  return { clase: '', corto: `${mayuscula(dia)}, ${turno}`, largo: `La próxima cisterna pasa el ${dia}, en el ${turno}.` };
}

// Portada: lista corta "La cisterna esta semana" y las opciones del buscador
function pintarHorarios(sectores, ejemplo) {
  const opciones = new Set([...sectores.map((s) => s.nombre), ...sectores.map((s) => s.distrito)]);
  const datalist = document.getElementById('lista-zonas');
  if (datalist) datalist.replaceChildren(...[...opciones].map((v) => elemento('option', { value: v })));

  const lista = document.getElementById('lista-horarios');
  if (!lista) return;
  const orden = [...sectores].sort((a, b) =>
    (a.proximaAtencion?.fecha ?? '9999').localeCompare(b.proximaAtencion?.fecha ?? '9999')
    || (a.proximaAtencion?.turno ?? '').localeCompare(b.proximaAtencion?.turno ?? ''));
  lista.replaceChildren(...orden.map((s) => elemento('li', { class: 'aviso' },
    elemento('span', { class: 'gota-vineta gota-cisterna', 'aria-hidden': 'true' }),
    elemento('div', {},
      elemento('p', { class: 'aviso-titulo' }, elemento('strong', {}, s.nombre), ' ', elemento('span', {}, s.distrito)),
      elemento('p', {}, estadoZona(s).corto)))));
  const conVisita = sectores.filter((s) => s.proximaAtencion).length;
  document.getElementById('horarios-cuenta').textContent = `${conVisita} de ${sectores.length} zonas con visita`;
  mostrarNota('nota-datos', ejemplo, 'Estos horarios son de ejemplo: no pudimos conectar con el sistema de cisternas en este momento.');
}

/* ---------------------------------------------------------------- Buscador "¿Hay agua hoy en tu zona?" */
function iniciarBuscador(datos) {
  const form = document.querySelector('.buscador');
  if (!form) return;
  const input = document.getElementById('zona');
  const error = document.getElementById('zona-error');
  const resultado = document.getElementById('resultado');
  const enCisterna = document.body.dataset.pagina === 'cisterna';

  const mostrarError = (texto) => {
    error.textContent = texto; error.hidden = false;
    input.setAttribute('aria-invalid', 'true');
    resultado.replaceChildren();
    input.focus();
  };

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const consulta = sinTildes(input.value);
    error.hidden = true; input.removeAttribute('aria-invalid');
    document.querySelectorAll('#lista-avisos .aviso').forEach((a) => a.classList.remove('resaltado'));
    if (consulta.length < 3) { mostrarError('Escribe al menos 3 letras de tu zona o distrito.'); return; }

    resultado.replaceChildren(elemento('p', {}, 'Buscando tu zona…'));
    const { datos: sectores } = await datos;
    const encontrados = sectores.filter((s) => sinTildes(s.nombre).includes(consulta) || sinTildes(s.distrito).includes(consulta));
    if (!encontrados.length) {
      mostrarError(`No encontramos «${input.value.trim()}». Prueba con el nombre de tu distrito, por ejemplo Chiclayo o Monsefú.`);
      return;
    }

    const tarjetas = encontrados.slice(0, 4).map((s) => {
      const estado = estadoZona(s);
      const avisosHoy = AVISOS.filter((a) => a.dia === 0 && a.tipo !== 'normal' && avisoDe(a, s.distrito));
      const textoAviso = avisosHoy.length ? avisosHoy.map((a) => TIPO_AVISO[a.tipo]).join(', ') : 'sin cortes programados hoy';
      return elemento('article', { class: 'tarjeta-zona' },
        elemento('span', { class: `gota-grande ${estado.clase}`, 'aria-hidden': 'true' }),
        elemento('h3', {}, `${s.nombre}, ${s.distrito}`),
        elemento('p', { class: 'estado-zona' }, estado.largo),
        elemento('p', { class: 'detalle-zona' }, `Avisos en ${s.distrito}: ${textoAviso}. `,
          elemento('a', { href: `avisos.html?distrito=${encodeURIComponent(s.distrito)}` }, `Ver los avisos de ${s.distrito}`)));
    });
    const mas = encontrados.length > 4 ? [elemento('p', {}, `Y ${encontrados.length - 4} zonas más. Escribe un nombre más exacto.`)] : [];
    const enlace = elemento('a', { class: 'enlace-flecha', href: enCisterna ? '#semana' : 'cisterna.html#semana' }, 'Ver la cisterna de toda la semana');
    resultado.replaceChildren(...tarjetas, ...mas, enlace);

    encontrados.forEach((s) => document.querySelectorAll('#lista-avisos .aviso').forEach((li) => {
      if ((li.dataset.distritos ?? '').split('|').some((d) => sinTildes(d) === sinTildes(s.distrito))) li.classList.add('resaltado');
    }));
  });
}

/* ---------------------------------------------------------------- Página "¿Pasa hoy la cisterna?": la semana */
function textoRuta(r) {
  const avance = `${r.paradasAtendidas} de ${r.totalParadas} paradas atendidas`;
  switch (r.estado) {
    case 'EN_CURSO': return { clase: 'en-camino', texto: `Repartiendo ahora · ${avance}` };
    case 'COMPLETADA': return { clase: 'terminada', texto: `Ya pasó · ${avance}` };
    case 'INTERRUMPIDA': return { clase: 'interrumpida', texto: `Se detuvo · EPSEL vuelve a programar las paradas que faltaron` };
    default: return { clase: '', texto: `Programada · ${r.totalParadas} paradas` };
  }
}

function iniciarSemana(cronograma) {
  const contenedor = document.getElementById('semana-dias');
  if (!contenedor) return;
  cronograma.then(({ datos: rutas, ejemplo }) => {
    const dias = Array.from({ length: 7 }, (_, i) => iso(sumarDias(i)));
    const pintar = (distrito) => {
      const elegidas = rutas.filter((r) => !distrito || sinTildes(r.distrito) === sinTildes(distrito));
      contenedor.replaceChildren(...dias.map((dia) => {
        const delDia = elegidas.filter((r) => r.fecha === dia).sort((a, b) => a.turno.localeCompare(b.turno));
        return elemento('section', { class: `dia${diasHasta(dia) === 0 ? ' dia-hoy' : ''}`, 'aria-labelledby': `dia-${dia}` },
          elemento('h3', { id: `dia-${dia}` }, nombreDelDia(dia)),
          delDia.length
            ? elemento('ul', { class: 'lista-gotas lista-rutas' }, delDia.map((r) => {
              const estado = textoRuta(r);
              const porcentaje = r.totalParadas ? Math.round((r.paradasAtendidas / r.totalParadas) * 100) : 0;
              return elemento('li', { class: 'aviso ruta' },
                elemento('span', { class: `gota-vineta gota-cisterna ${estado.clase}`, 'aria-hidden': 'true' }),
                elemento('div', {},
                  elemento('p', { class: 'aviso-titulo' }, elemento('strong', {}, r.sectorNombre), ' ', elemento('span', {}, r.distrito)),
                  elemento('p', {}, `${mayuscula(TURNO[r.turno] ?? r.turno)} · Cisterna `, elemento('span', { class: 'placa' }, r.cisternaPlaca ?? 'por asignar')),
                  elemento('p', { class: 'ruta-estado' }, estado.texto),
                  r.estado === 'EN_CURSO' || r.estado === 'COMPLETADA'
                    ? elemento('span', { class: 'avance', 'aria-hidden': 'true' }, elemento('span', { style: `width:${porcentaje}%` }))
                    : null));
            }))
            : elemento('p', { class: 'dia-vacio' }, distrito ? `Sin visitas en ${distrito}.` : 'Sin visitas programadas.'));
      }));
      const total = elegidas.length;
      document.getElementById('semana-cuenta').textContent =
        `${total === 1 ? '1 visita' : `${total} visitas`} de cisterna en los próximos 7 días${distrito ? ` en ${distrito}` : ''}.`;
    };
    const distritos = [...new Set(rutas.map((r) => r.distrito))].sort((a, b) => a.localeCompare(b, 'es'));
    pintar(crearFiltro(document.getElementById('filtro-semana'), distritos, pintar));
    mostrarNota('nota-semana', ejemplo, 'Estas visitas son de ejemplo: no pudimos conectar con el sistema de cisternas en este momento.');
  });
}

/* ---------------------------------------------------------------- Página de información pública: cifras y zonas */
function litrosEnPalabras(l) {
  if (l >= 1e6) return { cifra: `${numero.format(Math.round(l / 1e5) / 10)} millones`, unidad: 'de litros' };
  return { cifra: numero.format(l), unidad: 'litros' };
}

function pintarCifras(r) {
  const cerradas = r.rutas.completadas + r.rutas.interrumpidas;
  const litros = litrosEnPalabras(r.litrosDistribuidos);
  const cifras = [
    ['cumplimiento', r.paradas.cumplimientoPorcentaje === null ? 'Sin datos' : `${numero.format(Math.round(r.paradas.cumplimientoPorcentaje))} %`,
      r.paradas.cumplimientoPorcentaje === null ? 'aún no hay rutas terminadas en este periodo.' : `de las ${numero.format(r.paradas.total)} paradas programadas recibieron agua.`],
    ['litros', litros.cifra, `${litros.unidad} repartidos en cisterna.`],
    ['rutas', `${numero.format(r.rutas.completadas)} de ${numero.format(cerradas)}`, 'rutas terminaron completas; las demás se detuvieron y se volvieron a programar.'],
    ['calificacion', r.calificacion.promedio === null ? 'Sin notas' : `${numero.format(Math.round(r.calificacion.promedio * 10) / 10)} de 5`,
      r.calificacion.promedio === null ? 'los vecinos aún no califican entregas.' : `estrellas en promedio, de ${numero.format(r.calificacion.total)} calificaciones de vecinos.`],
    ['incidencias', `${numero.format(r.incidencias.resueltas)} de ${numero.format(r.incidencias.reportadas)}`, 'reportes de vecinos y conductores ya fueron resueltos.'],
    ['flota', `${numero.format(r.flota.disponiblesHoy)} de ${numero.format(r.flota.total)}`, 'cisternas listas hoy: en buen estado y con permiso sanitario vigente.'],
  ];
  cifras.forEach(([id, cifra, texto]) => {
    const caja = document.querySelector(`[data-cifra="${id}"]`);
    caja.querySelector('.numero-cifra').textContent = cifra;
    caja.querySelector('.numero-texto').textContent = texto;
  });
  document.getElementById('cifras-periodo').textContent =
    `Del ${fechaSinDia.format(desdeIso(r.desde))} al ${fechaSinDia.format(desdeIso(r.hasta))}.`;
}

function pintarZonas(sectores) {
  const cuerpo = document.getElementById('tabla-zonas');
  const orden = [...sectores].sort((a, b) => (a.cumplimientoPorcentaje ?? -1) - (b.cumplimientoPorcentaje ?? -1));
  cuerpo.replaceChildren(...orden.map((s) => {
    const c = s.cumplimientoPorcentaje;
    return elemento('tr', {},
      elemento('th', { scope: 'row' }, elemento('span', { class: 'zona-nombre' }, s.nombre), elemento('span', { class: 'zona-distrito' }, s.distrito)),
      elemento('td', { class: 'celda-cumplimiento' }, c === null
        ? 'Sin rutas terminadas'
        : [elemento('span', { class: 'barra', 'aria-hidden': 'true' }, elemento('span', { style: `width:${Math.max(2, c)}%` })),
          elemento('span', { class: 'barra-valor' }, `${numero.format(Math.round(c))} %`)]),
      elemento('td', {}, s.calificacionPromedio === null ? 'Sin notas' : `${numero.format(Math.round(s.calificacionPromedio * 10) / 10)} de 5`),
      elemento('td', {}, s.ultimaAtencion ? mayuscula(fechaCorta.format(desdeIso(s.ultimaAtencion))) : 'Sin visitas'),
      elemento('td', {}, estadoZona(s).corto));
  }));
}

function iniciarInformacion() {
  if (document.body.dataset.pagina !== 'informacion') return;
  const estado = document.getElementById('cifras-estado');
  const consultar = async (dias) => {
    const desde = iso(sumarDias(-(dias - 1)));
    const hasta = iso(sumarDias(0));
    document.querySelector('.cifras').setAttribute('aria-busy', 'true');
    estado.textContent = `Cargando las cifras de los últimos ${dias} días…`;
    const [resumen, sectores] = await Promise.all([
      pedir(`resumen?desde=${desde}&hasta=${hasta}`, () => resumenDeEjemplo(dias)),
      pedir(`sectores?desde=${desde}&hasta=${hasta}`, sectoresDeEjemplo),
    ]);
    pintarCifras(resumen.datos);
    pintarZonas(sectores.datos);
    document.querySelector('.cifras').removeAttribute('aria-busy');
    estado.textContent = `Mostrando las cifras de los últimos ${dias} días.`;
    mostrarNota('nota-cifras', resumen.ejemplo || sectores.ejemplo,
      'Estas cifras son de ejemplo: no pudimos conectar con el sistema de cisternas en este momento.');
  };
  const botones = [...document.querySelectorAll('[data-periodo]')];
  botones.forEach((b) => b.addEventListener('click', () => {
    botones.forEach((o) => o.setAttribute('aria-pressed', String(o === b)));
    consultar(Number(b.dataset.periodo));
  }));
  consultar(30);
}

/* ---------------------------------------------------------------- Acordeón del recorrido */
function iniciarAcordeon() {
  document.querySelectorAll('[data-acordeon]').forEach((acordeon) => {
    const etapas = [...acordeon.querySelectorAll('.etapa')];
    etapas.forEach((etapa) => {
      const boton = etapa.querySelector('.etapa-cabeza button');
      boton.addEventListener('click', () => {
        if (boton.getAttribute('aria-expanded') === 'true') return;
        etapas.forEach((otra) => {
          const b = otra.querySelector('.etapa-cabeza button');
          const abierta = otra === etapa;
          otra.classList.toggle('abierta', abierta);
          b.setAttribute('aria-expanded', String(abierta));
          document.getElementById(b.getAttribute('aria-controls')).hidden = !abierta;
        });
      });
    });
  });
}

/* ---------------------------------------------------------------- Índice con vista previa */
function iniciarIndice() {
  document.querySelectorAll('[data-indice]').forEach((indice) => {
    const vistas = [...indice.querySelectorAll('.vista')];
    const activar = (id) => vistas.forEach((v) => v.classList.toggle('activa', v.id === id));
    indice.querySelectorAll('[data-vista]').forEach((a) => {
      a.addEventListener('mouseenter', () => activar(a.dataset.vista));
      a.addEventListener('focus', () => activar(a.dataset.vista));
    });
  });
}

/* ---------------------------------------------------------------- Formulario: WhatsApp con el mensaje listo */
function iniciarFormulario() {
  const form = document.querySelector('[data-formulario-reclamo]');
  if (!form) return;
  const requeridos = [
    { campo: form.elements.nombre, mensaje: 'Escribe tu nombre.' },
    { campo: form.elements.motivo, mensaje: 'Elige qué pasó.' },
  ];
  form.addEventListener('submit', (e) => {
    e.preventDefault();
    let primero = null;
    requeridos.forEach(({ campo, mensaje }) => {
      const error = document.getElementById(`${campo.id}-error`);
      const vacio = !campo.value.trim();
      error.hidden = !vacio; error.textContent = vacio ? mensaje : '';
      if (vacio) { campo.setAttribute('aria-invalid', 'true'); primero ??= campo; } else campo.removeAttribute('aria-invalid');
    });
    if (primero) { primero.focus(); return; }
    const f = form.elements;
    const texto = [`Hola EPSEL, soy ${f.nombre.value.trim()}.`, `Motivo: ${f.motivo.value}.`,
      f.suministro.value.trim() && `Suministro: ${f.suministro.value.trim()}.`, f.mensaje.value.trim()].filter(Boolean).join('\n');
    window.open(`https://wa.me/?text=${encodeURIComponent(texto)}`, '_blank', 'noopener');
  });
}

/* ---------------------------------------------------------------- Aparición al desplazarse */
function iniciarMovimiento() {
  if (matchMedia('(prefers-reduced-motion: reduce)').matches || !('IntersectionObserver' in window)) return;
  const grupos = document.querySelectorAll('.seccion, .pie');
  const observador = new IntersectionObserver((entradas) => entradas.forEach((en) => {
    if (!en.isIntersecting) return;
    en.target.classList.add('visible');
    observador.unobserve(en.target);
  }), { rootMargin: '0px 0px -12% 0px' });
  grupos.forEach((grupo) => {
    const partes = grupo.querySelectorAll(':scope > *, .numeros-par > *, .pagos-lista > *, .indice-lista > li, .nosotros-fotos > *, .documentos > *');
    [...partes].filter((p) => !p.matches('.pie-marca, [data-sin-movimiento]')).forEach((p, i) => {
      p.classList.add('aparece'); p.style.setProperty('--retraso', `${Math.min(i, 8) * 75}ms`); observador.observe(p);
    });
  });
}

/* ---------------------------------------------------------------- Inicio */
document.querySelectorAll('[data-hoy]').forEach((el) => { el.textContent = `Hoy, ${fechaCorta.format(hoy)}`; });
iniciarMenu();
pintarAvisosDeHoy();
iniciarPaginaAvisos();
iniciarAcordeon();
iniciarIndice();
iniciarFormulario();
iniciarInformacion();
if (document.querySelector('.buscador, #lista-horarios')) {
  const sectores = pedir('sectores', sectoresDeEjemplo);
  iniciarBuscador(sectores);
  sectores.then(({ datos, ejemplo }) => pintarHorarios(datos, ejemplo));
}
if (document.getElementById('semana-dias')) iniciarSemana(pedir('cronograma', cronogramaDeEjemplo));
iniciarMovimiento();
