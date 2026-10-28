package pe.epsel.aguaya.capacidad.service;

import pe.epsel.aguaya.sector.entity.Sector;

/**
 * Viajes que necesita una cisterna para atender un sector en una visita (fórmula SUNASS).
 * <pre>
 * viajes    = ⌈ unidades_uso × dotacion_litros × frecuencia_dias ÷ capacidad_litros ⌉
 * cisternas = ⌈ viajes ÷ max_viajes_diarios ⌉
 * </pre>
 * La frecuencia multiplica porque cada visita debe cubrir los días hasta la siguiente.
 */
public final class CalculadoraViajes {

    private CalculadoraViajes() {
    }

    /** Litros que necesita el sector en cada visita. */
    public static long litrosPorVisita(Sector sector) {
        return (long) sector.getUnidadesUso() * sector.getDotacionLitros() * sector.getFrecuenciaDias();
    }

    /** Mínimo 1: una ruta programada siempre hace al menos un viaje. */
    public static int viajesNecesarios(Sector sector, int capacidadLitros) {
        if (capacidadLitros <= 0) {
            throw new IllegalArgumentException("La capacidad de la cisterna debe ser positiva");
        }
        long litros = litrosPorVisita(sector);
        long viajes = (litros + capacidadLitros - 1) / capacidadLitros;
        return (int) Math.max(1, viajes);
    }

    /** Cisternas para hacer esos viajes sin pasar el máximo diario del sector. */
    public static int cisternasNecesarias(int viajesNecesarios, int maxViajesDiarios) {
        if (maxViajesDiarios <= 0) {
            throw new IllegalArgumentException("El máximo de viajes diarios debe ser positivo");
        }
        return (viajesNecesarios + maxViajesDiarios - 1) / maxViajesDiarios;
    }
}
