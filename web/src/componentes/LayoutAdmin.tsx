import { NavLink, Outlet } from 'react-router-dom';

import { ProveedorTiempoReal, useEstadoTiempoReal } from '../api/tiempoReal';
import { useSesion } from '../auth/ContextoSesion';
import { AlertasIncidencias } from './AlertasIncidencias';

/** Secciones del panel; se agregan a medida que se implementan (ver CLAUDE.md, sección 4). */
const SECCIONES: { ruta: string; etiqueta: string; exacta?: boolean }[] = [
  { ruta: '/admin', etiqueta: 'Inicio', exacta: true },
  { ruta: '/admin/monitoreo', etiqueta: 'Mapa en vivo' },
  { ruta: '/admin/rutas', etiqueta: 'Rutas' },
  { ruta: '/admin/incidencias', etiqueta: 'Incidencias' },
  { ruta: '/admin/capacidad', etiqueta: 'Capacidad' },
  { ruta: '/admin/calificaciones', etiqueta: 'Calificaciones' },
  { ruta: '/admin/catalogos', etiqueta: 'Catálogos' },
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
  return (
    <div className="layout">
      <header className="barra">
        <NavLink to="/admin" className="marca">
          <img src="/gota.svg" alt="" width={22} height={22} />
          AguaYa <small>EPSEL</small>
        </NavLink>
        <div className="barra-usuario">
          <span className={`conexion conexion-${conexion}`} title="Alertas y posiciones en tiempo real">
            {TEXTO_CONEXION[conexion]}
          </span>
          <span className="nombre-usuario">
            {usuario?.nombres} {usuario?.apellidos}
          </span>
          <button type="button" className="boton boton-secundario" onClick={() => void cerrarSesion()}>
            Cerrar sesión
          </button>
        </div>
      </header>
      <nav className="menu" aria-label="Secciones del panel">
        {SECCIONES.map((s) => (
          <NavLink key={s.ruta} to={s.ruta} end={s.exacta}>
            {s.etiqueta}
          </NavLink>
        ))}
        <a href="/transparencia" target="_blank" rel="noreferrer">
          Panel público ↗
        </a>
      </nav>
      <main className="contenido">
        <Outlet />
      </main>
      <AlertasIncidencias />
    </div>
  );
}
