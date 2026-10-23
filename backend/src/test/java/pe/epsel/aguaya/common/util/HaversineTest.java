package pe.epsel.aguaya.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class HaversineTest {

    @Test
    void mismoPuntoEsCero() {
        assertThat(Haversine.distanciaMetros(-6.7714, -79.8409, -6.7714, -79.8409)).isZero();
    }

    @Test
    void chiclayoAMonsefuAproximadamente11Km() {
        // Plaza de Armas de Chiclayo -> Plaza de Armas de Monsefú (aprox.)
        double d = Haversine.distanciaMetros(-6.7714, -79.8409, -6.8775, -79.8722);
        assertThat(d).isCloseTo(12_300, within(500.0));
    }

    @Test
    void cincuentaMetrosSonDetectables() {
        // ~0.00045° de latitud equivalen a ~50 m
        double d = Haversine.distanciaMetros(-6.7714, -79.8409, -6.77185, -79.8409);
        assertThat(d).isCloseTo(50, within(1.0));
    }
}
