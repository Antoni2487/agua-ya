package pe.epsel.aguaya.ruta.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import pe.epsel.aguaya.ruta.entity.Parada;
import pe.epsel.aguaya.sector.entity.InstitucionCritica;
import pe.epsel.aguaya.sector.entity.Modalidad;
import pe.epsel.aguaya.sector.entity.PuntoEntrega;
import pe.epsel.aguaya.sector.entity.Sector;
import pe.epsel.aguaya.sector.entity.TipoInstitucion;

class OrdenadorParadasTest {

    private static final Sector SECTOR = new Sector("S", "D", Modalidad.PERMANENTE, 10, 3,
            BigDecimal.ZERO, BigDecimal.ZERO);

    /** Puntos sobre una misma latitud: la longitud indica la distancia al origen (0,0). */
    private static Parada punto(String nombre, double lng) {
        return Parada.dePuntoEntrega(new PuntoEntrega(SECTOR, nombre, BigDecimal.ZERO, BigDecimal.valueOf(lng)));
    }

    private static Parada institucion(String nombre, double lng) {
        return Parada.deInstitucion(new InstitucionCritica(nombre, TipoInstitucion.SALUD, SECTOR,
                BigDecimal.ZERO, BigDecimal.valueOf(lng)));
    }

    private static List<String> nombres(List<Parada> paradas) {
        return paradas.stream().map(p -> p.getInstitucion() != null
                ? p.getInstitucion().getNombre() : p.getPuntoEntrega().getNombre()).toList();
    }

    @Test
    void criticasPrimeroAunqueEstenMasLejos() {
        List<Parada> ordenadas = OrdenadorParadas.ordenar(
                List.of(punto("cerca", 0.001), institucion("posta lejana", 0.05)), 0, 0);

        assertThat(nombres(ordenadas)).containsExactly("posta lejana", "cerca");
        assertThat(ordenadas).extracting(Parada::getOrden).containsExactly(1, 2);
    }

    @Test
    void recorridoEncadenadoDesdeElOrigen() {
        // Origen en 0; puntos en 1, 5, 2 y -1 (en milésimas de grado)
        List<Parada> ordenadas = OrdenadorParadas.ordenar(List.of(
                punto("A", 0.001), punto("B", 0.005), punto("C", 0.002), punto("D", -0.001)), 0, 0);

        // Desde 0 lo más cercano es A(1) o D(-1); queda A (primera en la lista).
        // Desde A: C(2) está a 1, D a 2 -> C. Desde C: B(5) a 3, D a 3 -> el primero que encuentra.
        assertThat(nombres(ordenadas).subList(0, 2)).containsExactly("A", "C");
        assertThat(nombres(ordenadas)).containsExactlyInAnyOrder("A", "B", "C", "D");
        assertThat(ordenadas).extracting(Parada::getOrden).containsExactly(1, 2, 3, 4);
    }

    @Test
    void lasNormalesContinuanDesdeLaUltimaCritica() {
        // La posta está en 10; los puntos en 1 y 9: después de la posta sigue el punto 9
        List<Parada> ordenadas = OrdenadorParadas.ordenar(List.of(
                punto("p1", 0.001), punto("p9", 0.009), institucion("posta", 0.010)), 0, 0);

        assertThat(nombres(ordenadas)).containsExactly("posta", "p9", "p1");
    }

    @Test
    void listaVacia() {
        assertThat(OrdenadorParadas.ordenar(List.of(), 0, 0)).isEmpty();
    }
}
