// Inicio del vecino (Figma 01 "Cisterna en camino" y 02 "Atención programada"): cuándo le toca, cuántas paradas
// faltan antes de la suya, la placa de la cisterna y su última entrega. Si no hay visita programada, lo dice.
import type { ReactNode } from 'react';
import { RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import type { Atencion } from '../../api/tipos';
import { Icono } from '../../componentes/iconos';
import { Aviso, Boton, Cargando, Divisor, Etiqueta, Fila, PieServicio, Tarjeta, Texto } from '../../componentes/ui';
import { CabeceraVecino, GotaAvance, Muestra, PlacaCisterna, TiraParadas } from '../../componentes/vecino';
import { color, fuente, medida } from '../../tema';
import { cuando, fechaCorta, teToca, TURNO_CORTO, TURNO_FRASE } from '../../textos';
import type { DatosVecino } from './VecinoApp';

export function InicioVecino({ datos, barra, alVerMapa, alReportar, alCalificar }: {
  datos: DatosVecino; barra: ReactNode; alVerMapa: () => void; alReportar: () => void; alCalificar: (a: Atencion) => void;
}) {
  const borde = useSafeAreaInsets();
  const { perfil, atencion, error, cargando, recargar } = datos;
  const proxima = atencion?.proxima ?? null;
  const ultima = atencion?.ultima ?? null;
  const nombre = perfil?.nombres.split(' ')[0];
  const lugar = perfil ? [perfil.sector?.nombre, perfil.sector?.distrito, perfil.direccionReferencia].filter(Boolean).join(' · ') : '';

  return (
    <View style={[estilos.pantalla, { paddingTop: borde.top }]}>
      <CabeceraVecino />
      <ScrollView contentContainerStyle={estilos.contenido}
        refreshControl={<RefreshControl refreshing={false} onRefresh={() => void recargar()} colors={[color.acento]} />}>
        <View style={{ gap: 4 }}>
          <Texto tipo="titulo">{nombre ? `Hola, ${nombre}` : 'Hola'}</Texto>
          {lugar ? (
            <Fila estilo={{ gap: 6, flexWrap: 'nowrap' }}>
              <Icono nombre="ubicacion" tamano={18} color={color.tintaSuave} />
              <Texto tipo="pequeno" estilo={{ flex: 1 }}>{lugar}</Texto>
            </Fila>
          ) : null}
        </View>

        {cargando && !atencion ? <Cargando texto="Buscando tu próxima cisterna…" /> : null}
        <Aviso texto={error} />

        {atencion && !proxima ? <SinVisita sector={perfil?.sector?.nombre} /> : null}
        {proxima ? <TarjetaProxima atencion={proxima} punto={perfil?.sector?.tipoEntrega === 'PUNTO_ENTREGA'} /> : null}

        {proxima ? <Boton texto="Ver la cisterna en el mapa" icono="mapa" alPresionar={alVerMapa} /> : null}
        {proxima || ultima ? <Boton texto="Reportar un problema" icono="reportar" variante="secundario" alPresionar={alReportar} /> : null}

        {ultima ? <TarjetaUltima atencion={ultima} alCalificar={() => alCalificar(ultima)} /> : null}
        <PieServicio />
      </ScrollView>
      {barra}
    </View>
  );
}

function SinVisita({ sector }: { sector?: string }) {
  return (
    <Tarjeta>
      <Etiqueta texto="Sin programar" tono="neutro" icono="calendario" />
      <Texto tipo="grande">Aún no hay visita programada</Texto>
      <Texto>
        EPSEL todavía no programa la cisterna para {sector ?? 'tu sector'}. Cuando lo haga, aparecerá aquí con el día, el
        turno y la placa del camión, y te llegará un aviso.
      </Texto>
      <Divisor />
      <Texto tipo="pequeno">Mientras tanto, guarda agua en recipientes limpios y tapados.</Texto>
    </Tarjeta>
  );
}

function TarjetaProxima({ atencion, punto }: { atencion: Atencion; punto: boolean }) {
  const mi = atencion.miParada;
  const enCurso = atencion.estado === 'EN_CURSO';
  const lugar = punto ? 'Tu punto de entrega' : 'Tu casa';
  const avance = atencion.totalParadas ? atencion.paradasAtendidas / atencion.totalParadas : 0;

  let titular: string;
  let detalle: string;
  if (!mi) {
    titular = enCurso ? 'La cisterna está repartiendo' : teToca(atencion.fecha);
    detalle = 'Tu casa se registró después de programar esta ruta: entra en la siguiente visita a tu sector.';
  } else if (mi.estado === 'ATENDIDA') {
    titular = 'Ya te atendieron';
    detalle = 'La cisterna ya pasó por tu parada en esta ruta.';
  } else if (mi.estado !== 'PENDIENTE') {
    titular = mi.estado === 'REASIGNADA' ? 'Tu parada pasó a otra cisterna' : 'No se pudo atender tu parada';
    detalle = mi.estado === 'REASIGNADA' ? 'EPSEL la reprogramó; te avisaremos cuándo llega.' : 'Puedes reportarlo para que EPSEL lo revise.';
  } else if (enCurso) {
    titular = mi.pendientesAntes === 0 ? '¡Eres la siguiente parada!' : `Faltan ${mi.pendientesAntes} ${mi.pendientesAntes === 1 ? 'parada' : 'paradas'}`;
    detalle = mi.pendientesAntes === 0 ? 'Ten listos tus baldes o tu tanque.' : 'antes de la tuya. Ten listos tus baldes o tu tanque.';
  } else {
    titular = teToca(atencion.fecha);
    detalle = `${TURNO_FRASE[atencion.turno]}. Serás la parada ${mi.orden} de ${atencion.totalParadas}. Te avisaremos cuando la cisterna salga.`;
  }

  return (
    <Tarjeta>
      <Fila>
        {enCurso ? <Etiqueta texto="En camino" tono="marino" icono="camion" /> : <Etiqueta texto="Programada" tono="info" icono="calendario" />}
        <Text style={estilos.cuando}>{fechaCorta(atencion.fecha)} · {TURNO_CORTO[atencion.turno]}</Text>
      </Fila>
      <View style={estilos.titular}>
        <View style={{ flex: 1, gap: 6 }}>
          <Text style={estilos.grande} accessibilityRole="header">{titular}</Text>
          <Texto>{detalle}</Texto>
        </View>
        <GotaAvance avance={avance} />
      </View>
      {mi && atencion.totalParadas > 0 ? (
        <>
          <TiraParadas total={atencion.totalParadas} orden={mi.orden} pendientesAntes={mi.pendientesAntes} enCurso={enCurso} />
          <View style={{ gap: 6 }} accessible accessibilityLabel={`${atencion.paradasAtendidas} paradas atendidas de ${atencion.totalParadas}. ${lugar}: parada ${mi.orden}.`}>
            <Fila estilo={{ gap: 16 }}>
              {enCurso ? <Leyenda muestra="atendida" texto={`${atencion.paradasAtendidas} atendidas`} /> : <Leyenda muestra="despues" texto={`${atencion.totalParadas} paradas programadas`} />}
              {enCurso && mi.estado === 'PENDIENTE' && mi.pendientesAntes > 0 ? <Leyenda muestra="antes" texto={`${mi.pendientesAntes} antes de ti`} /> : null}
            </Fila>
            <Leyenda muestra="tuya" texto={`${lugar}: parada ${mi.orden} de ${atencion.totalParadas}`} naranja />
          </View>
        </>
      ) : null}
      {atencion.cisternaPlaca ? (
        <>
          <Divisor />
          <PlacaCisterna placa={atencion.cisternaPlaca} capacidad={atencion.capacidadLitros ?? null} />
          <Texto tipo="pequeno">Reconócela por su placa</Texto>
        </>
      ) : null}
    </Tarjeta>
  );
}

function Leyenda({ muestra, texto, naranja }: { muestra: 'atendida' | 'antes' | 'tuya' | 'despues'; texto: string; naranja?: boolean }) {
  return (
    <Fila estilo={{ gap: 6 }}>
      <Muestra tipo={muestra} />
      <Text style={[estilos.leyenda, naranja && { color: color.tuCasa }]}>{texto}</Text>
    </Fila>
  );
}

function TarjetaUltima({ atencion, alCalificar }: { atencion: Atencion; alCalificar: () => void }) {
  const mi = atencion.miParada;
  const estado = mi?.estado === 'ATENDIDA' ? { texto: 'Atendida', tono: 'bien' as const, icono: 'check' as const }
    : mi?.estado === 'NO_ATENDIDA' ? { texto: 'No atendida', tono: 'error' as const, icono: 'alerta' as const }
    : mi?.estado === 'REASIGNADA' ? { texto: 'Reprogramada', tono: 'neutro' as const, icono: 'calendario' as const }
    : { texto: atencion.estado === 'INTERRUMPIDA' ? 'Interrumpida' : 'Completada', tono: 'neutro' as const, icono: 'check' as const };
  return (
    <Tarjeta>
      <Fila estilo={{ justifyContent: 'space-between' }}>
        <Texto tipo="subtitulo">Última entrega</Texto>
        <Etiqueta texto={estado.texto} tono={estado.tono} icono={estado.icono} />
      </Fila>
      <Texto>{cuando(atencion.fecha)} · {TURNO_CORTO[atencion.turno]}{atencion.cisternaPlaca ? ` · cisterna ${atencion.cisternaPlaca}` : ''}</Texto>
      {mi?.verificadaGps ? (
        <Fila estilo={{ gap: 6, flexWrap: 'nowrap', alignItems: 'flex-start' }}>
          <Icono nombre="escudo" tamano={20} color={color.bien} />
          <Text style={[estilos.leyenda, { color: color.bien, flex: 1 }]}>Verificada por GPS: la cisterna llegó a menos de 50 m de tu parada</Text>
        </Fila>
      ) : null}
      {mi?.miCalificacion != null ? (
        <Fila estilo={{ gap: 4 }} >
          <Text style={estilos.leyenda} accessibilityLabel={`Tu calificación: ${mi.miCalificacion} de 5 estrellas`}>Tu calificación:</Text>
          {[1, 2, 3, 4, 5].map((n) => <Icono key={n} nombre={n <= mi.miCalificacion! ? 'estrella' : 'estrellaVacia'} tamano={20} color={color.acento} />)}
        </Fila>
      ) : mi?.estado === 'ATENDIDA' ? (
        <Boton texto="Calificar la entrega" icono="estrellaVacia" variante="secundario" alPresionar={alCalificar} />
      ) : null}
    </Tarjeta>
  );
}

const estilos = StyleSheet.create({
  pantalla: { flex: 1, backgroundColor: color.fondo },
  contenido: { padding: medida.margen, paddingTop: 4, gap: 16, paddingBottom: 24 },
  cuando: { fontSize: 15, fontFamily: fuente.cuerpo, color: color.tintaSuave },
  titular: { flexDirection: 'row', gap: 12, alignItems: 'center' },
  grande: { fontSize: 34, lineHeight: 40, fontFamily: fuente.titular, color: color.tinta, letterSpacing: -0.5 },
  leyenda: { fontSize: 15, fontFamily: fuente.cuerpo, color: color.cuerpo },
});
