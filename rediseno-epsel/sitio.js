// Rediseño EPSEL (propuesta académica PA3). Sin librerías: el sitio debe cargar rápido en un celular gama baja.
// Todo lo de esta página funciona sin JavaScript (menú visible, acordeones abiertos con <details> o visibles);
// el script agrega el menú desplegable, la búsqueda de zona, el acordeón del recorrido y el movimiento.

// URL del backend de AguaYa (panel público de transparencia). Con ?datos=ejemplo se usan datos de ejemplo.
const API = window.EPSEL_API ?? 'https://aguaya-backend.onrender.com';
const ESPERA_MS = 6000;

const hoy = new Date();
const fechaCorta = new Intl.DateTimeFormat('es-PE', { weekday: 'long', day: 'numeric', month: 'long' });
const TURNO = { MANANA: 'turno mañana', TARDE: 'turno tarde' };

/* ---------------------------------------------------------------- Menú */
function iniciarMenu() {
  const botonMenu = document.querySelector('.boton-menu');
  const nav = document.getElementById('navegacion');
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

/* ---------------------------------------------------------------- Datos de las zonas */
function datosDeEjemplo() {
  const dia = (n) => { const d = new Date(hoy); d.setDate(d.getDate() + n); return d.toISOString().slice(0, 10); };
  const sector = (nombre, distrito, n, turno, estado) =>
    ({ nombre, distrito, proximaAtencion: n === null ? null : { fecha: dia(n), turno, estado } });
  return [
    sector('Cerropón', 'Chiclayo', 0, 'MANANA', 'EN_CURSO'),
    sector('P.J. Atusparias', 'José Leonardo Ortiz', 0, 'TARDE', 'PROGRAMADA'),
    sector('P.J. Urrunaga', 'José Leonardo Ortiz', 0, 'TARDE', 'PROGRAMADA'),
    sector('Valle Hermoso', 'Monsefú', 1, 'MANANA', 'PROGRAMADA'),
    sector('Callanca', 'Monsefú', 2, 'MANANA', 'PROGRAMADA'),
  ];
}

async function cargarSectores() {
  if (new URLSearchParams(location.search).get('datos') === 'ejemplo') return { sectores: datosDeEjemplo(), ejemplo: true };
  try {
    const control = new AbortController();
    const reloj = setTimeout(() => control.abort(), ESPERA_MS);
    const r = await fetch(`${API}/api/public/transparencia/sectores`, { signal: control.signal });
    clearTimeout(reloj);
    if (!r.ok) throw new Error(r.status);
    const sectores = await r.json();
    if (!Array.isArray(sectores) || !sectores.length) throw new Error('vacío');
    return { sectores, ejemplo: false };
  } catch {
    return { sectores: datosDeEjemplo(), ejemplo: true };
  }
}

const sinTildes = (t) => t.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
  .replace(/\bp\.?\s?j\.?\s*/g, '').replace(/\s+/g, ' ').trim();

function diasHasta(fechaIso) {
  const [a, m, d] = fechaIso.split('-').map(Number);
  const objetivo = new Date(a, m - 1, d);
  const base = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
  return Math.round((objetivo - base) / 86400000);
}

// Texto del estado de una zona, siempre en palabras (el color solo acompaña)
function estadoZona(s) {
  const p = s.proximaAtencion;
  if (!p) return { clase: 'sin-visita', corto: 'Sin visita de cisterna programada', largo: 'Por ahora no hay una visita de cisterna programada para esta zona.' };
  const n = diasHasta(p.fecha);
  const turno = TURNO[p.turno] ?? p.turno.toLowerCase();
  if (p.estado === 'EN_CURSO') return { clase: 'en-camino', corto: `En camino ahora, ${turno}`, largo: `La cisterna está repartiendo ahora, en el ${turno}.` };
  if (n === 0) return { clase: '', corto: `Hoy, ${turno}`, largo: `Hoy pasa la cisterna, en el ${turno}.` };
  if (n === 1) return { clase: '', corto: `Mañana, ${turno}`, largo: `Mañana pasa la cisterna, en el ${turno}.` };
  const [a, m, d] = p.fecha.split('-').map(Number);
  const dia = fechaCorta.format(new Date(a, m - 1, d));
  return { clase: '', corto: `${dia[0].toUpperCase()}${dia.slice(1)}, ${turno}`, largo: `La próxima cisterna pasa el ${dia}, en el ${turno}.` };
}

function elemento(etiqueta, atributos = {}, ...hijos) {
  const el = document.createElement(etiqueta);
  Object.entries(atributos).forEach(([k, v]) => (k === 'class' ? (el.className = v) : el.setAttribute(k, v)));
  hijos.flat().forEach((h) => el.append(h));
  return el;
}

function pintarHorarios(sectores, ejemplo) {
  const lista = document.getElementById('lista-horarios');
  const orden = [...sectores].sort((a, b) =>
    (a.proximaAtencion?.fecha ?? '9999').localeCompare(b.proximaAtencion?.fecha ?? '9999')
    || (a.proximaAtencion?.turno ?? '').localeCompare(b.proximaAtencion?.turno ?? ''));
  lista.replaceChildren(...orden.map((s) => {
    const estado = estadoZona(s);
    return elemento('li', { class: 'aviso' },
      elemento('span', { class: 'gota-vineta gota-cisterna', 'aria-hidden': 'true' }),
      elemento('div', {},
        elemento('p', { class: 'aviso-titulo' }, elemento('strong', {}, s.nombre), elemento('span', {}, s.distrito)),
        elemento('p', {}, estado.corto)));
  }));
  const conVisita = sectores.filter((s) => s.proximaAtencion).length;
  document.getElementById('horarios-cuenta').textContent = `${conVisita} de ${sectores.length} zonas con visita`;
  const nota = document.getElementById('nota-datos');
  nota.hidden = !ejemplo;
  nota.textContent = ejemplo ? 'Estos horarios son de ejemplo: no pudimos conectar con el sistema de cisternas en este momento.' : '';

  const opciones = new Set([...sectores.map((s) => s.nombre), ...sectores.map((s) => s.distrito)]);
  document.getElementById('lista-zonas').replaceChildren(...[...opciones].map((v) => elemento('option', { value: v })));
}

/* ---------------------------------------------------------------- Buscador de la portada */
function iniciarBuscador(datos) {
  const form = document.querySelector('.buscador');
  const input = document.getElementById('zona');
  const error = document.getElementById('zona-error');
  const resultado = document.getElementById('resultado');
  const avisos = [...document.querySelectorAll('#lista-avisos .aviso')];

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
    avisos.forEach((a) => a.classList.remove('resaltado'));
    if (consulta.length < 3) { mostrarError('Escribe al menos 3 letras de tu zona o distrito.'); return; }

    resultado.replaceChildren(elemento('p', {}, 'Buscando tu zona…'));
    const { sectores } = await datos;
    const encontrados = sectores.filter((s) => sinTildes(s.nombre).includes(consulta) || sinTildes(s.distrito).includes(consulta));
    if (!encontrados.length) {
      mostrarError(`No encontramos «${input.value.trim()}». Prueba con el nombre de tu distrito, por ejemplo Chiclayo o Monsefú.`);
      return;
    }

    const tarjetas = encontrados.slice(0, 4).map((s) => {
      const estado = estadoZona(s);
      const aviso = avisos.find((a) => sinTildes(a.dataset.distrito ?? '') === sinTildes(s.distrito));
      const textoAviso = aviso ? aviso.querySelector('.aviso-titulo strong').textContent : 'Sin cortes programados hoy';
      return elemento('article', { class: 'tarjeta-zona' },
        elemento('span', { class: `gota-grande ${estado.clase}`, 'aria-hidden': 'true' }),
        elemento('h3', {}, `${s.nombre}, ${s.distrito}`),
        elemento('p', { class: 'estado-zona' }, estado.largo),
        elemento('p', { class: 'detalle-zona' }, `Avisos en ${s.distrito}: ${textoAviso}.`));
    });
    const mas = encontrados.length > 4 ? [elemento('p', {}, `Y ${encontrados.length - 4} zonas más. Escribe un nombre más exacto.`)] : [];
    const enlace = elemento('a', { class: 'enlace-flecha', href: '#avisos' }, 'Ver todos los avisos y horarios');
    resultado.replaceChildren(...tarjetas, ...mas, enlace);

    encontrados.forEach((s) => avisos
      .filter((a) => sinTildes(a.dataset.distrito ?? '') === sinTildes(s.distrito))
      .forEach((a) => a.classList.add('resaltado')));
  });
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
    const partes = grupo.querySelectorAll(':scope > *, .numeros-par > *, .lista-gotas > li, .pagos-lista > *, .indice-lista > li, .nosotros-fotos > *');
    [...partes].filter((p) => !p.matches('.pie-marca')).forEach((p, i) => { p.classList.add('aparece'); p.style.setProperty('--retraso', `${Math.min(i, 8) * 75}ms`); observador.observe(p); });
  });
}

/* ---------------------------------------------------------------- Inicio */
document.querySelectorAll('[data-hoy]').forEach((el) => {
  const t = fechaCorta.format(hoy); el.textContent = `Hoy, ${t}`;
});
iniciarMenu();
iniciarAcordeon();
iniciarIndice();
iniciarFormulario();
iniciarMovimiento();
const datos = cargarSectores();
iniciarBuscador(datos);
datos.then(({ sectores, ejemplo }) => pintarHorarios(sectores, ejemplo));
