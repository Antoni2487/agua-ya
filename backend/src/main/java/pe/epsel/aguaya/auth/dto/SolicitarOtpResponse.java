package pe.epsel.aguaya.auth.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @param codigo solo presente en desarrollo con {@code aguaya.otp.exponer-codigo=true}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SolicitarOtpResponse(String mensaje, OffsetDateTime expiraEn, String codigo) {
}
