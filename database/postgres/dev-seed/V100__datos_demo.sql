-- ============================================================
-- V100__datos_demo.sql
-- SGL-DQR · Expreso Marvisur
-- Datos de demostración (Seed) para el perfil DEV
--
-- ADVERTENCIA:
-- Este script es exclusivo para entornos locales y desarrollo (dev).
-- NUNCA debe ejecutarse en producción ni en pruebas unitarias estándar.
-- ============================================================

-- 1. Sucursales adicionales (Arequipa y Huancayo)
INSERT INTO sucursales (id_sucursal, nombre_agencia, direccion, ciudad) VALUES
    ('SUC-AQP-01', 'Agencia Arequipa', 'Calle Jacinto Ibañez 315, Parque Industrial', 'Arequipa'),
    ('SUC-HYO-01', 'Agencia Huancayo', 'Av. Ferrocarril 1020', 'Huancayo')
ON CONFLICT (id_sucursal) DO NOTHING;

-- 2. Vehículos de flota demo
INSERT INTO vehiculos (id_vehiculo, placa, modelo, capacidad_carga_kg) VALUES
    ('VEH-DEMO-01', 'V1A-100', 'Volvo FH 460', 15000.00),
    ('VEH-DEMO-02', 'V2B-200', 'Scania R500', 18000.00)
ON CONFLICT (id_vehiculo) DO NOTHING;

-- 3. Empleados por rol (TN1..TN4) con clave 'Demo1234*'
INSERT INTO empleados (id_empleado, id_rol, id_sucursal, dni, nombres, apellidos, correo, password_hash, estado) VALUES
    ('EMP-DEMO-TN1', 'ROL-TN1-RECEPCION', 'SUC-LIM-01', '10000001', 'Rosa', 'Mendoza', 'recepcionista.demo@marvisur.pe', '$2a$10$NBv6aCS1wzLaCf505fw71ee10LyrAbHwUec.f1eS9CrqAuYEKr0uS', TRUE),
    ('EMP-DEMO-TN2', 'ROL-TN2-ALMACEN',    'SUC-AQP-01', '10000002', 'Carlos', 'Quispe', 'almacenero.demo@marvisur.pe',     '$2a$10$NBv6aCS1wzLaCf505fw71ee10LyrAbHwUec.f1eS9CrqAuYEKr0uS', TRUE),
    ('EMP-DEMO-TN3', 'ROL-TN3-ADMIN',      'SUC-LIM-01', '10000003', 'Mario', 'Visuraga', 'admin.demo@marvisur.pe',          '$2a$10$NBv6aCS1wzLaCf505fw71ee10LyrAbHwUec.f1eS9CrqAuYEKr0uS', TRUE),
    ('EMP-DEMO-TN4', 'ROL-TN4-CHOFER',     'SUC-LIM-01', '10000004', 'Jorge', 'Huamán', 'chofer.demo@marvisur.pe',          '$2a$10$NBv6aCS1wzLaCf505fw71ee10LyrAbHwUec.f1eS9CrqAuYEKr0uS', TRUE)
ON CONFLICT (id_empleado) DO NOTHING;

-- 4. Rutas interprovinciales (Lima <-> Arequipa, Lima <-> Huancayo)
INSERT INTO rutas (id_ruta, id_sucursal_origen, id_sucursal_destino, distancia_km) VALUES
    ('RUT-LIM-AQP', 'SUC-LIM-01', 'SUC-AQP-01', 1010.50),
    ('RUT-LIM-HYO', 'SUC-LIM-01', 'SUC-HYO-01', 305.00),
    ('RUT-AQP-LIM', 'SUC-AQP-01', 'SUC-LIM-01', 1010.50),
    ('RUT-HYO-LIM', 'SUC-HYO-01', 'SUC-LIM-01', 305.00)
ON CONFLICT (id_ruta) DO NOTHING;

-- 5. Programación de viajes demo (uno por ruta)
INSERT INTO programacion_viajes (id_viaje, id_ruta, id_vehiculo, id_chofer, fecha_salida, fecha_llegada_estimada, estado_viaje) VALUES
    ('VIAJE-DEMO-01', 'RUT-LIM-AQP', 'VEH-DEMO-01', 'EMP-DEMO-TN4', NOW() + INTERVAL '1 day', NOW() + INTERVAL '2 day', 'PROGRAMADO'),
    ('VIAJE-DEMO-02', 'RUT-LIM-HYO', 'VEH-DEMO-02', 'EMP-DEMO-TN4', NOW() + INTERVAL '1 day', NOW() + INTERVAL '1 day 8 hour', 'PROGRAMADO'),
    ('VIAJE-DEMO-03', 'RUT-AQP-LIM', 'VEH-DEMO-01', 'EMP-DEMO-TN4', NOW() + INTERVAL '3 day', NOW() + INTERVAL '4 day', 'PROGRAMADO'),
    ('VIAJE-DEMO-04', 'RUT-HYO-LIM', 'VEH-DEMO-02', 'EMP-DEMO-TN4', NOW() + INTERVAL '2 day', NOW() + INTERVAL '2 day 8 hour', 'PROGRAMADO')
ON CONFLICT (id_viaje) DO NOTHING;

-- 6. Clientes demo
INSERT INTO clientes (id_cliente, tipo_documento, numero_documento, nombres_razon_social, telefono, correo) VALUES
    ('CLI-DEMO-01', 'DNI', '45678901', 'Ana María Flores', '987654321', 'ana.flores@demo.com'),
    ('CLI-DEMO-02', 'DNI', '45678902', 'Luis Alberto Morales', '987654322', 'luis.morales@demo.com'),
    ('CLI-DEMO-03', 'RUC', '20601234567', 'Comercializadora Andina S.A.C.', '987654323', 'contacto@andina.demo.com'),
    ('CLI-DEMO-04', 'DNI', '45678904', 'Elena Ramos Castro', '987654324', 'elena.ramos@demo.com')
ON CONFLICT (id_cliente) DO NOTHING;

-- 7. Encomiendas demo (3 estados clave)
-- Encomienda 1: PAGADA en EN_ALMACEN_DESTINO
INSERT INTO encomiendas (
    id_encomienda, codigo_comprobante, id_remitente, id_destinatario,
    id_sucursal_origen, id_sucursal_destino, peso_kg, volumen_m3,
    descripcion_contenido, monto_total, estado_pago, estado_logistico, fecha_registro
) VALUES (
    'UUID-ENC-DEMO-001', 'MV-DEMO-001', 'CLI-DEMO-01', 'CLI-DEMO-02',
    'SUC-LIM-01', 'SUC-AQP-01', 4.500, 0.015,
    'Paquete demo pagado listo para entrega', 16.25, 'PAGADO', 'EN_ALMACEN_DESTINO', NOW() - INTERVAL '2 day'
) ON CONFLICT (id_encomienda) DO NOTHING;

-- Encomienda 2: PENDIENTE (Contra Entrega) en EN_ALMACEN_DESTINO
INSERT INTO encomiendas (
    id_encomienda, codigo_comprobante, id_remitente, id_destinatario,
    id_sucursal_origen, id_sucursal_destino, peso_kg, volumen_m3,
    descripcion_contenido, monto_total, estado_pago, estado_logistico, fecha_registro
) VALUES (
    'UUID-ENC-DEMO-002', 'MV-DEMO-002', 'CLI-DEMO-03', 'CLI-DEMO-04',
    'SUC-LIM-01', 'SUC-AQP-01', 5.000, 0.020,
    'Paquete demo contra entrega pendiente de cobro', 17.50, 'PENDIENTE', 'EN_ALMACEN_DESTINO', NOW() - INTERVAL '2 day'
) ON CONFLICT (id_encomienda) DO NOTHING;

-- Encomienda 3: REGISTRADA en origen
INSERT INTO encomiendas (
    id_encomienda, codigo_comprobante, id_remitente, id_destinatario,
    id_sucursal_origen, id_sucursal_destino, peso_kg, volumen_m3,
    descripcion_contenido, monto_total, estado_pago, estado_logistico, fecha_registro
) VALUES (
    'UUID-ENC-DEMO-003', 'MV-DEMO-003', 'CLI-DEMO-01', 'CLI-DEMO-04',
    'SUC-LIM-01', 'SUC-HYO-01', 2.000, 0.010,
    'Documentos y sobres demo recién registrados', 10.00, 'PAGADO', 'REGISTRADO', NOW() - INTERVAL '1 hour'
) ON CONFLICT (id_encomienda) DO NOTHING;

-- 8. Parejas de Doble QR (QR1 y QR2 con hashes calculados)
INSERT INTO parejas_doble_qr (
    id_pareja_qr, id_encomienda, uuid_qr1_operativo, hash_qr2_llave_digital, fecha_generacion
) VALUES (
    'PAR-DEMO-001', 'UUID-ENC-DEMO-001', 'MARVISUR:QR1:UUID-ENC-DEMO-001',
    '7e62e05647a89c6d03218739d3ae6d620eab27978620e12882a43fe68d3ecab2', NOW() - INTERVAL '2 day'
), (
    'PAR-DEMO-002', 'UUID-ENC-DEMO-002', 'MARVISUR:QR1:UUID-ENC-DEMO-002',
    '5871ddaceca42227833c9d428f56f2225d6d074203f6c33f854f93a6f9b9caf4', NOW() - INTERVAL '2 day'
), (
    'PAR-DEMO-003', 'UUID-ENC-DEMO-003', 'MARVISUR:QR1:UUID-ENC-DEMO-003',
    '4a0ac62303a57ec9a47bcd87c159b4a16c283812070a4bedfb9456fffe8b0b76', NOW() - INTERVAL '1 hour'
) ON CONFLICT (id_pareja_qr) DO NOTHING;

-- 9. Estatus de tokens QR2
INSERT INTO tokens_qr2_estatus (
    id_token_status, id_pareja_qr, estado_token, fecha_invalidacion, motivo_inactivacion
) VALUES
    ('TOK-DEMO-001', 'PAR-DEMO-001', 'ACTIVO', NULL, NULL),
    ('TOK-DEMO-002', 'PAR-DEMO-002', 'ACTIVO', NULL, NULL),
    ('TOK-DEMO-003', 'PAR-DEMO-003', 'ACTIVO', NULL, NULL)
ON CONFLICT (id_token_status) DO NOTHING;

-- 10. Historial de tracking de demostración
INSERT INTO historial_tracking (
    id_tracking, id_encomienda, id_sucursal_actual, estado_evento, observacion, fecha_hora_evento
) VALUES
    ('TRK-DEMO-101', 'UUID-ENC-DEMO-001', 'SUC-LIM-01', 'RECEPCIONADO', 'Recepción en ventanilla Lima Central', NOW() - INTERVAL '2 day'),
    ('TRK-DEMO-102', 'UUID-ENC-DEMO-001', 'SUC-LIM-01', 'EN_TRANSITO', 'Despacho hacia Arequipa en viaje programado', NOW() - INTERVAL '1 day 12 hour'),
    ('TRK-DEMO-103', 'UUID-ENC-DEMO-001', 'SUC-AQP-01', 'EN_ALMACEN_DESTINO', 'Arribo a almacén Arequipa listo para retiro', NOW() - INTERVAL '6 hour'),

    ('TRK-DEMO-201', 'UUID-ENC-DEMO-002', 'SUC-LIM-01', 'RECEPCIONADO', 'Recepción en ventanilla Lima Central', NOW() - INTERVAL '2 day'),
    ('TRK-DEMO-202', 'UUID-ENC-DEMO-002', 'SUC-LIM-01', 'EN_TRANSITO', 'Despacho hacia Arequipa en viaje programado', NOW() - INTERVAL '1 day 12 hour'),
    ('TRK-DEMO-203', 'UUID-ENC-DEMO-002', 'SUC-AQP-01', 'EN_ALMACEN_DESTINO', 'Arribo a almacén Arequipa; cobro pendiente en ventanilla', NOW() - INTERVAL '5 hour'),

    ('TRK-DEMO-301', 'UUID-ENC-DEMO-003', 'SUC-LIM-01', 'RECEPCIONADO', 'Recepción en ventanilla Lima Central', NOW() - INTERVAL '1 hour')
ON CONFLICT (id_tracking) DO NOTHING;

-- 11. Pre-registro Express demo vigente
INSERT INTO pre_registros_express (
    id_preregistro, codigo_express, id_cliente_remitente, id_cliente_destinatario,
    id_sucursal_destino, peso_estimado, monto_cotizado, fecha_creacion, estado
) VALUES (
    'PRE-DEMO-001', 'PRE-DEMO01-PE', 'CLI-DEMO-01', 'CLI-DEMO-02',
    'SUC-AQP-01', 5.000, 17.50, NOW(), 'PENDIENTE_VENTANILLA'
) ON CONFLICT (id_preregistro) DO NOTHING;
