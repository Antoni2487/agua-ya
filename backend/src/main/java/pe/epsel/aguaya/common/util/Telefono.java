package pe.epsel.aguaya.common.util;

/**
 * Teléfonos móviles peruanos. Se guardan en E.164 ({@code +519XXXXXXXX}).
 */
public final class Telefono {

    /** Acepta {@code 9XXXXXXXX} o {@code +519XXXXXXXX}. */
    public static final String PATRON = "^(\\+51)?9\\d{8}$";

    private Telefono() {
    }

    public static String normalizar(String telefono) {
        return telefono.startsWith("+51") ? telefono : "+51" + telefono;
    }
}
