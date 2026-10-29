import { useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';

import { useSuscripcion } from '../api/tiempoReal';
import type { Incidencia } from '../api/tipos';
import { TIPOS_INCIDENCIA } from '../utilidades';

const DURACION_MS = 20_000;
const ROL: Record<string, string> = { CONDUCTOR: 'Conductor', VECINO: 'Vecino', ADMIN: 'Admin' };

/** RF06: cada incidencia reportada aparece al instante en cualquier pantalla del panel. */
export function AlertasIncidencias() {
  const [alertas, setAlertas] = useState<Incidencia[]>([]);
  const clienteConsultas = useQueryClient();

  useSuscripcion<Incidencia>('/topic/admin/incidencias', (incidencia) => {
    setAlertas((actuales) => [incidencia, ...actuales.filter((a) => a.id !== incidencia.id)].slice(0, 4));
    void clienteConsultas.invalidateQueries({ queryKey: ['incidencias'] });
    void clienteConsultas.invalidateQueries({ queryKey: ['ruta', incidencia.rutaId] });
  });

  const cerrar = useCallback((id: number) => setAlertas((actuales) => actuales.filter((a) => a.id !== id)), []);

  useEffect(() => {
    if (alertas.length === 0) return;
    const temporizador = setTimeout(() => setAlertas((actuales) => actuales.slice(0, -1)), DURACION_MS);
    return () => clearTimeout(temporizador);
  }, [alertas]);

  return (
    <div className="alertas" aria-live="assertive">
      {alertas.map((a) => (
        <div key={a.id} className="alerta-incidencia" role="alert">
          <div>
            <strong>
              {TIPOS_INCIDENCIA[a.tipo]} · {a.sectorNombre}
            </strong>
            <span>
              {ROL[a.reportadoPorRol] ?? a.reportadoPorRol}
              {a.ordenParada !== null && ` · parada ${a.ordenParada}`}
              {a.estadoRuta === 'INTERRUMPIDA' && ' · ruta interrumpida'}
            </span>
            {a.descripcion && <span className="texto-suave">“{a.descripcion}”</span>}
            <Link to={`/admin/incidencias/${a.id}`} onClick={() => cerrar(a.id)}>
              Atender incidencia
            </Link>
          </div>
          <button type="button" aria-label="Cerrar alerta" onClick={() => cerrar(a.id)}>
            ×
          </button>
        </div>
      ))}
    </div>
  );
}
