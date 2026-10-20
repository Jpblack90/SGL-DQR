-- ============================================================
-- V4__modulo4_encomiendas_pagos_almacen.sql
-- SGL-DQR · Expreso Marvisur
-- MÓDULO 4: Encomiendas, Pagos y Almacén (RF-04, RF-06)
--
-- Lógica central de encomiendas, tarifas y el bloqueo físico
-- de entregas con estado_pago = 'PENDIENTE' (Contra Entrega).
-- Depende de 'clientes', 'sucursales', 'programacion_viajes' y 'empleados'.
-- ============================================================

-- ------------------------------------------------------------
-- 1. Tabla: encomiendas
-- ------------------------------------------------------------
CREATE TABLE encomiendas (
    id_encomienda           VARCHAR(36)              NOT NULL,
    codigo_comprobante      VARCHAR(50)              NOT NULL,
    id_remitente            VARCHAR(36)              NOT NULL,
    id_destinatario         VARCHAR(36)              NOT NULL,
    id_sucursal_origen      VARCHAR(36)              NOT NULL,
    id_sucursal_destino     VARCHAR(36)              NOT NULL,
    peso_kg                 NUMERIC(8,3)             NOT NULL,
    volumen_m3              NUMERIC(8,3),
    descripcion_contenido   TEXT,
    monto_total             NUMERIC(10,2)            NOT NULL,
    estado_pago             VARCHAR(20)              NOT NULL,
    estado_logistico        VARCHAR(30)              NOT NULL,
    fecha_registro          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_encomiendas PRIMARY KEY (id_encomienda),
    CONSTRAINT uq_encomiendas_codigo_comprobante UNIQUE (codigo_comprobante),
    CONSTRAINT fk_encomiendas_remitente FOREIGN KEY (id_remitente) REFERENCES clientes (id_cliente),
    CONSTRAINT fk_encomiendas_destinatario FOREIGN KEY (id_destinatario) REFERENCES clientes (id_cliente),
    CONSTRAINT fk_encomiendas_sucursal_origen FOREIGN KEY (id_sucursal_origen) REFERENCES sucursales (id_sucursal),
    CONSTRAINT fk_encomiendas_sucursal_destino FOREIGN KEY (id_sucursal_destino) REFERENCES sucursales (id_sucursal),
    CONSTRAINT ck_encomiendas_estado_pago CHECK (estado_pago IN ('PAGADO', 'PENDIENTE')),
    CONSTRAINT ck_encomiendas_estado_logistico CHECK (estado_logistico IN ('REGISTRADO', 'EN_ALMACEN_ORIGEN', 'EN_RUTA', 'EN_ALMACEN_DESTINO', 'ENTREGADO'))
);

CREATE INDEX idx_encomiendas_codigo_comprobante ON encomiendas (codigo_comprobante);
CREATE INDEX idx_encomiendas_remitente ON encomiendas (id_remitente);
CREATE INDEX idx_encomiendas_destinatario ON encomiendas (id_destinatario);
CREATE INDEX idx_encomiendas_sucursal_origen ON encomiendas (id_sucursal_origen);
CREATE INDEX idx_encomiendas_sucursal_destino ON encomiendas (id_sucursal_destino);

-- ------------------------------------------------------------
-- 2. Tabla: despachos_lote_detalles
-- ------------------------------------------------------------
CREATE TABLE despachos_lote_detalles (
    id_despacho_detalle     VARCHAR(36)              NOT NULL,
    id_viaje                VARCHAR(36)              NOT NULL,
    id_encomienda           VARCHAR(36)              NOT NULL,
    fecha_asociacion        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_despachos_lote_detalles PRIMARY KEY (id_despacho_detalle),
    CONSTRAINT fk_despachos_viaje FOREIGN KEY (id_viaje) REFERENCES programacion_viajes (id_viaje),
    CONSTRAINT fk_despachos_encomienda FOREIGN KEY (id_encomienda) REFERENCES encomiendas (id_encomienda)
);

CREATE INDEX idx_despachos_viaje ON despachos_lote_detalles (id_viaje);
CREATE INDEX idx_despachos_encomienda ON despachos_lote_detalles (id_encomienda);

-- ------------------------------------------------------------
-- 3. Tabla: incidencias_emergencia
-- ------------------------------------------------------------
CREATE TABLE incidencias_emergencia (
    id_incidencia           VARCHAR(36)              NOT NULL,
    id_encomienda           VARCHAR(36)              NOT NULL,
    id_empleado_reporta     VARCHAR(36)              NOT NULL,
    tipo_incidencia         VARCHAR(30)              NOT NULL,
    motivo_reimpresion      TEXT,
    fecha_incidencia        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_incidencias_emergencia PRIMARY KEY (id_incidencia),
    CONSTRAINT fk_incidencias_encomienda FOREIGN KEY (id_encomienda) REFERENCES encomiendas (id_encomienda),
    CONSTRAINT fk_incidencias_empleado FOREIGN KEY (id_empleado_reporta) REFERENCES empleados (id_empleado),
    CONSTRAINT ck_incidencias_tipo CHECK (tipo_incidencia IN ('ETIQUETA_DANADA', 'ETIQUETA_PERDIDA'))
);

CREATE INDEX idx_incidencias_encomienda ON incidencias_emergencia (id_encomienda);
CREATE INDEX idx_incidencias_empleado ON incidencias_emergencia (id_empleado_reporta);
