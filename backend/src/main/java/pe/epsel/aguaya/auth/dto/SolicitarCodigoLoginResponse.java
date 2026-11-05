package pe.epsel.aguaya.auth.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @param telefonoEnmascarado solo los 3 últimos dígitos ("*** *** 789"), para que el vecino reconozca su número
 * @param codigo              solo presente en desarrollo con {@code aguaya.otp.exponer-codigo=true}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SolicitarCodigoLoginResponse(String mensaje, String telefonoEnmascarado, OffsetDateTime expiraEn,
                                           String codigo) {
}
