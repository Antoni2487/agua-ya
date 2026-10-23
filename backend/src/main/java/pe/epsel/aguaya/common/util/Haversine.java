package pe.epsel.aguaya.common.util;

/**
 * Distancia sobre la superficie terrestre entre dos coordenadas (fórmula de Haversine).
 * Se usa para la geocerca, la sugerencia por cercanía y el sector más cercano.
 */
public final class Haversine {

    private static final double RADIO_TIERRA_METROS = 6_371_000;

    private Haversine() {
    }

    public static double distanciaMetros(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * RADIO_TIERRA_METROS * Math.asin(Math.min(1, Math.sqrt(a)));
    }
}
