-- RF08: en sectores PUNTO_ENTREGA varios vecinos comparten la misma parada.
-- "Una vez por parada" pasa a ser una calificación por parada y vecino.
ALTER TABLE calificacion_servicio DROP CONSTRAINT uk_calificacion_parada;
ALTER TABLE calificacion_servicio ADD CONSTRAINT uk_calificacion_parada_vecino UNIQUE (parada_id, vecino_id);
