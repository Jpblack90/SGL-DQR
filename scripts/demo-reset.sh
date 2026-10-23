#!/usr/bin/env bash
# ==============================================================================
# demo-reset.sh — Reinicia el contenedor PostgreSQL y limpia volúmenes para demo
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

echo "=========================================================="
echo " [DEMO-RESET] Reiniciando entorno PostgreSQL y datos demo"
echo "=========================================================="

echo "--> Deteniendo y eliminando volúmenes (docker compose down -v)..."
docker compose down -v

echo "--> Levantando contenedor postgres (docker compose up -d)..."
docker compose up -d

echo "--> Esperando a que PostgreSQL esté listo..."
intentos=0
max_intentos=30
listo=0

while [ $intentos -lt $max_intentos ]; do
    intentos=$((intentos + 1))
    sleep 1
    if docker compose exec -T postgres pg_isready -U marvisur -d marvisur_db >/dev/null 2>&1; then
        listo=1
        break
    fi
    echo "    Intento $intentos/$max_intentos: esperando a PostgreSQL..."
done

if [ $listo -eq 1 ]; then
    echo "[OK] PostgreSQL listo en localhost:5433 (marvisur_db)."
else
    echo "ERROR: PostgreSQL no respondió a tiempo." >&2
    exit 1
fi
