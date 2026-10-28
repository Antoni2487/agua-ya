-- =====================================================================
-- AguaYa - V4: modalidad de entrega por sector y ajustes de rutas
--  - El sector reparte casa por casa (DOMICILIO) o en puntos de entrega
--    donde se detiene la cisterna (PUNTO_ENTREGA), p. ej. zonas altas sin acceso.
--  - La parada puede ser un punto de entrega.
--  - La ruta registra su punto de llenado (origen del recorrido).
--  - Una ruta por sector, fecha y turno; una cisterna no se asigna a dos
--    rutas en la misma fecha y turno.
-- =====================================================================

ALTER TABLE sector ADD COLUMN tipo_entrega VARCHAR(15) NOT NULL DEFAULT 'DOMICILIO';
ALTER TABLE sector ADD CONSTRAINT ck_sector_tipo_entrega CHECK (tipo_entrega IN ('DOMICILIO', 'PUNTO_ENTREGA'));

CREATE TABLE punto_entrega (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sector_id   BIGINT        NOT NULL REFERENCES sector (id),
    nombre      VARCHAR(150)  NOT NULL,
    referencia  VARCHAR(255),
    latitud     NUMERIC(9, 6) NOT NULL,
    longitud    NUMERIC(9, 6) NOT NULL,
    activo      BOOLEAN       NOT NULL DEFAULT TRUE
);
CREATE INDEX ix_punto_entrega_sector ON punto_entrega (sector_id);

ALTER TABLE parada ADD COLUMN punto_entrega_id BIGINT REFERENCES punto_entrega (id);
ALTER TABLE parada DROP CONSTRAINT ck_parada_tipo;
ALTER TABLE parada ADD CONSTRAINT ck_parada_tipo CHECK (tipo IN ('HOGAR', 'INSTITUCION', 'PUNTO_ENTREGA'));
ALTER TABLE parada DROP CONSTRAINT ck_parada_destino;
ALTER TABLE parada ADD CONSTRAINT ck_parada_destino CHECK (
    (tipo = 'HOGAR' AND vecino_id IS NOT NULL AND institucion_id IS NULL AND punto_entrega_id IS NULL)
    OR (tipo = 'INSTITUCION' AND institucion_id IS NOT NULL AND vecino_id IS NULL AND punto_entrega_id IS NULL)
    OR (tipo = 'PUNTO_ENTREGA' AND punto_entrega_id IS NOT NULL AND vecino_id IS NULL AND institucion_id IS NULL)
);

ALTER TABLE ruta ADD COLUMN punto_llenado_id BIGINT REFERENCES punto_llenado (id);
CREATE UNIQUE INDEX uk_ruta_sector_fecha_turno ON ruta (sector_id, fecha_programada, turno);
CREATE UNIQUE INDEX uk_ruta_cisterna_fecha_turno
    ON ruta (cisterna_id, fecha_programada, turno)
    WHERE cisterna_id IS NOT NULL;

-- Datos referenciales: la parte alta de Cerropón se atiende en puntos de entrega
UPDATE sector SET tipo_entrega = 'PUNTO_ENTREGA' WHERE nombre = 'Cerropón' AND distrito = 'Chiclayo';
INSERT INTO punto_entrega (sector_id, nombre, referencia, latitud, longitud)
SELECT s.id, p.nombre, p.referencia, p.lat, p.lng
FROM (VALUES
        ('Losa deportiva Cerropón (referencial)', 'Frente a la losa, parte baja', -6.781600, -79.822300),
        ('Parte alta Cerropón (referencial)',     'Último tramo transitable, Mz. F', -6.783200, -79.822100)
     ) AS p(nombre, referencia, lat, lng)
JOIN sector s ON s.nombre = 'Cerropón' AND s.distrito = 'Chiclayo';
