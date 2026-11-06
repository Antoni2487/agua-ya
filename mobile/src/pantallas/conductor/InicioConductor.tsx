import { useCallback, useEffect, useState } from 'react';
import { Pressable } from 'react-native';
import { api, textoError } from '../../api/cliente';
import type { RutaDetalle } from '../../api/tipos';
import { Aviso, Boton, Cargando, Etiqueta, Fila, Pantalla, Tarjeta, Texto } from '../../componentes/ui';
import { ESTADO_RUTA, TURNO } from '../../textos';

export function InicioConductor({ nombre, alAbrirRuta, alSalir }: {
  nombre: string; alAbrirRuta: (rutaId: number) => void; alSalir: () => void;
}) {
  const [rutas, setRutas] = useState<RutaDetalle[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);

  const cargar = useCallback(async () => {
    setCargando(true); setError(null);
    try {
      setRutas(await api<RutaDetalle[]>('/api/conductor/rutas/hoy'));
    } catch (e) {
      setError(textoError(e));
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => { void cargar(); }, [cargar]);

  return (
    <Pantalla titulo={`Hola, ${nombre}`}
      pie={<Fila>
        <Boton texto="Actualizar" variante="secundario" alPresionar={cargar} cargando={cargando} estilo={{ flex: 1 }} />
        <Boton texto="Salir" variante="secundario" alPresionar={alSalir} estilo={{ flex: 1 }} />
      </Fila>}>
      <Texto tipo="titulo">Tus rutas de hoy</Texto>
      <Aviso texto={error} />
      {!rutas && !error ? <Cargando texto="Buscando tus rutas…" /> : null}
      {rutas?.length === 0 ? (
        <Tarjeta tono="claro">
          <Texto tipo="subtitulo">Hoy no tienes rutas asignadas</Texto>
          <Texto>Cuando el administrador te asigne una, aparecerá aquí.</Texto>
        </Tarjeta>
      ) : null}
      {rutas?.map(({ ruta }) => {
        const estado = ESTADO_RUTA[ruta.estado];
        return (
          <Pressable key={ruta.id} onPress={() => alAbrirRuta(ruta.id)} accessibilityRole="button"
            accessibilityLabel={`Ruta ${ruta.sectorNombre}, ${TURNO[ruta.turno]}, ${estado.texto}. Abrir`}>
            {({ pressed }) => (
              <Tarjeta estilo={pressed ? { opacity: 0.8 } : undefined}>
                <Texto tipo="subtitulo">{ruta.sectorNombre}</Texto>
                <Fila>
                  <Etiqueta texto={TURNO[ruta.turno]} />
                  <Etiqueta texto={estado.texto} tono={estado.tono} />
                </Fila>
                <Texto>Cisterna {ruta.cisternaPlaca ?? 'por asignar'} · {ruta.paradasAtendidas} de {ruta.totalParadas} paradas</Texto>
                <Texto tipo="pequeno">Toca para abrir la ruta</Texto>
              </Tarjeta>
            )}
          </Pressable>
        );
      })}
    </Pantalla>
  );
}
