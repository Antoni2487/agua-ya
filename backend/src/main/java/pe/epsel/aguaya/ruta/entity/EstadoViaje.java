package pe.epsel.aguaya.ruta.entity;

/**
 * EN_CARGA existe en el esquema, pero el MVP registra el viaje cuando la cisterna ya sale cargada.
 */
public enum EstadoViaje {
    EN_CARGA,
    EN_REPARTO,
    FINALIZADO
}
