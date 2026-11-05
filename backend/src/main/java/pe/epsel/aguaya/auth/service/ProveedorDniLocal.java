package pe.epsel.aguaya.auth.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import pe.epsel.aguaya.common.exception.NegocioException;

/**
 * Proveedor de DNI para desarrollo y tests: no llama a ningún servicio. Devuelve nombres de prueba
 * derivados del DNI; {@value #DNI_INEXISTENTE} simula un DNI que no existe.
 */
@Service
@ConditionalOnProperty(name = "aguaya.dni.provider", havingValue = "local", matchIfMissing = true)
public class ProveedorDniLocal implements ProveedorDni {

    public static final String DNI_INEXISTENTE = "00000000";

    @Override
    public DatosDni consultar(String dni) {
        if (DNI_INEXISTENTE.equals(dni)) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "DNI_NO_ENCONTRADO",
                    "No encontramos ese DNI. Revisa que esté bien escrito");
        }
        return new DatosDni("Vecino", "De Prueba " + dni.substring(4));
    }
}
