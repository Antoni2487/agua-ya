package pe.epsel.aguaya.persona.dto;

import pe.epsel.aguaya.persona.entity.Rol;

/**
 * Datos básicos de la persona autenticada.
 */
public record PersonaResumen(
        Long id,
        String nombres,
        String apellidos,
        Rol rol) {
}
