-- ============================================================
-- limpiar-movimientos.sql
-- SGL-DQR · Expreso Marvisur
--
-- Deja la base lista para una demostración en vivo: borra TODOS los
-- movimientos (encomiendas, Doble QR, tracking, comprobantes, clientes,
-- pre-registros, validaciones, despachos, incidencias y auditoría) y
-- conserva el catálogo con el que se trabaja: sucursales, flota, rutas,
-- viajes programados, roles y empleados (los usuarios para iniciar sesión).
--
-- Los clientes se vuelven a crear solos al registrar la primera encomienda
-- con su DNI o RUC. No toca el historial de Flyway, así que el seed V100 no
-- se vuelve a cargar.
--
-- ¡Borrado definitivo! Solo para entornos de demostración.
-- ============================================================
\set ON_ERROR_STOP on

\echo 'Antes de limpiar:'
SELECT (SELECT count(*) FROM encomiendas)           AS encomiendas,
       (SELECT count(*) FROM clientes)              AS clientes,
       (SELECT count(*) FROM pre_registros_express) AS pre_registros,
       (SELECT count(*) FROM auditoria_logs)        AS auditoria;

BEGIN;
TRUNCATE TABLE
    validaciones_cruzadas_logs,
    tokens_qr2_estatus,
    parejas_doble_qr,
    archivos_pdf_comprobante,
    comprobantes_pago,
    historial_tracking,
    despachos_lote_detalles,
    incidencias_emergencia,
    encomiendas,
    pre_registros_express,
    clientes,
    auditoria_logs;
COMMIT;

\echo 'Después de limpiar (se conserva el catálogo):'
SELECT (SELECT count(*) FROM encomiendas)         AS encomiendas,
       (SELECT count(*) FROM clientes)            AS clientes,
       (SELECT count(*) FROM sucursales)          AS sucursales,
       (SELECT count(*) FROM rutas)               AS rutas,
       (SELECT count(*) FROM vehiculos)           AS vehiculos,
       (SELECT count(*) FROM empleados)           AS empleados;
