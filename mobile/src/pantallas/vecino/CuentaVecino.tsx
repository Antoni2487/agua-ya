// Mi cuenta: datos del vecino, su sector, sus reportes y cerrar sesión.
import type { ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import type { VecinoPerfil } from '../../api/tipos';
import { Boton, Cargando, Divisor, Pantalla, PieServicio, Tarjeta, Texto } from '../../componentes/ui';
import { color, fuente } from '../../tema';

export function CuentaVecino({ perfil, barra, alVerReportes, alSalir }: {
  perfil: VecinoPerfil | null; barra: ReactNode; alVerReportes: () => void; alSalir: () => void;
}) {
  return (
    <Pantalla titulo="Mi cuenta" barra={barra}>
      {!perfil ? <Cargando /> : (
        <Tarjeta>
          <Texto tipo="subtitulo">{perfil.nombres} {perfil.apellidos}</Texto>
          <Dato etiqueta="DNI" valor={perfil.dni} />
          <Dato etiqueta="Celular" valor={perfil.telefono.replace('+51', '')} />
          <Divisor />
          <Dato etiqueta="Sector" valor={perfil.sector ? `${perfil.sector.nombre} · ${perfil.sector.distrito}` : 'Sin sector'} />
          <Dato etiqueta="Entrega" valor={perfil.sector?.tipoEntrega === 'PUNTO_ENTREGA' ? 'En el punto de entrega más cercano' : 'En la puerta de tu casa'} />
          {perfil.direccionReferencia ? <Dato etiqueta="Referencia" valor={perfil.direccionReferencia} /> : null}
        </Tarjeta>
      )}
      <Boton texto="Mis reportes" icono="lista" variante="secundario" alPresionar={alVerReportes} />
      <Boton texto="Cerrar sesión" icono="salir" variante="secundario" alPresionar={alSalir} />
      <Texto tipo="pequeno">
        Para cambiar la ubicación de tu casa o tu celular, comunícate con EPSEL. Si olvidas tu clave, en la pantalla de
        ingreso toca «¿Olvidaste tu clave?».
      </Texto>
      <PieServicio />
    </Pantalla>
  );
}

function Dato({ etiqueta, valor }: { etiqueta: string; valor: string }) {
  return (
    <View style={estilos.dato}>
      <Text style={estilos.etiqueta}>{etiqueta}</Text>
      <Text style={estilos.valor}>{valor}</Text>
    </View>
  );
}

const estilos = StyleSheet.create({
  dato: { gap: 2 },
  etiqueta: { fontSize: 14, fontFamily: fuente.cuerpo, color: color.tintaSuave },
  valor: { fontSize: 17, fontFamily: fuente.negrita, color: color.cuerpo },
});
