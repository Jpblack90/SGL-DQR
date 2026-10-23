#!/usr/bin/env bash
# ==============================================================================
# demo-api.sh — Arranca marvisur-api en perfil dev con soporte para IP LAN y portal
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

PORTAL_HOST="${1:-}"
BACKGROUND="${BACKGROUND:-false}"

echo "=========================================================="
echo " [DEMO-API] Arrancando marvisur-api en perfil dev"
echo "=========================================================="

if [ -z "$PORTAL_HOST" ]; then
    # Intentar detectar IP LAN
    IP_LAN=$(hostname -I 2>/dev/null | awk '{print $1}' || true)
    if [ -z "$IP_LAN" ]; then
        IP_LAN=$(ip route get 1.1.1.1 2>/dev/null | awk '{print $7}' || true)
    fi
    if [ -n "$IP_LAN" ]; then
        PORTAL_HOST="$IP_LAN"
        echo "--> IP LAN detectada automáticamente para el portal: $PORTAL_HOST"
    else
        PORTAL_HOST="localhost"
        echo "--> Usando: localhost"
    fi
else
    echo "--> PortalHost configurado manualmente: $PORTAL_HOST"
fi

PORTAL_URL="http://${PORTAL_HOST}:5173"
echo "--> APP_PORTAL_URL_BASE configurada a: $PORTAL_URL"

export SPRING_PROFILES_ACTIVE="dev"
export APP_PORTAL_URL_BASE="$PORTAL_URL"

PID_FILE="$SCRIPT_DIR/.api.pid"

if [ "$BACKGROUND" = "true" ]; then
    echo "--> Iniciando API en segundo plano..."
    nohup mvn spring-boot:run -pl :marvisur-api -Dspring-boot.run.profiles=dev \
        "-Dspring-boot.run.jvmArguments=-Dapp.portal.url-base=$PORTAL_URL" > "$SCRIPT_DIR/api.log" 2>&1 &
    API_PID=$!
    echo "$API_PID" > "$PID_FILE"
    echo "[OK] Proceso iniciado con PID $API_PID (logs en scripts/api.log)."
else
    echo "$$" > "$PID_FILE"
    echo "--> Iniciando Spring Boot interactivo (Ctrl+C para detener)..."
    mvn spring-boot:run -pl :marvisur-api -Dspring-boot.run.profiles=dev \
        "-Dspring-boot.run.jvmArguments=-Dapp.portal.url-base=$PORTAL_URL"
fi
