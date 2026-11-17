#!/usr/bin/env bash
# =============================================================================
# SGL-DQR · Arranque de la API en Ubuntu Server.
# pm2 ejecuta este script (ver instalar.sh). Lee la configuración de api.env.
# =============================================================================
set -euo pipefail

DIR_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIR_REPO="$(cd "$DIR_SCRIPT/../.." && pwd)"
ARCHIVO_ENV="${ARCHIVO_ENV:-$DIR_SCRIPT/api.env}"

if [ -f "$ARCHIVO_ENV" ]; then
    set -a
    # shellcheck disable=SC1090
    . "$ARCHIVO_ENV"
    set +a
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"
export SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://localhost:5432/marvisur_db}"
export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-marvisur}"
export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-marvisur_dev}"
export APP_PORTAL_URL_BASE="${APP_PORTAL_URL_BASE:-http://localhost:8088}"
export CORS_ORIGENES="${CORS_ORIGENES:-$APP_PORTAL_URL_BASE}"

JAR="$(find "$DIR_REPO/backend-marvisur/marvisur-api/target" -maxdepth 1 -name 'marvisur-api-*-exec.jar' | head -n 1)"
if [ -z "$JAR" ]; then
    echo "No se encontró marvisur-api-*-exec.jar. Compila primero con deploy/ubuntu/instalar.sh." >&2
    exit 1
fi

exec java -Duser.timezone=America/Lima -jar "$JAR"
