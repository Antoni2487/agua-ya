-- =====================================================================
-- AguaYa - V3: DATOS REFERENCIALES (no oficiales de EPSEL)
-- Sectores, instituciones, punto de llenado, cisternas y personas de prueba
-- para desarrollo y demostración del curso SIE.
--  - Coordenadas aproximadas (OpenStreetMap) y unidades de uso estimadas.
--  - Placas, autorizaciones sanitarias, DNI y teléfonos son FICTICIOS.
--  - Contraseñas de prueba (documentadas en backend/README.md):
--      admin: Admin2026!   conductores: Conductor2026!   vecinos: Vecino2026!
-- =====================================================================

-- ---------------------------------------------------------------------
-- Sectores (dotación por defecto: 80 L por unidad de uso)
-- ---------------------------------------------------------------------
INSERT INTO sector (nombre, distrito, modalidad, es_zona_alta, unidades_uso, frecuencia_dias,
                    max_viajes_diarios, lat_centro, lng_centro)
VALUES ('Cerropón',        'Chiclayo',            'PERMANENTE',   TRUE,  350, 1, 4, -6.782000, -79.822000),
       ('P.J. Atusparias', 'José Leonardo Ortiz', 'PERMANENTE',   FALSE, 420, 1, 4, -6.748000, -79.838000),
       ('P.J. Urrunaga',   'José Leonardo Ortiz', 'CONTINGENCIA', FALSE, 280, 2, 3, -6.758000, -79.852000),
       ('Callanca',        'Monsefú',             'PERMANENTE',   FALSE, 180, 2, 3, -6.858000, -79.852000),
       ('Valle Hermoso',   'Monsefú',             'CONTINGENCIA', TRUE,  150, 2, 3, -6.893000, -79.885000);

-- ---------------------------------------------------------------------
-- Instituciones críticas (1-2 por sector)
-- ---------------------------------------------------------------------
INSERT INTO institucion_critica (nombre, tipo, sector_id, latitud, longitud)
SELECT i.nombre, i.tipo, s.id, i.lat, i.lng
FROM (VALUES
        ('Puesto de Salud Cerropón (referencial)',  'SALUD',     'Cerropón',        -6.781200, -79.823100),
        ('I.E. Cerropón (referencial)',             'EDUCACION', 'Cerropón',        -6.783100, -79.821000),
        ('Centro de Salud Atusparias (referencial)', 'SALUD',    'P.J. Atusparias', -6.747200, -79.839000),
        ('I.E. Atusparias (referencial)',           'EDUCACION', 'P.J. Atusparias', -6.749100, -79.837200),
        ('Puesto de Salud Urrunaga (referencial)',  'SALUD',     'P.J. Urrunaga',   -6.757300, -79.851100),
        ('Puesto de Salud Callanca (referencial)',  'SALUD',     'Callanca',        -6.857400, -79.852900),
        ('I.E. Callanca (referencial)',             'EDUCACION', 'Callanca',        -6.858900, -79.851300),
        ('I.E. Valle Hermoso (referencial)',        'EDUCACION', 'Valle Hermoso',   -6.892300, -79.884100)
     ) AS i(nombre, tipo, sector, lat, lng)
JOIN sector s ON s.nombre = i.sector;

-- ---------------------------------------------------------------------
-- Punto de llenado
-- ---------------------------------------------------------------------
INSERT INTO punto_llenado (nombre, tipo, direccion, latitud, longitud)
VALUES ('Planta de Tratamiento de Agua Potable EPSEL (referencial)', 'PLANTA',
        'Chiclayo (ubicación aproximada)', -6.765000, -79.813000);

-- ---------------------------------------------------------------------
-- Cisternas (placas ficticias AYA-xxx)
--  AYA-104: autorización sanitaria vencida (regla 2)
--  AYA-105: en mantenimiento
-- ---------------------------------------------------------------------
INSERT INTO cisterna (placa, capacidad_litros, estado_vehiculo, nro_autorizacion_sanitaria, vence_autorizacion)
VALUES ('AYA-101', 12000, 'OPERATIVA',     'REF-DIRESA-LAM-0101', DATE '2027-06-30'),
       ('AYA-102', 10000, 'OPERATIVA',     'REF-DIRESA-LAM-0102', DATE '2027-03-31'),
       ('AYA-103', 15000, 'OPERATIVA',     'REF-DIRESA-LAM-0103', DATE '2027-01-31'),
       ('AYA-104',  9000, 'OPERATIVA',     'REF-DIRESA-LAM-0104', DATE '2025-12-31'),
       ('AYA-105', 12000, 'MANTENIMIENTO', 'REF-DIRESA-LAM-0105', DATE '2027-02-28');

-- ---------------------------------------------------------------------
-- Personas de prueba
-- ---------------------------------------------------------------------
-- Administrador (Admin2026!)
INSERT INTO persona (dni, nombres, apellidos, telefono, email, password_hash, rol)
VALUES ('10000001', 'Admin', 'EPSEL (referencial)', '+51900000001', 'admin@aguaya.test',
        '$2a$10$8joukptcNnc2jEzIvJ2xJOkFuqCXTgMtU.7XLyZJIluFlLaCyrUCi', 'ADMIN');
INSERT INTO administrador (persona_id, cargo)
SELECT id, 'Jefe de Operaciones' FROM persona WHERE dni = '10000001';

-- Conductores (Conductor2026!)
INSERT INTO persona (dni, nombres, apellidos, telefono, password_hash, rol)
VALUES ('20000001', 'Carlos', 'Quispe Vílchez', '+51900000011',
        '$2a$10$tgo4oRdMYolju/ezdnx2zuSGOYj/Uh3p4j6FsvQmGG8g4O1bhWE5C', 'CONDUCTOR'),
       ('20000002', 'José',   'Santisteban Díaz', '+51900000012',
        '$2a$10$tgo4oRdMYolju/ezdnx2zuSGOYj/Uh3p4j6FsvQmGG8g4O1bhWE5C', 'CONDUCTOR'),
       ('20000003', 'Miguel', 'Chapoñán Ríos', '+51900000013',
        '$2a$10$tgo4oRdMYolju/ezdnx2zuSGOYj/Uh3p4j6FsvQmGG8g4O1bhWE5C', 'CONDUCTOR');
INSERT INTO conductor (persona_id, licencia, turno)
SELECT p.id, 'Q' || p.dni, c.turno
FROM (VALUES ('20000001', 'MANANA'), ('20000002', 'MANANA'), ('20000003', 'TARDE')) AS c(dni, turno)
JOIN persona p ON p.dni = c.dni;

-- Vecinos (Vecino2026!), cada uno cerca del centro de su sector
INSERT INTO persona (dni, nombres, apellidos, telefono, password_hash, rol)
SELECT v.dni, v.nombres, v.apellidos, v.telefono,
       '$2a$10$GgsOBjKNJgOQGlzmiPM1puMHV95xdstrB.Tie9NvqUf9KWsE/jXnW', 'VECINO'
FROM (VALUES
        ('30000001', 'Rosa',     'Ñiquén Puicón',     '+51900000101'),
        ('30000002', 'Juan',     'Llontop Chafloque', '+51900000102'),
        ('30000003', 'Marleni',  'Damián Effio',      '+51900000103'),
        ('30000004', 'Pedro',    'Custodio Sánchez',  '+51900000104'),
        ('30000005', 'Lucía',    'Mío Cajusol',       '+51900000105'),
        ('30000006', 'Víctor',   'Senmache Ayasta',   '+51900000106'),
        ('30000007', 'Elena',    'Tullume Bances',    '+51900000107'),
        ('30000008', 'Wilmer',   'Capuñay Sandoval',  '+51900000108'),
        ('30000009', 'Gladys',   'Inoñán Fiestas',    '+51900000109'),
        ('30000010', 'Segundo',  'Purisaca Llauce',   '+51900000110'),
        ('30000011', 'Teresa',   'Chumioque Gonzáles','+51900000111'),
        ('30000012', 'Felipe',   'Valdera Neciosup',  '+51900000112')
     ) AS v(dni, nombres, apellidos, telefono);

INSERT INTO vecino (persona_id, direccion_referencia, latitud, longitud, sector_id)
SELECT p.id, v.direccion, v.lat, v.lng, s.id
FROM (VALUES
        ('30000001', 'Mz. A Lt. 4, Cerropón',            -6.781400, -79.822700, 'Cerropón'),
        ('30000002', 'Mz. C Lt. 12, Cerropón',           -6.782800, -79.821300, 'Cerropón'),
        ('30000003', 'Mz. F Lt. 1, parte alta',          -6.783500, -79.822400, 'Cerropón'),
        ('30000004', 'Calle Los Andes 245',              -6.747500, -79.838900, 'P.J. Atusparias'),
        ('30000005', 'Mz. 14 Lt. 7',                     -6.748900, -79.837100, 'P.J. Atusparias'),
        ('30000006', 'Av. Chiclayo cdra. 30',            -6.747100, -79.837500, 'P.J. Atusparias'),
        ('30000007', 'Mz. B Lt. 9',                      -6.757600, -79.852800, 'P.J. Urrunaga'),
        ('30000008', 'Calle Las Palmeras 110',           -6.758700, -79.851400, 'P.J. Urrunaga'),
        ('30000009', 'Caserío Callanca, frente a la losa', -6.857500, -79.852500, 'Callanca'),
        ('30000010', 'Callanca, a 2 cuadras de la iglesia', -6.858600, -79.851600, 'Callanca'),
        ('30000011', 'Valle Hermoso, Mz. A Lt. 3',       -6.892500, -79.885600, 'Valle Hermoso'),
        ('30000012', 'Valle Hermoso, parte alta',        -6.893700, -79.884500, 'Valle Hermoso')
     ) AS v(dni, direccion, lat, lng, sector)
JOIN persona p ON p.dni = v.dni
JOIN sector s ON s.nombre = v.sector;
