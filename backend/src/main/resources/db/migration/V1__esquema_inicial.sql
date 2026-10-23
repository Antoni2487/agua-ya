-- =====================================================================
-- AguaYa - V1: esquema inicial (modelo completo, CLAUDE.md sección 7)
-- Enums persistidos como texto con CHECK. Coordenadas numeric(9,6).
-- Fechas en timestamptz (zona de negocio: America/Lima).
-- =====================================================================

-- ---------------------------------------------------------------------
-- Usuarios (herencia JOINED: usuario + tabla por subtipo)
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dni            CHAR(8)      NOT NULL,
    nombres        VARCHAR(100) NOT NULL,
    apellidos      VARCHAR(100) NOT NULL,
    telefono       VARCHAR(15)  NOT NULL,
    email          VARCHAR(150),
    password_hash  VARCHAR(100),
    rol            VARCHAR(20)  NOT NULL,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_usuario_dni UNIQUE (dni),
    CONSTRAINT uk_usuario_telefono UNIQUE (telefono),
    CONSTRAINT ck_usuario_dni CHECK (dni ~ '^[0-9]{8}$'),
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('ADMIN', 'CONDUCTOR', 'VECINO')),
    -- Admin y conductor ingresan con contraseña; el vecino solo con OTP
    CONSTRAINT ck_usuario_password CHECK (rol = 'VECINO' OR password_hash IS NOT NULL)
);

CREATE TABLE sector (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre              VARCHAR(100)  NOT NULL,
    distrito            VARCHAR(60)   NOT NULL,
    modalidad           VARCHAR(15)   NOT NULL,
    es_zona_alta        BOOLEAN       NOT NULL DEFAULT FALSE,
    unidades_uso        INTEGER       NOT NULL,
    dotacion_litros     INTEGER       NOT NULL DEFAULT 80,
    frecuencia_dias     INTEGER       NOT NULL DEFAULT 1,
    max_viajes_diarios  INTEGER       NOT NULL,
    lat_centro          NUMERIC(9, 6) NOT NULL,
    lng_centro          NUMERIC(9, 6) NOT NULL,
    activo              BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_sector_nombre_distrito UNIQUE (nombre, distrito),
    CONSTRAINT ck_sector_modalidad CHECK (modalidad IN ('PERMANENTE', 'CONTINGENCIA')),
    CONSTRAINT ck_sector_unidades_uso CHECK (unidades_uso >= 0),
    CONSTRAINT ck_sector_dotacion CHECK (dotacion_litros > 0),
    CONSTRAINT ck_sector_frecuencia CHECK (frecuencia_dias > 0),
    CONSTRAINT ck_sector_max_viajes CHECK (max_viajes_diarios > 0)
);

CREATE TABLE vecino (
    usuario_id            BIGINT PRIMARY KEY REFERENCES usuario (id),
    direccion_referencia  VARCHAR(255),
    latitud               NUMERIC(9, 6) NOT NULL,
    longitud              NUMERIC(9, 6) NOT NULL,
    sector_id             BIGINT        NOT NULL REFERENCES sector (id),
    estado                VARCHAR(25)   NOT NULL DEFAULT 'PENDIENTE_VALIDACION',
    CONSTRAINT ck_vecino_estado CHECK (estado IN ('PENDIENTE_VALIDACION', 'ACTIVO', 'RECHAZADO'))
);
CREATE INDEX ix_vecino_sector_estado ON vecino (sector_id, estado);

CREATE TABLE conductor (
    usuario_id                BIGINT PRIMARY KEY REFERENCES usuario (id),
    licencia                  VARCHAR(20)   NOT NULL,
    estado_disponibilidad     VARCHAR(15)   NOT NULL DEFAULT 'DISPONIBLE',
    turno                     VARCHAR(10),
    lat_actual                NUMERIC(9, 6),
    lng_actual                NUMERIC(9, 6),
    ubicacion_actualizada_en  TIMESTAMPTZ,
    CONSTRAINT uk_conductor_licencia UNIQUE (licencia),
    CONSTRAINT ck_conductor_estado CHECK (estado_disponibilidad IN ('DISPONIBLE', 'EN_RUTA', 'INACTIVO')),
    CONSTRAINT ck_conductor_turno CHECK (turno IN ('MANANA', 'TARDE'))
);

CREATE TABLE administrador (
    usuario_id    BIGINT PRIMARY KEY REFERENCES usuario (id),
    cargo         VARCHAR(100),
    nivel_acceso  VARCHAR(30)
);

-- ---------------------------------------------------------------------
-- Autenticación
-- ---------------------------------------------------------------------
CREATE TABLE codigo_otp (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    telefono     VARCHAR(15)  NOT NULL,
    codigo_hash  VARCHAR(100) NOT NULL,
    proposito    VARCHAR(10)  NOT NULL,
    expira_en    TIMESTAMPTZ  NOT NULL,
    intentos     INTEGER      NOT NULL DEFAULT 0,
    usado        BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_otp_proposito CHECK (proposito IN ('REGISTRO', 'LOGIN')),
    CONSTRAINT ck_otp_intentos CHECK (intentos >= 0)
);
-- Límite de envíos por hora y búsqueda del último código vigente
CREATE INDEX ix_otp_telefono_creado ON codigo_otp (telefono, creado_en DESC);

CREATE TABLE refresh_token (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL REFERENCES usuario (id),
    token_hash  VARCHAR(100) NOT NULL,
    expira_en   TIMESTAMPTZ  NOT NULL,
    revocado    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash)
);
CREATE INDEX ix_refresh_token_usuario ON refresh_token (usuario_id);

CREATE TABLE dispositivo_push (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id      BIGINT       NOT NULL REFERENCES usuario (id),
    expo_token      VARCHAR(255) NOT NULL,
    plataforma      VARCHAR(10),
    actualizado_en  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_dispositivo_expo_token UNIQUE (expo_token)
);
CREATE INDEX ix_dispositivo_usuario ON dispositivo_push (usuario_id);

-- ---------------------------------------------------------------------
-- Catálogos
-- ---------------------------------------------------------------------
CREATE TABLE institucion_critica (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre     VARCHAR(150)  NOT NULL,
    tipo       VARCHAR(15)   NOT NULL,
    sector_id  BIGINT        NOT NULL REFERENCES sector (id),
    latitud    NUMERIC(9, 6) NOT NULL,
    longitud   NUMERIC(9, 6) NOT NULL,
    activo     BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_institucion_tipo CHECK (tipo IN ('SALUD', 'BOMBEROS', 'EDUCACION', 'OTRO'))
);
CREATE INDEX ix_institucion_sector ON institucion_critica (sector_id);

CREATE TABLE punto_llenado (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre     VARCHAR(150)  NOT NULL,
    tipo       VARCHAR(10)   NOT NULL,
    direccion  VARCHAR(255),
    latitud    NUMERIC(9, 6) NOT NULL,
    longitud   NUMERIC(9, 6) NOT NULL,
    activo     BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_punto_llenado_tipo CHECK (tipo IN ('PLANTA', 'SURTIDOR', 'POZO'))
);

CREATE TABLE cisterna (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    placa                       VARCHAR(10) NOT NULL,
    capacidad_litros            INTEGER     NOT NULL,
    estado_vehiculo             VARCHAR(15) NOT NULL DEFAULT 'OPERATIVA',
    nro_autorizacion_sanitaria  VARCHAR(50),
    vence_autorizacion          DATE,
    CONSTRAINT uk_cisterna_placa UNIQUE (placa),
    CONSTRAINT ck_cisterna_capacidad CHECK (capacidad_litros > 0),
    CONSTRAINT ck_cisterna_estado CHECK (estado_vehiculo IN ('OPERATIVA', 'MANTENIMIENTO', 'AVERIADA'))
);

-- ---------------------------------------------------------------------
-- Operación: rutas, viajes, paradas, GPS
-- ---------------------------------------------------------------------
CREATE TABLE ruta (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fecha_programada     DATE        NOT NULL,
    turno                VARCHAR(10) NOT NULL,
    sector_id            BIGINT      NOT NULL REFERENCES sector (id),
    conductor_id         BIGINT      REFERENCES conductor (usuario_id),
    cisterna_id          BIGINT      REFERENCES cisterna (id),
    administrador_id     BIGINT      NOT NULL REFERENCES administrador (usuario_id),
    viajes_planificados  INTEGER,
    estado               VARCHAR(15) NOT NULL DEFAULT 'PROGRAMADA',
    inicio               TIMESTAMPTZ,
    fin                  TIMESTAMPTZ,
    creado_en            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_ruta_turno CHECK (turno IN ('MANANA', 'TARDE')),
    CONSTRAINT ck_ruta_estado CHECK (estado IN ('PROGRAMADA', 'EN_CURSO', 'INTERRUMPIDA', 'COMPLETADA')),
    CONSTRAINT ck_ruta_viajes CHECK (viajes_planificados IS NULL OR viajes_planificados > 0),
    CONSTRAINT ck_ruta_fechas CHECK (fin IS NULL OR inicio IS NULL OR fin >= inicio)
);
CREATE INDEX ix_ruta_fecha_estado ON ruta (fecha_programada, estado);
CREATE INDEX ix_ruta_sector ON ruta (sector_id);
-- Regla 2: un conductor no puede tener dos rutas en la misma fecha y turno
CREATE UNIQUE INDEX uk_ruta_conductor_fecha_turno
    ON ruta (conductor_id, fecha_programada, turno)
    WHERE conductor_id IS NOT NULL;

CREATE TABLE viaje (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ruta_id                 BIGINT      NOT NULL REFERENCES ruta (id),
    nro_viaje               INTEGER     NOT NULL,
    punto_llenado_id        BIGINT      NOT NULL REFERENCES punto_llenado (id),
    volumen_cargado_litros  INTEGER,
    salida                  TIMESTAMPTZ,
    retorno                 TIMESTAMPTZ,
    estado                  VARCHAR(15) NOT NULL DEFAULT 'EN_CARGA',
    CONSTRAINT uk_viaje_ruta_nro UNIQUE (ruta_id, nro_viaje),
    CONSTRAINT ck_viaje_nro CHECK (nro_viaje > 0),
    CONSTRAINT ck_viaje_volumen CHECK (volumen_cargado_litros IS NULL OR volumen_cargado_litros >= 0),
    CONSTRAINT ck_viaje_estado CHECK (estado IN ('EN_CARGA', 'EN_REPARTO', 'FINALIZADO'))
);

CREATE TABLE parada (
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ruta_id                   BIGINT        NOT NULL REFERENCES ruta (id),
    viaje_id                  BIGINT        REFERENCES viaje (id),
    tipo                      VARCHAR(15)   NOT NULL,
    vecino_id                 BIGINT        REFERENCES vecino (usuario_id),
    institucion_id            BIGINT        REFERENCES institucion_critica (id),
    latitud                   NUMERIC(9, 6) NOT NULL,
    longitud                  NUMERIC(9, 6) NOT NULL,
    orden                     INTEGER       NOT NULL,
    prioridad                 VARCHAR(10)   NOT NULL,
    estado                    VARCHAR(15)   NOT NULL DEFAULT 'PENDIENTE',
    volumen_entregado_litros  INTEGER,
    verificada_gps            BOOLEAN       NOT NULL DEFAULT FALSE,
    atendida_en               TIMESTAMPTZ,
    CONSTRAINT ck_parada_tipo CHECK (tipo IN ('HOGAR', 'INSTITUCION')),
    -- Una parada HOGAR apunta a un vecino; una INSTITUCION a una institución
    CONSTRAINT ck_parada_destino CHECK (
        (tipo = 'HOGAR' AND vecino_id IS NOT NULL AND institucion_id IS NULL)
        OR (tipo = 'INSTITUCION' AND institucion_id IS NOT NULL AND vecino_id IS NULL)
    ),
    CONSTRAINT ck_parada_prioridad CHECK (prioridad IN ('CRITICA', 'NORMAL')),
    CONSTRAINT ck_parada_estado CHECK (estado IN ('PENDIENTE', 'ATENDIDA', 'NO_ATENDIDA', 'REASIGNADA')),
    CONSTRAINT ck_parada_volumen CHECK (volumen_entregado_litros IS NULL OR volumen_entregado_litros >= 0)
);
CREATE INDEX ix_parada_ruta_orden ON parada (ruta_id, orden);
CREATE INDEX ix_parada_vecino ON parada (vecino_id);

CREATE TABLE monitoreo_gps (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ruta_id        BIGINT        NOT NULL REFERENCES ruta (id),
    latitud        NUMERIC(9, 6) NOT NULL,
    longitud       NUMERIC(9, 6) NOT NULL,
    registrado_en  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX ix_monitoreo_ruta_registrado ON monitoreo_gps (ruta_id, registrado_en DESC);

-- ---------------------------------------------------------------------
-- Incidencias y calificaciones
-- ---------------------------------------------------------------------
CREATE TABLE incidencia (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ruta_id          BIGINT       NOT NULL REFERENCES ruta (id),
    parada_id        BIGINT       REFERENCES parada (id),
    reportado_por    BIGINT       NOT NULL REFERENCES usuario (id),
    tipo             VARCHAR(25)  NOT NULL,
    descripcion      VARCHAR(500),
    estado           VARCHAR(15)  NOT NULL DEFAULT 'ABIERTA',
    ruta_destino_id  BIGINT       REFERENCES ruta (id),
    resuelta_por     BIGINT       REFERENCES usuario (id),
    creado_en        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resuelto_en      TIMESTAMPTZ,
    -- Conductor: AVERIA, ACCESO_BLOQUEADO, AGUA_AGOTADA, OTRO
    -- Vecino:    NO_LLEGO, LLEGO_TARDE, YA_PASO, VOLUMEN_INSUFICIENTE, OTRO
    CONSTRAINT ck_incidencia_tipo CHECK (tipo IN (
        'AVERIA', 'ACCESO_BLOQUEADO', 'AGUA_AGOTADA',
        'NO_LLEGO', 'LLEGO_TARDE', 'YA_PASO', 'VOLUMEN_INSUFICIENTE',
        'OTRO'
    )),
    CONSTRAINT ck_incidencia_estado CHECK (estado IN ('ABIERTA', 'REASIGNADA', 'CERRADA')),
    CONSTRAINT ck_incidencia_reasignada CHECK (estado <> 'REASIGNADA' OR ruta_destino_id IS NOT NULL)
);
CREATE INDEX ix_incidencia_estado ON incidencia (estado, creado_en DESC);
CREATE INDEX ix_incidencia_ruta ON incidencia (ruta_id);

CREATE TABLE calificacion_servicio (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    parada_id   BIGINT       NOT NULL REFERENCES parada (id),
    vecino_id   BIGINT       NOT NULL REFERENCES vecino (usuario_id),
    estrellas   SMALLINT     NOT NULL,
    comentario  VARCHAR(500),
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_calificacion_parada UNIQUE (parada_id),
    CONSTRAINT ck_calificacion_estrellas CHECK (estrellas BETWEEN 1 AND 5)
);
CREATE INDEX ix_calificacion_vecino ON calificacion_servicio (vecino_id);
