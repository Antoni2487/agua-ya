-- El vecino ingresa con DNI + una clave de 4 dígitos (como Yape o Plin), decisión del 6 nov 2026.
-- El código SMS queda solo para registrarse y para restablecer la clave si la olvida (ahorra envíos).
-- La clave se guarda con BCrypt en password_hash, igual que la contraseña del admin y del conductor.

-- Con 4 dígitos hay solo 10 000 claves posibles: tras 5 intentos fallidos la cuenta se bloquea un rato.
ALTER TABLE persona ADD COLUMN intentos_fallidos SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE persona ADD COLUMN bloqueado_hasta TIMESTAMPTZ;

-- Datos referenciales: los vecinos de prueba de V3 pasan a la clave 1234 (antes Vecino2026!)
UPDATE persona
SET password_hash = '$2a$10$XV5EUl2wqzsC6E5tg9pKgOtj4tT2TpURZBaIPcLcJRBgSwUQnhxQC'
WHERE rol = 'VECINO' AND dni LIKE '300000%';
