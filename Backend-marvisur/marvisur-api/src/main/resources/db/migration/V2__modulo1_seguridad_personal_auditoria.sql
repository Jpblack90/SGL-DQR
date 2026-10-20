-- ============================================================
-- V2__modulo1_seguridad_personal_auditoria.sql
-- SGL-DQR · Expreso Marvisur
-- MÓDULO 1: Seguridad, Personal y Auditoría (RF-01, RF-03)
--
-- Incluye roles, empleados (con FK a roles y sucursales) y auditoría.
-- Resuelve la dependencia circular añadiendo la FK de
-- programacion_viajes hacia empleados mediante ALTER TABLE.
-- ============================================================

-- ------------------------------------------------------------
-- 1. Tabla: roles
-- ------------------------------------------------------------
CREATE TABLE roles (
    id_rol      VARCHAR(36)  NOT NULL,
    nombre_rol  VARCHAR(50)  NOT NULL,
    descripcion TEXT,
    CONSTRAINT pk_roles PRIMARY KEY (id_rol)
);

-- ------------------------------------------------------------
-- 2. Tabla: empleados
-- ------------------------------------------------------------
CREATE TABLE empleados (
    id_empleado     VARCHAR(36)  NOT NULL,
    id_rol          VARCHAR(36)  NOT NULL,
    id_sucursal     VARCHAR(36)  NOT NULL,
    dni             VARCHAR(8)   NOT NULL,
    nombres         VARCHAR(100) NOT NULL,
    apellidos       VARCHAR(100) NOT NULL,
    correo          VARCHAR(150) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    estado          BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_empleados PRIMARY KEY (id_empleado),
    CONSTRAINT fk_empleados_rol FOREIGN KEY (id_rol) REFERENCES roles (id_rol),
    CONSTRAINT fk_empleados_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales (id_sucursal),
    CONSTRAINT uq_empleados_dni UNIQUE (dni),
    CONSTRAINT uq_empleados_correo UNIQUE (correo)
);

CREATE INDEX idx_empleados_rol ON empleados (id_rol);
CREATE INDEX idx_empleados_sucursal ON empleados (id_sucursal);
CREATE INDEX idx_empleados_dni ON empleados (dni);
CREATE INDEX idx_empleados_correo ON empleados (correo);

-- ------------------------------------------------------------
-- 3. Tabla: auditoria_logs
-- ------------------------------------------------------------
CREATE TABLE auditoria_logs (
    id_log                  VARCHAR(36)              NOT NULL,
    id_empleado             VARCHAR(36),
    accion                  VARCHAR(100)             NOT NULL,
    tabla_afectada          VARCHAR(100)             NOT NULL,
    id_registro_afectado    VARCHAR(36)              NOT NULL,
    fecha_hora              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ip_origen               VARCHAR(45),
    CONSTRAINT pk_auditoria_logs PRIMARY KEY (id_log),
    CONSTRAINT fk_auditoria_empleado FOREIGN KEY (id_empleado) REFERENCES empleados (id_empleado)
);

CREATE INDEX idx_auditoria_empleado ON auditoria_logs (id_empleado);

-- ------------------------------------------------------------
-- 4. Resolución de dependencia circular (programacion_viajes -> empleados)
-- ------------------------------------------------------------
ALTER TABLE programacion_viajes
    ADD CONSTRAINT fk_viajes_chofer
    FOREIGN KEY (id_chofer) REFERENCES empleados (id_empleado);
