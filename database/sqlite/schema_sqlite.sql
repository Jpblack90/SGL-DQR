-- ============================================================
-- schema_sqlite.sql — Esquema SQLite para escritorio offline
-- SGL-DQR · Expreso Marvisur
--
-- Subconjunto operativo del esquema PostgreSQL adaptado
-- a las limitaciones de SQLite:
--   · Sin CHECK de ENUMs (SQLite no los valida nativamente).
--   · Sin TIMESTAMPTZ (se usa TEXT en formato ISO-8601 UTC).
--   · Sin BIGSERIAL (INTEGER autoincrement).
--   · Relaciones con ON DELETE CASCADE para integridad local.
--
-- Uso: el adaptador SQLiteAdapter ejecuta este script al
-- inicializar la base de datos local del escritorio JavaFX.
-- ============================================================

PRAGMA journal_mode = WAL;
PRAGMA foreign_keys = ON;

-- ============================================================
-- 1. AGENCIA (cache local sincronizada desde el servidor)
-- ============================================================
CREATE TABLE IF NOT EXISTS agencia (
    id        TEXT NOT NULL PRIMARY KEY,
    codigo    TEXT NOT NULL UNIQUE,
    nombre    TEXT NOT NULL,
    direccion TEXT NOT NULL,
    ciudad    TEXT NOT NULL,
    activo    INTEGER NOT NULL DEFAULT 1,
    creado_en TEXT NOT NULL DEFAULT (datetime('now'))
);

-- ============================================================
-- 2. USUARIO (cache local — solo los datos necesarios)
-- ============================================================
CREATE TABLE IF NOT EXISTS usuario (
    id               TEXT NOT NULL PRIMARY KEY,
    nombre_completo  TEXT NOT NULL,
    nombre_usuario   TEXT NOT NULL UNIQUE,
    hash_contrasena  TEXT NOT NULL,
    rol              TEXT NOT NULL,
    agencia_id       TEXT NOT NULL,
    activo           INTEGER NOT NULL DEFAULT 1,
    FOREIGN KEY (agencia_id) REFERENCES agencia (id)
);

-- ============================================================
-- 3. ENCOMIENDA (tabla operativa principal del escritorio)
-- ============================================================
CREATE TABLE IF NOT EXISTS encomienda (
    id                   TEXT NOT NULL PRIMARY KEY,
    remitente            TEXT NOT NULL,
    destinatario         TEXT NOT NULL,
    peso_kg              REAL NOT NULL,
    tarifa               REAL NOT NULL,
    estado_pago          TEXT NOT NULL DEFAULT 'PENDIENTE',
    estado_envio         TEXT NOT NULL DEFAULT 'REGISTRADA',
    agencia_origen_id    TEXT NOT NULL,
    agencia_destino_id   TEXT NOT NULL,
    ruta_id              TEXT,
    registrado_por_id    TEXT,
    creado_en            TEXT NOT NULL DEFAULT (datetime('now')),
    actualizado_en       TEXT NOT NULL DEFAULT (datetime('now')),
    sincronizado         INTEGER NOT NULL DEFAULT 0,  -- 0=pendiente, 1=sincronizado
    FOREIGN KEY (agencia_origen_id)  REFERENCES agencia (id),
    FOREIGN KEY (agencia_destino_id) REFERENCES agencia (id),
    FOREIGN KEY (registrado_por_id)  REFERENCES usuario (id)
);

CREATE INDEX IF NOT EXISTS idx_enc_estado_envio ON encomienda (estado_envio);
CREATE INDEX IF NOT EXISTS idx_enc_sincronizado ON encomienda (sincronizado);

-- ============================================================
-- 4. PAR_DOBLE_QR (hashes SHA-256 del par QR)
-- ============================================================
CREATE TABLE IF NOT EXISTS par_doble_qr (
    encomienda_id      TEXT NOT NULL PRIMARY KEY,
    qr1_hash           TEXT NOT NULL UNIQUE,
    qr2_hash           TEXT NOT NULL UNIQUE,
    qr2_estado         TEXT NOT NULL DEFAULT 'ACTIVO',
    fecha_invalidacion TEXT,
    creado_en          TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (encomienda_id) REFERENCES encomienda (id) ON DELETE CASCADE
);

-- ============================================================
-- 5. COLA_SINCRONIZACION
--    Registro de operaciones pendientes de subir al servidor.
-- ============================================================
CREATE TABLE IF NOT EXISTS cola_sincronizacion (
    id            INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    operacion     TEXT NOT NULL,   -- 'CREAR_ENCOMIENDA', 'ACTUALIZAR_ESTADO', 'INVALIDAR_QR2'
    entidad_id    TEXT NOT NULL,
    payload_json  TEXT NOT NULL,
    intentos      INTEGER NOT NULL DEFAULT 0,
    creado_en     TEXT NOT NULL DEFAULT (datetime('now')),
    procesado_en  TEXT
);

CREATE INDEX IF NOT EXISTS idx_cola_procesado ON cola_sincronizacion (procesado_en);
