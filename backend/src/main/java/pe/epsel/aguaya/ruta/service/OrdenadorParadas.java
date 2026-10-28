package pe.epsel.aguaya.ruta.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import pe.epsel.aguaya.common.util.Haversine;
import pe.epsel.aguaya.ruta.entity.Parada;
import pe.epsel.aguaya.ruta.entity.PrioridadParada;

/**
 * Orden de las paradas (regla 3): primero las CRITICA y luego las NORMAL, cada grupo como
 * recorrido encadenado: desde el punto actual se va a la parada más cercana aún no visitada.
 * Es el orden que seguiría un conductor que conoce la zona; no es optimización de rutas.
 */
public final class OrdenadorParadas {

    private OrdenadorParadas() {
    }

    /**
     * Asigna {@code orden} 1..n y devuelve las paradas ordenadas.
     *
     * @param latOrigen latitud del punto de llenado (salida de la cisterna)
     * @param lngOrigen longitud del punto de llenado
     */
    public static List<Parada> ordenar(List<Parada> paradas, double latOrigen, double lngOrigen) {
        List<Parada> criticas = paradas.stream().filter(p -> p.getPrioridad() == PrioridadParada.CRITICA).toList();
        List<Parada> normales = paradas.stream().filter(p -> p.getPrioridad() != PrioridadParada.CRITICA).toList();

        List<Parada> resultado = new ArrayList<>(paradas.size());
        double[] posicion = {latOrigen, lngOrigen};
        encadenar(criticas, posicion, resultado);
        encadenar(normales, posicion, resultado);

        for (int i = 0; i < resultado.size(); i++) {
            resultado.get(i).setOrden(i + 1);
        }
        return resultado;
    }

    /** Agrega el grupo a {@code resultado} en orden encadenado y deja {@code posicion} en la última parada. */
    private static void encadenar(List<Parada> grupo, double[] posicion, List<Parada> resultado) {
        List<Parada> pendientes = new ArrayList<>(grupo);
        while (!pendientes.isEmpty()) {
            Parada siguiente = pendientes.stream()
                    .min(Comparator.comparingDouble(p -> Haversine.distanciaMetros(posicion[0], posicion[1],
                            p.getLatitud().doubleValue(), p.getLongitud().doubleValue())))
                    .orElseThrow();
            pendientes.remove(siguiente);
            resultado.add(siguiente);
            posicion[0] = siguiente.getLatitud().doubleValue();
            posicion[1] = siguiente.getLongitud().doubleValue();
        }
    }
}
