-- ============================================================
-- V3__modulo3_clientes_preregistros.sql
-- SGL-DQR · Expreso Marvisur
-- MÓDULO 3: Clientes y Pre-Registros (RF-08, RF-10)
--
-- Administra clientes (remitentes/destinatarios) y la persistencia
-- temporal del Pre-Registro Web ('Código Express').
-- Depende de 'sucursales' (creada en V1).
-- ============================================================

-- ------------------------------------------------------------
-- 1. Tabla: clientes
-- ------------------------------------------------------------
CREATE TABLE clientes (
    id_cliente              VARCHAR(36)  NOT NULL,
    tipo_documento          VARCHAR(20)  NOT NULL,
    numero_documento        VARCHAR(12)  NOT NULL,
    nombres_razon_social    VARCHAR(200) NOT NULL,
    telefono                VARCHAR(20),
    correo                  VARCHAR(150),
    CONSTRAINT pk_clientes PRIMARY KEY (id_cliente),
    CONSTRAINT uq_clientes_numero_documento UNIQUE (numero_documento),
    CONSTRAINT ck_clientes_tipo_documento CHECK (tipo_documento IN ('DNI', 'RUC'))
);

CREATE INDEX idx_clientes_numero_documento ON clientes (numero_documento);

-- ------------------------------------------------------------
-- 2. Tabla: pre_registros_express
-- ------------------------------------------------------------
CREATE TABLE pre_registros_express (
    id_preregistro          VARCHAR(36)              NOT NULL,
    codigo_express          VARCHAR(50)              NOT NULL,
    id_cliente_remitente    VARCHAR(36)              NOT NULL,
    id_cliente_destinatario VARCHAR(36)              NOT NULL,
    id_sucursal_destino     VARCHAR(36)              NOT NULL,
    peso_estimado           NUMERIC(8,3)             NOT NULL,
    monto_cotizado          NUMERIC(10,2)            NOT NULL,
    fecha_creacion          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    estado                  VARCHAR(30)              NOT NULL,
    CONSTRAINT pk_pre_registros_express PRIMARY KEY (id_preregistro),
    CONSTRAINT fk_preregistro_remitente FOREIGN KEY (id_cliente_remitente) REFERENCES clientes (id_cliente),
    CONSTRAINT fk_preregistro_destinatario FOREIGN KEY (id_cliente_destinatario) REFERENCES clientes (id_cliente),
    CONSTRAINT fk_preregistro_sucursal_destino FOREIGN KEY (id_sucursal_destino) REFERENCES sucursales (id_sucursal),
    CONSTRAINT uq_preregistros_codigo_express UNIQUE (codigo_express),
    CONSTRAINT ck_preregistros_estado CHECK (estado IN ('PENDIENTE_VENTANILLA', 'PROCESADO', 'EXPIRADO'))
);

CREATE INDEX idx_preregistros_codigo_express ON pre_registros_express (codigo_express);
CREATE INDEX idx_preregistros_remitente ON pre_registros_express (id_cliente_remitente);
CREATE INDEX idx_preregistros_destinatario ON pre_registros_express (id_cliente_destinatario);
CREATE INDEX idx_preregistros_sucursal_destino ON pre_registros_express (id_sucursal_destino);
