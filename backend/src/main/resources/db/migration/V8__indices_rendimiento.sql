-- Índices de rendimiento (auditoría de claves foráneas sin índice y de filtros frecuentes).
-- Las FK hacia persona (administrador, resuelta_por) y ruta_destino no se indexan: las personas se
-- desactivan, no se borran, y no se consultan por esas columnas.

-- Mis incidencias y control de duplicados (reportado_por + ruta + tipo + estado)
CREATE INDEX ix_incidencia_reportado_ruta ON incidencia (reportado_por, ruta_id);
-- Panel de transparencia: incidencias del periodo
CREATE INDEX ix_incidencia_creado ON incidencia (creado_en);
CREATE INDEX ix_incidencia_parada ON incidencia (parada_id) WHERE parada_id IS NOT NULL;

-- parada es la tabla que más crece: sin estos índices, borrar un punto de entrega, una institución o
-- un viaje obliga a Postgres a recorrerla completa para validar la FK
CREATE INDEX ix_parada_viaje ON parada (viaje_id) WHERE viaje_id IS NOT NULL;
CREATE INDEX ix_parada_punto_entrega ON parada (punto_entrega_id) WHERE punto_entrega_id IS NOT NULL;
CREATE INDEX ix_parada_institucion ON parada (institucion_id) WHERE institucion_id IS NOT NULL;

-- Borrado o consulta de puntos de llenado
CREATE INDEX ix_ruta_punto_llenado ON ruta (punto_llenado_id);
CREATE INDEX ix_viaje_punto_llenado ON viaje (punto_llenado_id);
