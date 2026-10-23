#!/usr/bin/env bash
# ==============================================================================
# demo-parar-api.sh — Detiene la API (escena offline de la demo)
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="$SCRIPT_DIR/.api.pid"
detenidos=0

echo "=========================================================="
echo " [DEMO-PARAR-API] Deteniendo marvisur-api (Escena Offline)"
echo "=========================================================="

if [ -f "$PID_FILE" ]; then
    SAVED_PID=$(cat "$PID_FILE" | tr -d '[:space:]')
    if [ -n "$SAVED_PID" ] && kill -0 "$SAVED_PID" 2>/dev/null; then
        echo "--> Deteniendo proceso PID $SAVED_PID (desde .api.pid)..."
        kill -9 "$SAVED_PID" 2>/dev/null || true
        detenidos=$((detenidos + 1))
    fi
    rm -f "$PID_FILE"
fi

# Detener procesos en puerto 8080 si quedaron activos
PIDS_PORT=$(lsof -ti:8080 2>/dev/null || true)
if [ -n "$PIDS_PORT" ]; then
    echo "--> Deteniendo procesos en puerto 8080: $PIDS_PORT"
    echo "$PIDS_PORT" | xargs kill -9 2>/dev/null || true
    detenidos=$((detenidos + 1))
fi

if [ $detenidos -gt 0 ]; then
    echo "[OK] API detenida exitosamente. El puerto 8080 ha sido liberado."
    echo "     El escritorio detectará la caída en <= 2 segundos."
else
    echo "[INFO] No se encontró ningún proceso activo de la API en el puerto 8080."
fi
