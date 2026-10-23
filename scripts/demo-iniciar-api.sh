#!/usr/bin/env bash
# ==============================================================================
# demo-iniciar-api.sh — Inicia la API backend (escena de reconexión)
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

PORTAL_HOST="${1:-}"

echo "=========================================================="
echo " [DEMO-INICIAR-API] Reiniciando marvisur-api (Reconexión)"
echo "=========================================================="

BACKGROUND="true" "$SCRIPT_DIR/demo-api.sh" "$PORTAL_HOST"

echo "--> Esperando a que la API responda en http://localhost:8080/actuator/health..."
intentos=0
max_intentos=45
lista=0

while [ $intentos -lt $max_intentos ]; do
    intentos=$((intentos + 1))
    sleep 1
    if curl -s -f http://localhost:8080/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
        lista=1
        break
    fi
    echo "    Intento $intentos/$max_intentos: esperando UP..."
done

if [ $lista -eq 1 ]; then
    echo "[OK] API activa (UP). El cliente de escritorio sincronizará automáticamente la cola local."
else
    echo "[AVISO] La API se inició en segundo plano pero tardó en responder. Revisa scripts/api.log."
fi
