package pe.epsel.aguaya.common.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Respuesta paginada estándar de la API.
 *
 * @param page número de página (empieza en 0)
 */
public record PaginaResponse<T>(
        List<T> contenido,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <E, T> PaginaResponse<T> de(Page<E> pagina, Function<E, T> mapper) {
        return new PaginaResponse<>(
                pagina.getContent().stream().map(mapper).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
