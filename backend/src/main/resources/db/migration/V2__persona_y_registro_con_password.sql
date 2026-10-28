-- =====================================================================
-- AguaYa - V2: ajustes al modelo de personas y registro
--  1. La tabla base "usuario" pasa a llamarse "persona" (clase padre de
--     vecino, conductor y administrador).
--  2. Todas las personas ingresan con contraseña (el OTP solo verifica el
--     teléfono en el registro).
--  3. El vecino queda activo al registrarse: se elimina la validación del admin.
--  4. El OTP solo se usa en el registro: se elimina "proposito".
-- =====================================================================

-- 1. usuario -> persona
ALTER TABLE usuario RENAME TO persona;
ALTER TABLE persona RENAME CONSTRAINT usuario_pkey TO persona_pkey;
ALTER TABLE persona RENAME CONSTRAINT uk_usuario_dni TO uk_persona_dni;
ALTER TABLE persona RENAME CONSTRAINT uk_usuario_telefono TO uk_persona_telefono;
ALTER TABLE persona RENAME CONSTRAINT ck_usuario_dni TO ck_persona_dni;
ALTER TABLE persona RENAME CONSTRAINT ck_usuario_rol TO ck_persona_rol;
ALTER SEQUENCE usuario_id_seq RENAME TO persona_id_seq;

ALTER TABLE vecino RENAME COLUMN usuario_id TO persona_id;
ALTER TABLE conductor RENAME COLUMN usuario_id TO persona_id;
ALTER TABLE administrador RENAME COLUMN usuario_id TO persona_id;
ALTER TABLE refresh_token RENAME COLUMN usuario_id TO persona_id;
ALTER TABLE dispositivo_push RENAME COLUMN usuario_id TO persona_id;
ALTER INDEX ix_refresh_token_usuario RENAME TO ix_refresh_token_persona;
ALTER INDEX ix_dispositivo_usuario RENAME TO ix_dispositivo_persona;

-- 2. Contraseña obligatoria para todos los roles
ALTER TABLE persona DROP CONSTRAINT ck_usuario_password;
ALTER TABLE persona ALTER COLUMN password_hash SET NOT NULL;

-- 3. Sin validación de vecinos (el índice sobre estado cae con la columna)
ALTER TABLE vecino DROP COLUMN estado;
CREATE INDEX ix_vecino_sector ON vecino (sector_id);

-- 4. OTP solo para registro
ALTER TABLE codigo_otp DROP COLUMN proposito;
