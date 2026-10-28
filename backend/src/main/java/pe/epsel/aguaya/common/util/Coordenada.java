package pe.epsel.aguaya.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Coordenadas en grados con 6 decimales (~10 cm), el tamaño de las columnas {@code numeric(9,6)}.
 * El GPS del celular y los mapas envían más decimales; se redondean en vez de rechazarlos.
 */
public final class Coordenada {

    private Coordenada() {
    }

    public static BigDecimal normalizar(BigDecimal grados) {
        return grados == null ? null : grados.setScale(6, RoundingMode.HALF_UP);
    }
}
