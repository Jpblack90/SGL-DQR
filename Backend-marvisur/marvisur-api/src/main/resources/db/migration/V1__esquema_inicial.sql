-- ============================================================
-- V1__esquema_inicial.sql
-- SGL-DQR · Expreso Marvisur
-- Migración inicial: esquema completo de la base de datos.
--
-- Reglas de diseño:
--   · PKs y FKs son UUID VARCHAR(36), generados en el cliente.
--   · Hashes SHA-256 almacenados como CHAR(64) (nunca el QR en claro).
--   · Todos los timestamps en UTC con zona horaria.
--   · Índices en columnas de búsqueda frecuente.
-- ============================================================

-- ============================================================
-- 1. AGENCIA
--    Sucursal de Expreso Marvisur (origen o destino de envíos).
-- ============================================================
CREATE TABLE agencia (
    id          VARCHAR(36)  NOT NULL,
    codigo      VARCHAR(20)  NOT NULL,
    nombre      VARCHAR(150) NOT NULL,
    direccion   VARCHAR(250) NOT NULL,
    ciudad      VARCHAR(100) NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_agencia PRIMARY KEY (id),
    CONSTRAINT uq_agencia_codigo UNIQUE (codigo)
);

COMMENT ON TABLE  agencia            IS 'Sucursales de Expreso Marvisur.';
COMMENT ON COLUMN agencia.id         IS 'UUID generado en el cliente.';
COMMENT ON COLUMN agencia.codigo     IS 'Código corto único de la agencia (ej.: LIM-01).';

-- ============================================================
-- 2. VEHICULO
--    Unidades de transporte de la flota Marvisur.
-- ============================================================
CREATE TABLE vehiculo (
    id          VARCHAR(36)  NOT NULL,
    placa       VARCHAR(10)  NOT NULL,
    chofer_id   VARCHAR(36),          -- FK a usuario (se agrega tras crear usuario)
    estado      VARCHAR(30)  NOT NULL DEFAULT 'DISPONIBLE',
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_vehiculo  PRIMARY KEY (id),
    CONSTRAINT uq_vehiculo_placa UNIQUE (placa),
    CONSTRAINT ck_vehiculo_estado CHECK (estado IN ('DISPONIBLE','EN_RUTA','EN_MANTENIMIENTO','INACTIVO'))
);

COMMENT ON TABLE  vehiculo          IS 'Flota de vehículos de transporte.';
COMMENT ON COLUMN vehiculo.chofer_id IS 'UUID del usuario con rol TN4_CHOFER asignado.';

-- ============================================================
-- 3. RUTA
--    Trayecto entre dos agencias, cubierto por un vehículo.
-- ============================================================
CREATE TABLE ruta (
    id                  VARCHAR(36)  NOT NULL,
    agencia_origen_id   VARCHAR(36)  NOT NULL,
    agencia_destino_id  VARCHAR(36)  NOT NULL,
    vehiculo_id         VARCHAR(36),
    estado              VARCHAR(30)  NOT NULL DEFAULT 'PROGRAMADA',
    hora_salida         TIME,
    fecha_salida        DATE,
    creado_en           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_ruta              PRIMARY KEY (id),
    CONSTRAINT fk_ruta_origen       FOREIGN KEY (agencia_origen_id)  REFERENCES agencia (id),
    CONSTRAINT fk_ruta_destino      FOREIGN KEY (agencia_destino_id) REFERENCES agencia (id),
    CONSTRAINT fk_ruta_vehiculo     FOREIGN KEY (vehiculo_id)        REFERENCES vehiculo (id),
    CONSTRAINT ck_ruta_estado       CHECK (estado IN ('PROGRAMADA','EN_TRANSITO','LLEGADA_REGISTRADA','CANCELADA'))
);

COMMENT ON TABLE ruta IS 'Trayectos planificados entre agencias.';

-- ============================================================
-- 4. USUARIO
--    Personal interno de Marvisur (no clientes finales).
--    El cliente accede sin cuenta (token temporal).
-- ============================================================
CREATE TABLE usuario (
    id               VARCHAR(36)  NOT NULL,
    nombre_completo  VARCHAR(200) NOT NULL,
    nombre_usuario   VARCHAR(80)  NOT NULL,
    hash_contrasena  VARCHAR(255) NOT NULL,   -- bcrypt, nunca texto en claro
    rol              VARCHAR(30)  NOT NULL,
    agencia_id       VARCHAR(36)  NOT NULL,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    ultimo_acceso    TIMESTAMPTZ,
    CONSTRAINT pk_usuario           PRIMARY KEY (id),
    CONSTRAINT uq_usuario_nombre    UNIQUE (nombre_usuario),
    CONSTRAINT fk_usuario_agencia   FOREIGN KEY (agencia_id) REFERENCES agencia (id),
    CONSTRAINT ck_usuario_rol       CHECK (rol IN (
        'TN1_RECEPCIONISTA','TN2_ALMACENERO','TN3_ADMINISTRADOR','TN4_CHOFER'
    ))
);

COMMENT ON TABLE  usuario                  IS 'Personal interno de Expreso Marvisur.';
COMMENT ON COLUMN usuario.hash_contrasena  IS 'Hash bcrypt de la contraseña. Nunca se almacena en claro.';
COMMENT ON COLUMN usuario.rol              IS 'Rol del actor: TN1 Recepcionista, TN2 Almacenero, TN3 Admin, TN4 Chofer.';

-- FK diferida: vehiculo.chofer_id -> usuario
ALTER TABLE vehiculo
    ADD CONSTRAINT fk_vehiculo_chofer FOREIGN KEY (chofer_id) REFERENCES usuario (id);

-- ============================================================
-- 5. ENCOMIENDA
--    Paquete registrado para envío entre agencias.
-- ============================================================
CREATE TABLE encomienda (
    id                   VARCHAR(36)     NOT NULL,
    remitente            VARCHAR(200)    NOT NULL,
    destinatario         VARCHAR(200)    NOT NULL,
    peso_kg              NUMERIC(8,3)    NOT NULL,
    tarifa               NUMERIC(10,2)   NOT NULL,
    estado_pago          VARCHAR(20)     NOT NULL DEFAULT 'PENDIENTE',
    estado_envio         VARCHAR(30)     NOT NULL DEFAULT 'REGISTRADA',
    agencia_origen_id    VARCHAR(36)     NOT NULL,
    agencia_destino_id   VARCHAR(36)     NOT NULL,
    ruta_id              VARCHAR(36),
    registrado_por_id    VARCHAR(36),              -- usuario TN1
    creado_en            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    actualizado_en       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_encomienda             PRIMARY KEY (id),
    CONSTRAINT fk_enc_agencia_origen     FOREIGN KEY (agencia_origen_id)  REFERENCES agencia  (id),
    CONSTRAINT fk_enc_agencia_destino    FOREIGN KEY (agencia_destino_id) REFERENCES agencia  (id),
    CONSTRAINT fk_enc_ruta               FOREIGN KEY (ruta_id)            REFERENCES ruta     (id),
    CONSTRAINT fk_enc_usuario            FOREIGN KEY (registrado_por_id)  REFERENCES usuario  (id),
    CONSTRAINT ck_encomienda_estado_pago CHECK (estado_pago IN ('PENDIENTE','PAGADO','CANCELADO')),
    CONSTRAINT ck_encomienda_estado_envio CHECK (estado_envio IN (
        'REGISTRADA','EN_TRANSITO','EN_ALMACEN_DESTINO','BLOQUEADA','ENTREGADA','CANCELADA'
    )),
    CONSTRAINT ck_encomienda_peso CHECK (peso_kg > 0),
    CONSTRAINT ck_encomienda_tarifa CHECK (tarifa >= 5.00)
);

COMMENT ON TABLE  encomienda              IS 'Paquetes registrados en el sistema SGL-DQR.';
COMMENT ON COLUMN encomienda.estado_pago  IS 'PENDIENTE = Contra Entrega (bloquea entrega). PAGADO = contado.';
COMMENT ON COLUMN encomienda.estado_envio IS 'Ciclo de vida: REGISTRADA -> EN_TRANSITO -> EN_ALMACEN_DESTINO -> ENTREGADA.';

-- Indices de busqueda frecuente
CREATE INDEX idx_encomienda_estado_envio  ON encomienda (estado_envio);
CREATE INDEX idx_encomienda_estado_pago   ON encomienda (estado_pago);
CREATE INDEX idx_encomienda_destinatario  ON encomienda (destinatario);
CREATE INDEX idx_encomienda_agencia_dest  ON encomienda (agencia_destino_id);

-- ============================================================
-- 6. PAR_DOBLE_QR  (= CodigoQR en el dominio)
--    Hashes SHA-256 del par QR1+QR2. Nunca los valores en claro.
-- ============================================================
CREATE TABLE par_doble_qr (
    encomienda_id       VARCHAR(36)  NOT NULL,
    qr1_hash            CHAR(64)     NOT NULL,   -- SHA-256 hex (64 chars)
    qr2_hash            CHAR(64)     NOT NULL,   -- SHA-256 hex (64 chars)
    qr2_estado          VARCHAR(15)  NOT NULL DEFAULT 'ACTIVO',
    fecha_invalidacion  TIMESTAMPTZ,
    creado_en           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_par_doble_qr        PRIMARY KEY (encomienda_id),
    CONSTRAINT fk_pdqr_encomienda     FOREIGN KEY (encomienda_id) REFERENCES encomienda (id) ON DELETE CASCADE,
    CONSTRAINT uq_pdqr_qr1_hash       UNIQUE (qr1_hash),
    CONSTRAINT uq_pdqr_qr2_hash       UNIQUE (qr2_hash),
    CONSTRAINT ck_pdqr_qr2_estado     CHECK (qr2_estado IN ('ACTIVO','INVALIDADO'))
);

COMMENT ON TABLE  par_doble_qr                IS 'Par de hashes SHA-256 del Doble QR por encomienda. Relacion 1:1.';
COMMENT ON COLUMN par_doble_qr.qr1_hash       IS 'SHA-256 de los bytes PNG del QR1 fisico impreso en ticket.';
COMMENT ON COLUMN par_doble_qr.qr2_hash       IS 'SHA-256 de los bytes PNG del QR2 digital enviado al cliente.';
COMMENT ON COLUMN par_doble_qr.qr2_estado     IS 'ACTIVO o INVALIDADO (irreversible tras entrega exitosa).';
COMMENT ON COLUMN par_doble_qr.fecha_invalidacion IS 'Timestamp UTC de invalidacion del QR2. NULL si aun ACTIVO.';

-- Indices para validacion cruzada rapida (RNF-03 <= 1000 ms)
CREATE INDEX idx_pdqr_qr1_hash ON par_doble_qr (qr1_hash);
CREATE INDEX idx_pdqr_qr2_hash ON par_doble_qr (qr2_hash);

-- ============================================================
-- 7. COMPROBANTE
--    Comprobante de pago asociado a una encomienda.
-- ============================================================
CREATE TABLE comprobante (
    id              VARCHAR(36)  NOT NULL,
    encomienda_id   VARCHAR(36)  NOT NULL,
    tipo            VARCHAR(20)  NOT NULL DEFAULT 'BOLETA',  -- BOLETA, FACTURA
    serie           VARCHAR(10)  NOT NULL,
    numero          INTEGER      NOT NULL,
    monto           NUMERIC(10,2) NOT NULL,
    emitido_en      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_comprobante           PRIMARY KEY (id),
    CONSTRAINT fk_comprobante_enc       FOREIGN KEY (encomienda_id) REFERENCES encomienda (id),
    CONSTRAINT uq_comprobante_serie_num UNIQUE (serie, numero),
    CONSTRAINT ck_comprobante_tipo      CHECK (tipo IN ('BOLETA','FACTURA'))
);

COMMENT ON TABLE comprobante IS 'Comprobantes de pago (boletas/facturas) por encomienda.';

-- ============================================================
-- 8. AUDITORIA
--    Registro de cambios de estado para trazabilidad.
-- ============================================================
CREATE TABLE auditoria (
    id              BIGSERIAL    NOT NULL,
    entidad         VARCHAR(50)  NOT NULL,   -- 'ENCOMIENDA', 'PAR_DOBLE_QR', etc.
    entidad_id      VARCHAR(36)  NOT NULL,
    accion          VARCHAR(50)  NOT NULL,   -- 'CREAR', 'CAMBIAR_ESTADO', 'INVALIDAR_QR2', etc.
    estado_anterior VARCHAR(50),
    estado_nuevo    VARCHAR(50),
    usuario_id      VARCHAR(36),
    ip_origen       VARCHAR(45),
    detalle         TEXT,
    ocurrido_en     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_auditoria PRIMARY KEY (id)
);

CREATE INDEX idx_auditoria_entidad_id ON auditoria (entidad_id);
CREATE INDEX idx_auditoria_ocurrido   ON auditoria (ocurrido_en DESC);

COMMENT ON TABLE  auditoria        IS 'Registro de auditoria inmutable para trazabilidad de encomiendas.';
COMMENT ON COLUMN auditoria.accion IS 'Verbo que describe la operacion realizada.';

-- ============================================================
-- 9. TOKEN_ACCESO_CLIENTE
--    Token temporal para que el cliente acceda sin cuenta.
--    Accede al portal web con este token; expira automaticamente.
-- ============================================================
CREATE TABLE token_acceso_cliente (
    token           VARCHAR(128) NOT NULL,
    encomienda_id   VARCHAR(36)  NOT NULL,
    expira_en       TIMESTAMPTZ  NOT NULL,
    usado           BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_token_acceso     PRIMARY KEY (token),
    CONSTRAINT fk_token_encomienda FOREIGN KEY (encomienda_id) REFERENCES encomienda (id) ON DELETE CASCADE
);

CREATE INDEX idx_token_encomienda ON token_acceso_cliente (encomienda_id);
CREATE INDEX idx_token_expira     ON token_acceso_cliente (expira_en);

COMMENT ON TABLE  token_acceso_cliente      IS 'Tokens temporales para acceso del cliente sin cuenta (portal web).';
COMMENT ON COLUMN token_acceso_cliente.usado IS 'TRUE si el cliente ya consulto su encomienda con este token.';
