package pe.epsel.aguaya.capacidad.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import pe.epsel.aguaya.sector.entity.Modalidad;
import pe.epsel.aguaya.sector.entity.Sector;

class CalculadoraViajesTest {

    private static Sector sector(int unidadesUso, int frecuenciaDias) {
        Sector s = new Sector("Prueba", "Chiclayo", Modalidad.PERMANENTE, unidadesUso, 4,
                BigDecimal.ZERO, BigDecimal.ZERO);
        s.setFrecuenciaDias(frecuenciaDias);
        return s;
    }

    @Test
    void cerroponConCisternaDe12000Litros() {
        // 350 UU × 80 L × 1 día = 28 000 L ÷ 12 000 L = 2,33 -> 3
        assertThat(CalculadoraViajes.litrosPorVisita(sector(350, 1))).isEqualTo(28_000);
        assertThat(CalculadoraViajes.viajesNecesarios(sector(350, 1), 12_000)).isEqualTo(3);
        assertThat(CalculadoraViajes.viajesNecesarios(sector(350, 1), 15_000)).isEqualTo(2);
    }

    @Test
    void laFrecuenciaMultiplicaElAguaPorVisita() {
        // Callanca: 180 UU × 80 L × 2 días = 28 800 L ÷ 10 000 L -> 3
        assertThat(CalculadoraViajes.viajesNecesarios(sector(180, 1), 10_000)).isEqualTo(2);
        assertThat(CalculadoraViajes.viajesNecesarios(sector(180, 2), 10_000)).isEqualTo(3);
    }

    @Test
    void divisionExactaNoSumaUnViajeDeMas() {
        // 150 UU × 80 L = 12 000 L ÷ 12 000 L = 1
        assertThat(CalculadoraViajes.viajesNecesarios(sector(150, 1), 12_000)).isEqualTo(1);
    }

    @Test
    void siempreAlMenosUnViaje() {
        assertThat(CalculadoraViajes.viajesNecesarios(sector(0, 1), 12_000)).isEqualTo(1);
    }

    @Test
    void usaLaDotacionDelSector() {
        Sector s = sector(150, 1);
        s.setDotacionLitros(20); // interrupción que afecta a más del 10 % de conexiones
        assertThat(CalculadoraViajes.viajesNecesarios(s, 1_000)).isEqualTo(3);
    }

    @Test
    void capacidadInvalida() {
        assertThatThrownBy(() -> CalculadoraViajes.viajesNecesarios(sector(10, 1), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cisternasNecesariasRedondeaHaciaArriba() {
        // P.J. Urrunaga: 5 viajes con máximo 3 por cisterna al día -> 2 cisternas
        assertThat(CalculadoraViajes.cisternasNecesarias(5, 3)).isEqualTo(2);
        assertThat(CalculadoraViajes.cisternasNecesarias(4, 4)).isEqualTo(1);
        assertThatThrownBy(() -> CalculadoraViajes.cisternasNecesarias(4, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
