-- ============================================================
-- V1__modulo2_sucursales_flotas_rutas.sql
-- SGL-DQR · Expreso Marvisur
-- MÓDULO 2: Sucursales, Flotas y Rutas (RF-02, RF-05)
--
-- Se ejecuta primero porque 'sucursales' no tiene dependencias de FK
-- y es requerida por 'empleados' (Módulo 1).
-- La FK de programacion_viajes.id_chofer hacia empleados.id_empleado
-- se añade con ALTER TABLE en V2 para resolver la dependencia circular.
-- ============================================================

-- ------------------------------------------------------------
-- 1. Tabla: sucursales
-- ------------------------------------------------------------
CREATE TABLE sucursales (
    id_sucursal     VARCHAR(36)  NOT NULL,
    nombre_agencia  VARCHAR(150) NOT NULL,
    direccion       TEXT,
    ciudad          VARCHAR(100) NOT NULL,
    CONSTRAINT pk_sucursales PRIMARY KEY (id_sucursal)
);

-- ------------------------------------------------------------
-- 2. Tabla: vehiculos
-- ------------------------------------------------------------
CREATE TABLE vehiculos (
    id_vehiculo         VARCHAR(36)   NOT NULL,
    placa               VARCHAR(10)   NOT NULL,
    modelo              VARCHAR(100)  NOT NULL,
    capacidad_carga_kg  NUMERIC(10,2) NOT NULL,
    CONSTRAINT pk_vehiculos PRIMARY KEY (id_vehiculo),
    CONSTRAINT uq_vehiculos_placa UNIQUE (placa)
);

CREATE INDEX idx_vehiculos_placa ON vehiculos (placa);

-- ------------------------------------------------------------
-- 3. Tabla: rutas
-- ------------------------------------------------------------
CREATE TABLE rutas (
    id_ruta             VARCHAR(36)   NOT NULL,
    id_sucursal_origen  VARCHAR(36)   NOT NULL,
    id_sucursal_destino VARCHAR(36)   NOT NULL,
    distancia_km        NUMERIC(10,2) NOT NULL,
    CONSTRAINT pk_rutas PRIMARY KEY (id_ruta),
    CONSTRAINT fk_rutas_origen FOREIGN KEY (id_sucursal_origen) REFERENCES sucursales (id_sucursal),
    CONSTRAINT fk_rutas_destino FOREIGN KEY (id_sucursal_destino) REFERENCES sucursales (id_sucursal)
);

CREATE INDEX idx_rutas_sucursal_origen ON rutas (id_sucursal_origen);
CREATE INDEX idx_rutas_sucursal_destino ON rutas (id_sucursal_destino);

-- ------------------------------------------------------------
-- 4. Tabla: programacion_viajes
-- ------------------------------------------------------------
CREATE TABLE programacion_viajes (
    id_viaje                VARCHAR(36)              NOT NULL,
    id_ruta                 VARCHAR(36)              NOT NULL,
    id_vehiculo             VARCHAR(36)              NOT NULL,
    id_chofer               VARCHAR(36)              NOT NULL,
    fecha_salida            TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_llegada_estimada  TIMESTAMP WITH TIME ZONE NOT NULL,
    estado_viaje            VARCHAR(30)              NOT NULL,
    CONSTRAINT pk_programacion_viajes PRIMARY KEY (id_viaje),
    CONSTRAINT fk_viajes_ruta FOREIGN KEY (id_ruta) REFERENCES rutas (id_ruta),
    CONSTRAINT fk_viajes_vehiculo FOREIGN KEY (id_vehiculo) REFERENCES vehiculos (id_vehiculo),
    CONSTRAINT ck_viajes_estado CHECK (estado_viaje IN ('PROGRAMADO', 'EN_RUTA', 'FINALIZADO'))
);

CREATE INDEX idx_viajes_ruta ON programacion_viajes (id_ruta);
CREATE INDEX idx_viajes_vehiculo ON programacion_viajes (id_vehiculo);
CREATE INDEX idx_viajes_chofer ON programacion_viajes (id_chofer);
