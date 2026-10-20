-- ============================================================
-- V6__modulo6_tracking_comprobantes.sql
-- SGL-DQR · Expreso Marvisur
-- MÓDULO 6: Trazabilidad, Tracking y Comprobantes PDF (RF-08, RF-11)
--
-- Soporta la línea de tiempo pública que ve el cliente en la web
-- y la persistencia de comprobantes y sus archivos PDF asociados.
-- Depende de 'encomiendas' (V4) y 'sucursales' (V1).
-- ============================================================

-- ------------------------------------------------------------
-- 1. Tabla: historial_tracking
-- ------------------------------------------------------------
CREATE TABLE historial_tracking (
    id_tracking         VARCHAR(36)              NOT NULL,
    id_encomienda       VARCHAR(36)              NOT NULL,
    id_sucursal_actual  VARCHAR(36)              NOT NULL,
    estado_evento       VARCHAR(50)              NOT NULL,
    observacion         TEXT,
    fecha_hora_evento   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_historial_tracking PRIMARY KEY (id_tracking),
    CONSTRAINT fk_tracking_encomienda FOREIGN KEY (id_encomienda) REFERENCES encomiendas (id_encomienda) ON DELETE CASCADE,
    CONSTRAINT fk_tracking_sucursal FOREIGN KEY (id_sucursal_actual) REFERENCES sucursales (id_sucursal)
);

CREATE INDEX idx_tracking_encomienda ON historial_tracking (id_encomienda);
CREATE INDEX idx_tracking_sucursal ON historial_tracking (id_sucursal_actual);

-- ------------------------------------------------------------
-- 2. Tabla: comprobantes_pago
-- ------------------------------------------------------------
CREATE TABLE comprobantes_pago (
    id_comprobante      VARCHAR(36)              NOT NULL,
    id_encomienda       VARCHAR(36)              NOT NULL,
    tipo_comprobante    VARCHAR(20)              NOT NULL,
    serie_correlativo   VARCHAR(50)              NOT NULL,
    monto_subtotal      NUMERIC(10,2)            NOT NULL,
    monto_igv           NUMERIC(10,2)            NOT NULL,
    monto_total         NUMERIC(10,2)            NOT NULL,
    fecha_emision       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_comprobantes_pago PRIMARY KEY (id_comprobante),
    CONSTRAINT uq_comprobantes_encomienda UNIQUE (id_encomienda),
    CONSTRAINT fk_comprobantes_encomienda FOREIGN KEY (id_encomienda) REFERENCES encomiendas (id_encomienda) ON DELETE CASCADE,
    CONSTRAINT ck_comprobantes_tipo CHECK (tipo_comprobante IN ('BOLETA', 'FACTURA'))
);

CREATE INDEX idx_comprobantes_encomienda ON comprobantes_pago (id_encomienda);

-- ------------------------------------------------------------
-- 3. Tabla: archivos_pdf_comprobante
-- ------------------------------------------------------------
CREATE TABLE archivos_pdf_comprobante (
    id_pdf                  VARCHAR(36)              NOT NULL,
    id_comprobante          VARCHAR(36)              NOT NULL,
    nombre_archivo          VARCHAR(255)             NOT NULL,
    ruta_almacenamiento_vps TEXT                     NOT NULL,
    hash_sha256_archivo     VARCHAR(64)              NOT NULL,
    fecha_generacion        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_archivos_pdf_comprobante PRIMARY KEY (id_pdf),
    CONSTRAINT uq_archivos_comprobante UNIQUE (id_comprobante),
    CONSTRAINT fk_archivos_comprobante FOREIGN KEY (id_comprobante) REFERENCES comprobantes_pago (id_comprobante) ON DELETE CASCADE
);

CREATE INDEX idx_archivos_comprobante ON archivos_pdf_comprobante (id_comprobante);
