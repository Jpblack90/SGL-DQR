-- ============================================================
-- V8__indice_hash_qr2.sql
-- SGL-DQR · Expreso Marvisur
-- Índice sobre parejas_doble_qr(hash_qr2_llave_digital) para
-- optimizar la búsqueda de la Billetera Digital (RF-09).
-- ============================================================

CREATE INDEX idx_parejas_hash_qr2 ON parejas_doble_qr (hash_qr2_llave_digital);
