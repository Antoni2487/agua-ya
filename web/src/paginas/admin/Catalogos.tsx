import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { lazy, Suspense, useEffect, useRef, useState, type FormEvent } from 'react';
import { Navigate, NavLink, useParams, useSearchParams } from 'react-router-dom';

import { useSectores } from '../../api/catalogos';
import { api } from '../../api/cliente';
import type { Pagina } from '../../api/tipos';
import { Cargando, MensajeError } from '../../componentes/Estado';
import { Paginacion } from '../../componentes/Paginacion';
import { CATALOGOS } from './catalogos/definiciones';
import { aCuerpo, camposDe, centroDelSector, esObligatorio, valoresIniciales, type Campo, type Catalogo, type Valores } from './catalogos/modelo';

const SelectorUbicacion = lazy(() => import('../../componentes/SelectorUbicacion').then((m) => ({ default: m.SelectorUbicacion })));

type Registro = { id: number } & Record<string, unknown>;
const TIPOS_INPUT: Partial<Record<NonNullable<Campo['tipo']>, string>> = { numero: 'number', coordenada: 'number', fecha: 'date', password: 'password' };
type Edicion = { modo: 'crear' } | { modo: 'editar'; item: Registro };

const mayuscula = (texto: string) => texto.charAt(0).toUpperCase() + texto.slice(1);
/** Concordancia de género con el nombre del catálogo: "Nuevo sector", "Nueva cisterna", "guardada". */
const genero = (catalogo: Catalogo<never>, masculino: string, femenino: string) =>
  catalogo.singular.articulo === 'la' ? femenino : masculino;

/** CRUD de sectores, instituciones, puntos de entrega y de llenado, cisternas y conductores (sección 4). */
export function Catalogos() {
  const { tipo } = useParams();
  const catalogo = CATALOGOS.find((c) => c.ruta === tipo);
  if (!catalogo) return <Navigate to={`/admin/catalogos/${CATALOGOS[0]!.ruta}`} replace />;
  return (
    <section>
      <h1>Catálogos</h1>
      <nav className="pestanas" aria-label="Catálogos">
        {CATALOGOS.map((c) => (
          <NavLink key={c.ruta} to={`/admin/catalogos/${c.ruta}`}>
            {c.titulo}
          </NavLink>
        ))}
      </nav>
      {/* La key reinicia filtros abiertos y formularios al cambiar de catálogo. */}
      <ListadoCatalogo key={catalogo.ruta} catalogo={catalogo} />
    </section>
  );
}

function ListadoCatalogo({ catalogo }: { catalogo: Catalogo<never> }) {
  const [params, setParams] = useSearchParams();
  const page = Number(params.get('page') ?? 0);
  const filtros = Object.fromEntries(catalogo.filtros.map((f) => [f.nombre, params.get(f.nombre) ?? '']));
  const [edicion, setEdicion] = useState<Edicion | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const sectores = useSectores();
  const clienteConsultas = useQueryClient();
  const { nombre } = catalogo.singular;

  const listado = useQuery({
    queryKey: ['catalogo', catalogo.ruta, 'lista', { ...filtros, page }],
    queryFn: ({ signal }) =>
      api<Pagina<Registro>>(catalogo.endpoint, { parametros: { ...filtros, page, size: 20, sort: catalogo.orden }, signal }),
    placeholderData: keepPreviousData,
  });

  const baja = useMutation({
    mutationFn: (item: Registro) => api<void>(`${catalogo.endpoint}/${item.id}`, { metodo: 'DELETE' }),
    onSuccess: (_, item) => {
      const accion = catalogo.baja.etiqueta === 'Eliminar' ? genero(catalogo, 'eliminado', 'eliminada') : genero(catalogo, 'desactivado', 'desactivada');
      setAviso(`${mayuscula(nombre)} ${catalogo.nombreDe(item as never)} ${accion}.`);
      void clienteConsultas.invalidateQueries({ queryKey: ['catalogo'] });
    },
  });

  function filtrar(cambios: Record<string, string>) {
    const siguiente = new URLSearchParams(params);
    Object.entries(cambios).forEach(([k, v]) => (v ? siguiente.set(k, v) : siguiente.delete(k)));
    if (!('page' in cambios)) siguiente.delete('page');
    setParams(siguiente, { replace: true });
  }

  function abrir(e: Edicion) {
    setAviso(null);
    baja.reset();
    setEdicion(e);
  }

  return (
    <>
      <div className="encabezado encabezado-catalogo">
        <p className="texto-suave">{catalogo.descripcion}</p>
        <button
          type="button"
          className="boton"
          onClick={() => (edicion?.modo === 'crear' ? setEdicion(null) : abrir({ modo: 'crear' }))}
          aria-expanded={edicion?.modo === 'crear'}
        >
          {edicion?.modo === 'crear' ? 'Cerrar' : `${genero(catalogo, 'Nuevo', 'Nueva')} ${nombre}`}
        </button>
      </div>

      {edicion && (
        <Formulario
          key={edicion.modo === 'editar' ? edicion.item.id : 'nuevo'}
          catalogo={catalogo}
          item={edicion.modo === 'editar' ? edicion.item : null}
          alCerrar={() => setEdicion(null)}
          alGuardar={(item) => {
            setEdicion(null);
            setAviso(`${mayuscula(nombre)} ${catalogo.nombreDe(item as never)} ${genero(catalogo, 'guardado', 'guardada')}.`);
          }}
        />
      )}

      {aviso && (
        <div className="alerta alerta-exito resultado" role="status">
          {aviso}
        </div>
      )}
      {baja.isError && <MensajeError error={baja.error} />}

      {catalogo.filtros.length > 0 && (
        <div className="filtros">
          {catalogo.filtros.map((f) => (
            <label key={f.nombre}>
              {f.etiqueta}
              <select value={filtros[f.nombre]} onChange={(e) => filtrar({ [f.nombre]: e.target.value })}>
                <option value="">Todos</option>
                {f.opciones === 'activo' ? (
                  <>
                    <option value="true">Activos</option>
                    <option value="false">Inactivos</option>
                  </>
                ) : f.opciones === 'sector' ? (
                  sectores.data?.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.nombre}
                    </option>
                  ))
                ) : (
                  Object.entries(f.opciones).map(([v, e]) => (
                    <option key={v} value={v}>
                      {e}
                    </option>
                  ))
                )}
              </select>
            </label>
          ))}
        </div>
      )}

      {listado.isPending && <Cargando />}
      {listado.isError && <MensajeError error={listado.error} reintentar={() => void listado.refetch()} />}
      {listado.data?.contenido.length === 0 && <p className="texto-suave">No hay registros con esos filtros.</p>}
      {listado.data && listado.data.contenido.length > 0 && (
        <>
          <div className="tabla-contenedor">
            <table className="tabla tabla-catalogo">
              <thead>
                <tr>
                  {catalogo.columnas.map((c) => (
                    <th key={c.titulo}>{c.titulo}</th>
                  ))}
                  <th className="acciones-fila">
                    <span className="solo-lector">Acciones</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {listado.data.contenido.map((item) => (
                  <tr key={item.id} className={catalogo.activo && !catalogo.activo(item as never) ? 'fila-inactiva' : undefined}>
                    {catalogo.columnas.map((c) => (
                      <td key={c.titulo}>{c.valor(item as never)}</td>
                    ))}
                    <td className="acciones-fila">
                      <div>
                      <button type="button" className="boton boton-secundario" onClick={() => abrir({ modo: 'editar', item })}>
                        Editar<span className="solo-lector"> {catalogo.nombreDe(item as never)}</span>
                      </button>
                      {catalogo.baja.disponible(item as never) && (
                        <button
                          type="button"
                          className="boton boton-peligro"
                          disabled={baja.isPending}
                          onClick={() => {
                            setAviso(null);
                            if (window.confirm(catalogo.baja.confirmacion(item as never))) baja.mutate(item);
                          }}
                        >
                          {catalogo.baja.etiqueta}
                          <span className="solo-lector"> {catalogo.nombreDe(item as never)}</span>
                        </button>
                      )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Paginacion pagina={listado.data} alCambiar={(p) => filtrar({ page: String(p) })} />
        </>
      )}
    </>
  );
}

function Formulario({
  catalogo,
  item,
  alCerrar,
  alGuardar,
}: {
  catalogo: Catalogo<never>;
  item: Registro | null;
  alCerrar: () => void;
  alGuardar: (item: Registro) => void;
}) {
  const editando = item !== null;
  const [valores, setValores] = useState<Valores>(() => valoresIniciales(catalogo, item));
  const sectores = useSectores();
  const clienteConsultas = useQueryClient();
  const { nombre } = catalogo.singular;
  const titulo = editando ? `Editar ${nombre}` : `${genero(catalogo, 'Nuevo', 'Nueva')} ${nombre}`;

  const guardar = useMutation({
    mutationFn: () =>
      api<Registro>(editando ? `${catalogo.endpoint}/${item.id}` : catalogo.endpoint, {
        metodo: editando ? 'PUT' : 'POST',
        cuerpo: aCuerpo(catalogo, valores, editando),
      }),
    onSuccess: (guardado) => {
      void clienteConsultas.invalidateQueries({ queryKey: ['catalogo'] });
      alGuardar(guardado);
    },
  });

  // Al editar desde el final de la tabla, el formulario (arriba) queda a la vista.
  const formulario = useRef<HTMLFormElement>(null);
  useEffect(() => {
    formulario.current?.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
  }, []);

  const cambiar = (campo: string, valor: string | boolean) => setValores((v) => ({ ...v, [campo]: valor }));

  function enviar(e: FormEvent) {
    e.preventDefault();
    guardar.mutate();
  }

  const [campoLat, campoLng] = catalogo.ubicacion ?? [];
  const posicion: [number, number] | null =
    campoLat && campoLng && valores[campoLat] !== '' && valores[campoLng] !== '' && !Number.isNaN(Number(valores[campoLat])) && !Number.isNaN(Number(valores[campoLng]))
      ? [Number(valores[campoLat]), Number(valores[campoLng])]
      : null;

  return (
    <form ref={formulario} className="tarjeta formulario" onSubmit={enviar} aria-label={titulo}>
      <h2>
        {titulo}
        {editando && <span className="texto-suave"> · {catalogo.nombreDe(item as never)}</span>}
      </h2>
      <div className="campos">
        {camposDe(catalogo, editando).map((campo) => (
          <CampoFormulario
            key={campo.nombre}
            campo={campo}
            valor={valores[campo.nombre]!}
            obligatorio={esObligatorio(campo, editando)}
            sectores={sectores.data}
            alCambiar={(v) => cambiar(campo.nombre, v)}
          />
        ))}
      </div>
      {campoLat && campoLng && (
        <Suspense fallback={<div className="mapa mapa-selector mapa-cargando"><Cargando texto="Cargando mapa…" /></div>}>
          <SelectorUbicacion
            posicion={posicion}
            centro={centroDelSector(catalogo, valores, sectores.data)}
            alElegir={(lat, lng) => setValores((v) => ({ ...v, [campoLat]: String(lat), [campoLng]: String(lng) }))}
          />
        </Suspense>
      )}
      {guardar.isError && <MensajeError error={guardar.error} />}
      <div className="acciones">
        <button type="button" className="boton boton-secundario" onClick={alCerrar}>
          Cancelar
        </button>
        <button type="submit" className="boton" disabled={guardar.isPending}>
          {guardar.isPending ? 'Guardando…' : 'Guardar'}
        </button>
      </div>
    </form>
  );
}

function CampoFormulario({
  campo,
  valor,
  obligatorio,
  sectores,
  alCambiar,
}: {
  campo: Campo;
  valor: string | boolean;
  obligatorio: boolean;
  sectores: { id: number; nombre: string; activo: boolean }[] | undefined;
  alCambiar: (valor: string | boolean) => void;
}) {
  const ayuda = campo.ayuda && <small className="texto-suave">{campo.ayuda}</small>;
  if (campo.tipo === 'casilla') {
    return (
      <label className="casilla">
        <input type="checkbox" checked={valor === true} onChange={(e) => alCambiar(e.target.checked)} />
        {campo.etiqueta}
      </label>
    );
  }
  if (campo.tipo === 'select' || campo.tipo === 'sector') {
    const opciones =
      campo.tipo === 'sector'
        ? (sectores ?? []).map((s) => [String(s.id), s.activo ? s.nombre : `${s.nombre} (inactivo)`] as const)
        : Object.entries(campo.opciones ?? {});
    return (
      <label>
        {campo.etiqueta}
        <select required={obligatorio && !campo.sinValor} value={String(valor)} onChange={(e) => alCambiar(e.target.value)}>
          {(campo.sinValor || campo.tipo === 'sector') && <option value="">{campo.sinValor ?? 'Elegir…'}</option>}
          {opciones.map(([v, e]) => (
            <option key={v} value={v}>
              {e}
            </option>
          ))}
        </select>
        {ayuda}
      </label>
    );
  }
  const tipo = TIPOS_INPUT[campo.tipo ?? 'texto'] ?? 'text';
  return (
    <label>
      {campo.etiqueta}
      <input
        type={tipo}
        {...campo.atributos}
        required={obligatorio}
        value={String(valor)}
        onChange={(e) => alCambiar(e.target.value)}
      />
      {ayuda}
    </label>
  );
}
