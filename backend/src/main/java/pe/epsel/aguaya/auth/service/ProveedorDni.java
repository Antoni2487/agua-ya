package pe.epsel.aguaya.auth.service;

/**
 * Obtiene nombres y apellidos a partir del DNI, para que el vecino no tenga que escribirlos al registrarse.
 * Se elige con {@code aguaya.dni.provider}:
 * <ul>
 *   <li>{@code local}: {@link ProveedorDniLocal}, nombres de prueba sin llamar a nadie (dev y tests).</li>
 *   <li>{@code miapi}: {@link ProveedorDniMiApi}, consulta miapi.cloud (prod).</li>
 * </ul>
 * Solo se guardan nombres y apellidos; la dirección, el género y la fecha de nacimiento que devuelva el
 * proveedor se descartan (minimización de datos personales, Ley 29733).
 */
public interface ProveedorDni {

    /**
     * Lanza {@code NegocioException} {@code DNI_NO_ENCONTRADO} si el DNI no existe o
     * {@code DNI_PROVEEDOR_ERROR} si el servicio no responde.
     */
    DatosDni consultar(String dni);

    /**
     * @param nombres   con mayúscula inicial (p. ej. "Norbi Antoni")
     * @param apellidos paterno y materno separados por espacio
     */
    record DatosDni(String nombres, String apellidos) {
    }
}
