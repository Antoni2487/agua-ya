-- Notificaciones Web Push para la web del vecino (decisión del 4 nov 2026: el vecino usa la web, no la app).
-- Cada fila es la suscripción de un navegador (PushSubscription): endpoint del servicio de push del navegador
-- y las claves con las que se cifra el mensaje (RFC 8291). dispositivo_push (Expo) queda para la app del conductor.
CREATE TABLE suscripcion_web_push (
    id              BIGSERIAL    PRIMARY KEY,
    persona_id      BIGINT       NOT NULL REFERENCES persona (id) ON DELETE CASCADE,
    endpoint        VARCHAR(1000) NOT NULL,
    p256dh          VARCHAR(100) NOT NULL,
    auth            VARCHAR(50)  NOT NULL,
    actualizado_en  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_suscripcion_web_push_endpoint UNIQUE (endpoint)
);

-- Destinatarios de cada aviso: suscripciones de una lista de personas
CREATE INDEX ix_suscripcion_web_push_persona ON suscripcion_web_push (persona_id);
