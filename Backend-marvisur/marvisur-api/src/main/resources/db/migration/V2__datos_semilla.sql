-- ============================================================
-- V2__datos_semilla.sql
-- SGL-DQR · Expreso Marvisur
-- Datos iniciales: agencias representativas de Marvisur
-- y un usuario administrador de prueba.
--
-- IMPORTANTE: El hash de contraseña es bcrypt de 'admin1234'.
-- Cambiar OBLIGATORIAMENTE en el primer despliegue a produccion.
-- ============================================================

-- ============================================================
-- Agencias principales de Expreso Marvisur (datos referenciales)
-- ============================================================
INSERT INTO agencia (id, codigo, nombre, direccion, ciudad) VALUES
    ('agencia-lima-01',    'LIM-01', 'Agencia Lima Centro',    'Av. Grau 450, Cercado de Lima',       'Lima'),
    ('agencia-arequipa-01','AQP-01', 'Agencia Arequipa',       'Calle Mercaderes 210, Arequipa',      'Arequipa'),
    ('agencia-cusco-01',   'CUS-01', 'Agencia Cusco',          'Av. El Sol 312, Cusco',               'Cusco'),
    ('agencia-ica-01',     'ICA-01', 'Agencia Ica',            'Av. Municipalidad 128, Ica',          'Ica'),
    ('agencia-moquegua-01','MOQ-01', 'Agencia Moquegua',       'Jr. Moquegua 567, Moquegua',         'Moquegua'),
    ('agencia-tacna-01',   'TAC-01', 'Agencia Tacna',          'Av. San Martin 890, Tacna',           'Tacna'),
    ('agencia-puno-01',    'PUN-01', 'Agencia Puno',           'Jr. Lima 234, Puno',                  'Puno');

-- ============================================================
-- Usuario administrador inicial (TN3)
-- Contraseña: admin1234 (bcrypt)
-- CAMBIAR en produccion.
-- ============================================================
INSERT INTO usuario (id, nombre_completo, nombre_usuario, hash_contrasena, rol, agencia_id) VALUES
    ('usuario-admin-01',
     'Administrador Sistema',
     'admin',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewYppjzCdVGXNXUS',
     'TN3_ADMINISTRADOR',
     'agencia-lima-01');

-- Usuario recepcionista Lima para pruebas
INSERT INTO usuario (id, nombre_completo, nombre_usuario, hash_contrasena, rol, agencia_id) VALUES
    ('usuario-recep-01',
     'María Torres Quispe',
     'mtorres',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewYppjzCdVGXNXUS',
     'TN1_RECEPCIONISTA',
     'agencia-lima-01');

-- Usuario almacenero Lima para pruebas
INSERT INTO usuario (id, nombre_completo, nombre_usuario, hash_contrasena, rol, agencia_id) VALUES
    ('usuario-almac-01',
     'Carlos Mamani Quispe',
     'cmamani',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewYppjzCdVGXNXUS',
     'TN2_ALMACENERO',
     'agencia-lima-01');
