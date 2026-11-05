-- El vecino se registra e ingresa con DNI + código SMS, sin contraseña (decisión del 4 nov 2026).
-- Administrador y conductor siguen ingresando con DNI + contraseña, así que para ellos sigue siendo obligatoria.
-- Los vecinos de prueba de V3 conservan su contraseña para las demos.
ALTER TABLE persona ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE persona ADD CONSTRAINT ck_persona_password CHECK (rol = 'VECINO' OR password_hash IS NOT NULL);
