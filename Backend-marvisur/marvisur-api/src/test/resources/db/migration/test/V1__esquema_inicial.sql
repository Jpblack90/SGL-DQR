-- ============================================================
-- V1__esquema_inicial.sql (version H2 para tests)
-- SGL-DQR · Expreso Marvisur
--
-- Equivalente al esquema PostgreSQL pero adaptado a H2:
--   · TIMESTAMPTZ -> TIMESTAMP WITH TIME ZONE
--   · BIGSERIAL -> BIGINT AUTO_INCREMENT
--   · CHAR(64) -> VARCHAR(64) (H2 no diferencia)
--   · NOW() -> CURRENT_TIMESTAMP
--   · Sin COMMENT ON (H2 los ignora o falla)
-- ============================================================

CREATE TABLE IF NOT EXISTS agencia (
    id        VARCHAR(36)  NOT NULL,
    codigo    VARCHAR(20)  NOT NULL,
    nombre    VARCHAR(150) NOT NULL,
    direccion VARCHAR(250) NOT NULL,
    ciudad    VARCHAR(100) NOT NULL,
    activo    BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_agencia PRIMARY KEY (id),
    CONSTRAINT uq_agencia_codigo UNIQUE (codigo)
);

CREATE TABLE IF NOT EXISTS vehiculo (
    id        VARCHAR(36) NOT NULL,
    placa     VARCHAR(10) NOT NULL,
    chofer_id VARCHAR(36),
    estado    VARCHAR(30) NOT NULL DEFAULT 'DISPONIBLE',
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_vehiculo PRIMARY KEY (id),
    CONSTRAINT uq_vehiculo_placa UNIQUE (placa)
);

CREATE TABLE IF NOT EXISTS ruta (
    id                  VARCHAR(36) NOT NULL,
    agencia_origen_id   VARCHAR(36) NOT NULL,
    agencia_destino_id  VARCHAR(36) NOT NULL,
    vehiculo_id         VARCHAR(36),
    estado              VARCHAR(30) NOT NULL DEFAULT 'PROGRAMADA',
    hora_salida         TIME,
    fecha_salida        DATE,
    creado_en           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_ruta         PRIMARY KEY (id),
    CONSTRAINT fk_ruta_origen  FOREIGN KEY (agencia_origen_id)  REFERENCES agencia (id),
    CONSTRAINT fk_ruta_destino FOREIGN KEY (agencia_destino_id) REFERENCES agencia (id),
    CONSTRAINT fk_ruta_vehiculo FOREIGN KEY (vehiculo_id)       REFERENCES vehiculo (id)
);

CREATE TABLE IF NOT EXISTS usuario (
    id               VARCHAR(36)  NOT NULL,
    nombre_completo  VARCHAR(200) NOT NULL,
    nombre_usuario   VARCHAR(80)  NOT NULL,
    hash_contrasena  VARCHAR(255) NOT NULL,
    rol              VARCHAR(30)  NOT NULL,
    agencia_id       VARCHAR(36)  NOT NULL,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso    TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_usuario         PRIMARY KEY (id),
    CONSTRAINT uq_usuario_nombre  UNIQUE (nombre_usuario),
    CONSTRAINT fk_usuario_agencia FOREIGN KEY (agencia_id) REFERENCES agencia (id)
);

ALTER TABLE vehiculo ADD CONSTRAINT fk_vehiculo_chofer
    FOREIGN KEY (chofer_id) REFERENCES usuario (id);

CREATE TABLE IF NOT EXISTS encomienda (
    id                   VARCHAR(36)   NOT NULL,
    remitente            VARCHAR(200)  NOT NULL,
    destinatario         VARCHAR(200)  NOT NULL,
    peso_kg              NUMERIC(8,3)  NOT NULL,
    tarifa               NUMERIC(10,2) NOT NULL,
    estado_pago          VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    estado_envio         VARCHAR(30)   NOT NULL DEFAULT 'REGISTRADA',
    agencia_origen_id    VARCHAR(36)   NOT NULL,
    agencia_destino_id   VARCHAR(36)   NOT NULL,
    ruta_id              VARCHAR(36),
    registrado_por_id    VARCHAR(36),
    creado_en            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_encomienda          PRIMARY KEY (id),
    CONSTRAINT fk_enc_agencia_origen  FOREIGN KEY (agencia_origen_id)  REFERENCES agencia (id),
    CONSTRAINT fk_enc_agencia_destino FOREIGN KEY (agencia_destino_id) REFERENCES agencia (id),
    CONSTRAINT fk_enc_ruta            FOREIGN KEY (ruta_id)            REFERENCES ruta    (id),
    CONSTRAINT fk_enc_usuario         FOREIGN KEY (registrado_por_id)  REFERENCES usuario (id)
);

CREATE INDEX IF NOT EXISTS idx_encomienda_estado_envio ON encomienda (estado_envio);
CREATE INDEX IF NOT EXISTS idx_encomienda_estado_pago  ON encomienda (estado_pago);

CREATE TABLE IF NOT EXISTS par_doble_qr (
    encomienda_id       VARCHAR(36) NOT NULL,
    qr1_hash            VARCHAR(64) NOT NULL,
    qr2_hash            VARCHAR(64) NOT NULL,
    qr2_estado          VARCHAR(15) NOT NULL DEFAULT 'ACTIVO',
    fecha_invalidacion  TIMESTAMP WITH TIME ZONE,
    creado_en           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_par_doble_qr    PRIMARY KEY (encomienda_id),
    CONSTRAINT fk_pdqr_encomienda FOREIGN KEY (encomienda_id) REFERENCES encomienda (id) ON DELETE CASCADE,
    CONSTRAINT uq_pdqr_qr1_hash   UNIQUE (qr1_hash),
    CONSTRAINT uq_pdqr_qr2_hash   UNIQUE (qr2_hash)
);

CREATE INDEX IF NOT EXISTS idx_pdqr_qr1_hash ON par_doble_qr (qr1_hash);
CREATE INDEX IF NOT EXISTS idx_pdqr_qr2_hash ON par_doble_qr (qr2_hash);

CREATE TABLE IF NOT EXISTS comprobante (
    id            VARCHAR(36)   NOT NULL,
    encomienda_id VARCHAR(36)   NOT NULL,
    tipo          VARCHAR(20)   NOT NULL DEFAULT 'BOLETA',
    serie         VARCHAR(10)   NOT NULL,
    numero        INTEGER       NOT NULL,
    monto         NUMERIC(10,2) NOT NULL,
    emitido_en    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_comprobante           PRIMARY KEY (id),
    CONSTRAINT fk_comprobante_enc       FOREIGN KEY (encomienda_id) REFERENCES encomienda (id),
    CONSTRAINT uq_comprobante_serie_num UNIQUE (serie, numero)
);

CREATE TABLE IF NOT EXISTS auditoria (
    id              BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    entidad         VARCHAR(50)  NOT NULL,
    entidad_id      VARCHAR(36)  NOT NULL,
    accion          VARCHAR(50)  NOT NULL,
    estado_anterior VARCHAR(50),
    estado_nuevo    VARCHAR(50),
    usuario_id      VARCHAR(36),
    ip_origen       VARCHAR(45),
    detalle         TEXT,
    ocurrido_en     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_auditoria_entidad_id ON auditoria (entidad_id);

CREATE TABLE IF NOT EXISTS token_acceso_cliente (
    token         VARCHAR(128) NOT NULL,
    encomienda_id VARCHAR(36)  NOT NULL,
    expira_en     TIMESTAMP WITH TIME ZONE NOT NULL,
    usado         BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_token_acceso     PRIMARY KEY (token),
    CONSTRAINT fk_token_encomienda FOREIGN KEY (encomienda_id) REFERENCES encomienda (id) ON DELETE CASCADE
);
