import { NavLink, Outlet } from 'react-router-dom';

import { useSesion } from '../auth/ContextoSesion';

/** Secciones del panel; se agregan a medida que se implementan (ver CLAUDE.md, sección 4). */
const SECCIONES: { ruta: string; etiqueta: string }[] = [{ ruta: '/admin', etiqueta: 'Inicio' }];

export function LayoutAdmin() {
  const { usuario, cerrarSesion } = useSesion();
  return (
    <div className="layout">
      <header className="barra">
        <NavLink to="/admin" className="marca">
          <img src="/gota.svg" alt="" width={22} height={22} />
          AguaYa <small>EPSEL</small>
        </NavLink>
        <div className="barra-usuario">
          <span>
            {usuario?.nombres} {usuario?.apellidos}
          </span>
          <button type="button" className="boton boton-secundario" onClick={() => void cerrarSesion()}>
            Cerrar sesión
          </button>
        </div>
      </header>
      <nav className="menu" aria-label="Secciones del panel">
        {SECCIONES.map((s) => (
          <NavLink key={s.ruta} to={s.ruta} end>
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
    </div>
  );
}
