package pe.epsel.aguaya.common.exception;

import java.util.List;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

import org.hibernate.query.sqm.UnknownPathException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import pe.epsel.aguaya.common.exception.ErrorResponse.DetalleError;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorResponse> negocio(NegocioException e) {
        return responder(e.getStatus(), e.getCodigo(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException e) {
        List<DetalleError> detalles = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new DetalleError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "VALIDACION", "Datos de entrada inválidos", detalles);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> validacion(ConstraintViolationException e) {
        List<DetalleError> detalles = e.getConstraintViolations().stream()
                .map(v -> new DetalleError(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "VALIDACION", "Datos de entrada inválidos", detalles);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> solicitudInvalida(Exception e) {
        return responder(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA", "La solicitud no tiene un formato válido");
    }

    /** {@code ?sort=} con un campo inexistente (consultas derivadas). */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> ordenInvalido(PropertyReferenceException e) {
        return ordenInvalido();
    }

    /** {@code ?sort=} con un campo inexistente en consultas {@code @Query}; cualquier otro caso es un 500. */
    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ErrorResponse> usoInvalidoDeDatos(InvalidDataAccessApiUsageException e) {
        for (Throwable causa = e; causa != null; causa = causa.getCause()) {
            if (causa instanceof UnknownPathException) {
                return ordenInvalido();
            }
        }
        return inesperado(e);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(NoResourceFoundException e) {
        return responder(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Recurso no encontrado");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodoNoPermitido(HttpRequestMethodNotSupportedException e) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NO_PERMITIDO", "Método HTTP no permitido");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> tipoNoSoportado(HttpMediaTypeNotSupportedException e) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "TIPO_NO_SOPORTADO", "Tipo de contenido no soportado");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accesoDenegado(AccessDeniedException e) {
        return responder(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", "No tiene permisos para este recurso");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException e) {
        log.warn("Violación de integridad: {}", e.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, "CONFLICTO_DATOS", "La operación entra en conflicto con datos existentes");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception e) {
        log.error("Error no controlado", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Ocurrió un error inesperado");
    }

    private static ResponseEntity<ErrorResponse> ordenInvalido() {
        return responder(HttpStatus.BAD_REQUEST, "ORDEN_INVALIDO", "El campo de ordenamiento (sort) no existe");
    }

    private static ResponseEntity<ErrorResponse> responder(HttpStatus status, String codigo, String mensaje) {
        return responder(status, codigo, mensaje, List.of());
    }

    private static ResponseEntity<ErrorResponse> responder(HttpStatus status, String codigo, String mensaje,
                                                           List<DetalleError> detalles) {
        return ResponseEntity.status(status).body(ErrorResponse.de(status, codigo, mensaje, detalles));
    }
}
