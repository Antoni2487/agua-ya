package pe.epsel.aguaya.common.health;

import java.time.Clock;
import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health")
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public record HealthResponse(String estado, String baseDatos, OffsetDateTime timestamp) {
    }

    @Operation(summary = "Estado del backend y de la conexión a la base de datos")
    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        boolean bdArriba = baseDatosResponde();
        HealthResponse body = new HealthResponse(
                bdArriba ? "UP" : "DOWN",
                bdArriba ? "UP" : "DOWN",
                OffsetDateTime.now(clock));
        return ResponseEntity.status(bdArriba ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    private boolean baseDatosResponde() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
