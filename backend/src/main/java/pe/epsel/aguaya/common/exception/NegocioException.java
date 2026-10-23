package pe.epsel.aguaya.common.exception;

import lombok.Getter;

import org.springframework.http.HttpStatus;

/**
 * Error de regla de negocio. Los servicios la lanzan con el status HTTP y un
 * código estable (p. ej. {@code CISTERNA_AUTORIZACION_VENCIDA}) que el front puede usar.
 */
@Getter
public class NegocioException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public NegocioException(HttpStatus status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }

    public static NegocioException noEncontrado(String recurso, Object id) {
        return new NegocioException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", recurso + " " + id + " no existe");
    }

    public static NegocioException conflicto(String codigo, String mensaje) {
        return new NegocioException(HttpStatus.CONFLICT, codigo, mensaje);
    }
}
