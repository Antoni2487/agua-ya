-- =====================================================================
-- AguaYa - V5: registro de intentos de atención fuera de la geocerca (regla 4)
-- Evidencia para el administrador ante desvíos de cisternas: cada vez que un
-- conductor intenta marcar una parada como atendida lejos de ella, queda aquí.
-- =====================================================================

CREATE TABLE intento_geocerca (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    parada_id         BIGINT        NOT NULL REFERENCES parada (id) ON DELETE CASCADE,
    conductor_id      BIGINT        NOT NULL REFERENCES conductor (persona_id),
    latitud           NUMERIC(9, 6),
    longitud          NUMERIC(9, 6),
    distancia_metros  NUMERIC(10, 1),
    registrado_en     TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX ix_intento_geocerca_parada ON intento_geocerca (parada_id);

-- monitoreo_gps se borra junto con su ruta (solo se eliminan rutas que no iniciaron)
ALTER TABLE monitoreo_gps DROP CONSTRAINT monitoreo_gps_ruta_id_fkey;
ALTER TABLE monitoreo_gps ADD CONSTRAINT monitoreo_gps_ruta_id_fkey
    FOREIGN KEY (ruta_id) REFERENCES ruta (id) ON DELETE CASCADE;
