package pe.epsel.aguaya.auth.service;

import java.time.OffsetDateTime;

/**
 * Envía y verifica el código OTP que confirma el teléfono del vecino al registrarse.
 * Se elige con {@code aguaya.otp.provider}:
 * <ul>
 *   <li>{@code log}: {@link ProveedorOtpLocal}, genera el código, lo guarda hasheado y lo muestra en el log (dev).</li>
 *   <li>{@code twilio}: {@link ProveedorOtpTwilio}, Twilio Verify genera, envía y valida el código (prod).</li>
 * </ul>
 * Los errores se lanzan como {@code NegocioException} con códigos {@code OTP_*}.
 */
public interface ProveedorOtp {

    /**
     * @param telefono en formato E.164 ({@code +519XXXXXXXX})
     */
    OtpEnviado enviar(String telefono);

    /**
     * Valida el código y lo consume. Lanza {@code NegocioException} si es incorrecto, expiró o no existe.
     */
    void verificar(String telefono, String codigo);

    /**
     * @param expiraEn cuándo vence el código, si el proveedor lo informa
     * @param codigo   el código en claro, solo en dev con {@code aguaya.otp.exponer-codigo=true}
     */
    record OtpEnviado(OffsetDateTime expiraEn, String codigo) {
    }
}
