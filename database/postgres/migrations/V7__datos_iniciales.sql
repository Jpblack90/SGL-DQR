-- ============================================================
-- V7__datos_iniciales.sql
-- SGL-DQR · Expreso Marvisur
-- Datos iniciales del sistema:
--   1. Los 4 roles del Diccionario (Administrador, Almacenero, Recepcionista, Chofer).
--   2. 1 sucursal de ejemplo (Agencia Lima Central).
--   3. 1 usuario administrador inicial (TN3).
--
-- NOTA DE SEGURIDAD:
-- El password_hash fue generado con BCrypt (cost factor 12) usando la librería bcrypt.
-- La contraseña temporal asociada se documenta exclusivamente en database/postgres/README.md.
-- ============================================================

-- ------------------------------------------------------------
-- 1. Inserción de los 4 roles del Diccionario
-- ------------------------------------------------------------
INSERT INTO roles (id_rol, nombre_rol, descripcion) VALUES
    ('ROL-TN3-ADMIN',      'Administrador', 'Acceso total y configuración de personal, auditoría y control de fraude (TN3)'),
    ('ROL-TN2-ALMACEN',    'Almacenero',   'Recepción, validación cruzada y despacho de paquetes en almacén (TN2)'),
    ('ROL-TN1-RECEPCION',  'Recepcionista', 'Registro de encomiendas, cobro y emisión de QR1 impreso (TN1)'),
    ('ROL-TN4-CHOFER',     'Chofer',        'Transporte de flota y reporte de estado de ruta en viaje (TN4)');

-- ------------------------------------------------------------
-- 2. Inserción de 1 sucursal de ejemplo
-- ------------------------------------------------------------
INSERT INTO sucursales (id_sucursal, nombre_agencia, direccion, ciudad) VALUES
    ('SUC-LIM-01', 'Agencia Lima Central', 'Av. Grau 450, Cercado de Lima', 'Lima');

-- ------------------------------------------------------------
-- 3. Inserción de 1 usuario administrador (TN3)
-- ------------------------------------------------------------
INSERT INTO empleados (
    id_empleado,
    id_rol,
    id_sucursal,
    dni,
    nombres,
    apellidos,
    correo,
    password_hash,
    estado
) VALUES (
    'EMP-ADMIN-01',
    'ROL-TN3-ADMIN',
    'SUC-LIM-01',
    '00000001',
    'Administrador',
    'Sistema Marvisur',
    'admin@marvisur.pe',
    '$2b$12$goz1tdfrnGCvrV/InZEN3OZS.xvzWnlozTQf2LUs8LoZx9uvYjzEW',
    TRUE
);
