-- Notificaciones push: el aviso "la cisterna está cerca" se envía una sola vez por parada.
ALTER TABLE parada ADD COLUMN aviso_cercania_en TIMESTAMPTZ;
