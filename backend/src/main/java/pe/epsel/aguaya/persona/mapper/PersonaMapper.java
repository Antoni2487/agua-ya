package pe.epsel.aguaya.persona.mapper;

import pe.epsel.aguaya.persona.dto.PersonaResumen;
import pe.epsel.aguaya.persona.entity.Persona;

public final class PersonaMapper {

    private PersonaMapper() {
    }

    public static PersonaResumen aResumen(Persona persona) {
        return new PersonaResumen(persona.getId(), persona.getNombres(), persona.getApellidos(), persona.getRol());
    }
}
