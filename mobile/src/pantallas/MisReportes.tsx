import { useEffect, useState } from 'react';
import { api, textoError } from '../api/cliente';
import type { Incidencia, Pagina } from '../api/tipos';
import { Aviso, Cargando, Etiqueta, Fila, Pantalla, Tarjeta, Texto } from '../componentes/ui';
import { cuando, hora, NOMBRE_INCIDENCIA } from '../textos';

const ESTADO = {
  ABIERTA: { texto: 'En revisión', tono: 'aviso' as const },
  REASIGNADA: { texto: 'Resuelto: otra cisterna irá', tono: 'bien' as const },
  CERRADA: { texto: 'Cerrado por EPSEL', tono: 'neutro' as const },
};

export function MisReportes({ alVolver }: { alVolver: () => void }) {
  const [lista, setLista] = useState<Incidencia[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api<Pagina<Incidencia>>('/api/incidencias?size=50').then((p) => setLista(p.contenido)).catch((e) => setError(textoError(e)));
  }, []);

  return (
    <Pantalla titulo="Mis reportes" alVolver={alVolver}>
      <Aviso texto={error} />
      {!lista && !error ? <Cargando /> : null}
      {lista?.length === 0 ? <Texto tipo="suave">Todavía no has reportado ningún problema.</Texto> : null}
      {lista?.map((i) => (
        <Tarjeta key={i.id}>
          <Texto tipo="subtitulo">{NOMBRE_INCIDENCIA[i.tipo]}</Texto>
          <Fila><Etiqueta texto={ESTADO[i.estado].texto} tono={ESTADO[i.estado].tono} /></Fila>
          {i.descripcion ? <Texto>{i.descripcion}</Texto> : null}
          <Texto tipo="pequeno">{i.sectorNombre} · {cuando(i.creadoEn)}, {hora(i.creadoEn)}</Texto>
        </Tarjeta>
      ))}
    </Pantalla>
  );
}
