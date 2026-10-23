package pe.epsel.aguaya.auth.service;

/**
 * Canal por el que se entrega el código OTP al vecino.
 * Implementaciones: {@link LogOtpSender} (dev) y TwilioOtpSender (prod, pendiente).
 */
public interface OtpSender {

    /**
     * @param telefono en formato E.164 ({@code +519XXXXXXXX})
     * @param codigo   código de 6 dígitos en claro
     */
    void enviar(String telefono, String codigo);
}
