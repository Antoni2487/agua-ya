import { NavLink, Outlet } from 'react-router-dom';

import { ProveedorTiempoReal, useEstadoTiempoReal } from '../api/tiempoReal';
import { useSesion } from '../auth/ContextoSesion';
import { AlertasIncidencias } from './AlertasIncidencias';
import { WEB_PUBLICA_URL } from '../utilidades';
import { Icono, type NombreIcono } from './Icono';

/** Secciones del panel (ver CLAUDE.md, sección 4). */
const SECCIONES: { ruta: string; etiqueta: string; icono: NombreIcono; exacta?: boolean }[] = [
  { ruta: '/admin', etiqueta: 'Inicio', icono: 'inicio', exacta: true },
  { ruta: '/admin/monitoreo', etiqueta: 'Mapa en vivo', icono: 'mapa' },
  { ruta: '/admin/rutas', etiqueta: 'Rutas', icono: 'rutas' },
  { ruta: '/admin/incidencias', etiqueta: 'Incidencias', icono: 'incidencias' },
  { ruta: '/admin/capacidad', etiqueta: 'Capacidad', icono: 'capacidad' },
  { ruta: '/admin/calificaciones', etiqueta: 'Calificaciones', icono: 'calificaciones' },
  { ruta: '/admin/catalogos', etiqueta: 'Catálogos', icono: 'catalogos' },
];

export function LayoutAdmin() {
  return (
    <ProveedorTiempoReal>
      <Contenido />
    </ProveedorTiempoReal>
  );
}

const TEXTO_CONEXION = { conectado: 'En vivo', conectando: 'Conectando…', desconectado: 'Sin conexión en vivo' };

function Contenido() {
  const { usuario, cerrarSesion } = useSesion();
  const conexion = useEstadoTiempoReal();
  const iniciales = `${usuario?.nombres?.[0] ?? ''}${usuario?.apellidos?.[0] ?? ''}`;
  return (
    <div className="layout">
      <aside className="lateral">
        <NavLink to="/admin" className="marca" aria-label="AguaYa, inicio del panel">
          <img src="/logo-epsel.webp" alt="EPSEL S.A." width={88} height={60} />
          <span className="marca-texto">
            AguaYa
            <small>Cisternas · Administración</small>
          </span>
        </NavLink>
        <nav className="menu" aria-label="Secciones del panel">
          {SECCIONES.map((s) => (
            <NavLink key={s.ruta} to={s.ruta} end={s.exacta}>
              <Icono nombre={s.icono} />
              <span>{s.etiqueta}</span>
            </NavLink>
          ))}
          <a href={WEB_PUBLICA_URL} target="_blank" rel="noreferrer" className="menu-externo">
            <Icono nombre="publico" />
            <span>
              Web de EPSEL <span aria-hidden="true">↗</span>
              <span className="solo-lector"> (se abre en otra pestaña)</span>
            </span>
          </a>
        </nav>
        <div className="lateral-pie">
          <span className={`conexion conexion-${conexion}`} title="Alertas y posiciones en tiempo real">
            {TEXTO_CONEXION[conexion]}
          </span>
          <div className="usuario">
            <span className="avatar" aria-hidden="true">{iniciales}</span>
            <span className="nombre-usuario">
              {usuario?.nombres} {usuario?.apellidos}
              <small>Administrador</small>
            </span>
          </div>
          <button type="button" className="boton boton-salir" onClick={() => void cerrarSesion()}>
            <Icono nombre="salir" tamano={18} />
            Cerrar sesión
          </button>
        </div>
      </aside>
      <main className="contenido">
        <Outlet />
      </main>
      <AlertasIncidencias />
    </div>
  );
}
