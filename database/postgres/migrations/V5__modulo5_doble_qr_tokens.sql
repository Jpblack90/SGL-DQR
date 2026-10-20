-- ============================================================
-- V5__modulo5_doble_qr_tokens.sql
-- SGL-DQR · Expreso Marvisur
-- MÓDULO 5: Motor Criptográfico del Doble QR y Tokens (RF-07, RF-09, RNF-01)
--
-- Estructura del par de códigos enlazados (QR1 Operativo / QR2 Llave Digital),
-- el ciclo de vida del hash y auditoría de validaciones cruzadas.
-- Depende de 'encomiendas' (V4) y 'empleados' (V2).
-- ============================================================

-- ------------------------------------------------------------
-- 1. Tabla: parejas_doble_qr
-- ------------------------------------------------------------
CREATE TABLE parejas_doble_qr (
    id_pareja_qr            VARCHAR(36)              NOT NULL,
    id_encomienda           VARCHAR(36)              NOT NULL,
    uuid_qr1_operativo      VARCHAR(100)             NOT NULL,
    hash_qr2_llave_digital  VARCHAR(255)             NOT NULL,
    fecha_generacion        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_parejas_doble_qr PRIMARY KEY (id_pareja_qr),
    CONSTRAINT uq_parejas_encomienda UNIQUE (id_encomienda),
    CONSTRAINT fk_parejas_encomienda FOREIGN KEY (id_encomienda) REFERENCES encomiendas (id_encomienda) ON DELETE CASCADE
);

CREATE INDEX idx_parejas_encomienda ON parejas_doble_qr (id_encomienda);

-- ------------------------------------------------------------
-- 2. Tabla: tokens_qr2_estatus
-- ------------------------------------------------------------
CREATE TABLE tokens_qr2_estatus (
    id_token_status         VARCHAR(36)              NOT NULL,
    id_pareja_qr            VARCHAR(36)              NOT NULL,
    estado_token            VARCHAR(20)              NOT NULL,
    fecha_invalidacion      TIMESTAMP WITH TIME ZONE,
    motivo_inactivacion     VARCHAR(30),
    CONSTRAINT pk_tokens_qr2_estatus PRIMARY KEY (id_token_status),
    CONSTRAINT fk_tokens_pareja FOREIGN KEY (id_pareja_qr) REFERENCES parejas_doble_qr (id_pareja_qr) ON DELETE CASCADE,
    CONSTRAINT ck_tokens_estado CHECK (estado_token IN ('ACTIVO', 'INVALIDADO', 'EXPIRADO')),
    CONSTRAINT ck_tokens_motivo CHECK (motivo_inactivacion IS NULL OR motivo_inactivacion IN ('ENTREGA_EXITOSA', 'INTENTO_FRAUDE'))
);

CREATE INDEX idx_tokens_pareja ON tokens_qr2_estatus (id_pareja_qr);

-- ------------------------------------------------------------
-- 3. Tabla: validaciones_cruzadas_logs
-- ------------------------------------------------------------
CREATE TABLE validaciones_cruzadas_logs (
    id_validacion               VARCHAR(36)              NOT NULL,
    id_pareja_qr                VARCHAR(36)              NOT NULL,
    id_empleado_valida          VARCHAR(36)              NOT NULL,
    resultado                   VARCHAR(40)              NOT NULL,
    latencia_procesamiento_ms   INT                      NOT NULL,
    fecha_validacion            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_validaciones_cruzadas_logs PRIMARY KEY (id_validacion),
    CONSTRAINT fk_validaciones_pareja FOREIGN KEY (id_pareja_qr) REFERENCES parejas_doble_qr (id_pareja_qr),
    CONSTRAINT fk_validaciones_empleado FOREIGN KEY (id_empleado_valida) REFERENCES empleados (id_empleado),
    CONSTRAINT ck_validaciones_resultado CHECK (resultado IN ('EXITOSO', 'FALLIDO_PAGO_PENDIENTE', 'FALLIDO_TOKEN_INVALIDO'))
);

CREATE INDEX idx_validaciones_pareja ON validaciones_cruzadas_logs (id_pareja_qr);
CREATE INDEX idx_validaciones_empleado ON validaciones_cruzadas_logs (id_empleado_valida);
