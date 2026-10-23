#!/usr/bin/env bash
# ==============================================================================
# demo-escritorio.sh — Ejecuta la app JavaFX con api.base-url por parámetro
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

API_BASE_URL="${1:-http://localhost:8080}"

echo "=========================================================="
echo " [DEMO-ESCRITORIO] Ejecutando Aplicación JavaFX (SGL-DQR)"
echo "=========================================================="
echo "--> Conectando a API en: $API_BASE_URL"

mvn javafx:run -pl :marvisur-escritorio \
    "-Dmarvisur.api.base-url=$API_BASE_URL" \
    "-Dapi.base-url=$API_BASE_URL" \
    "-Djavafx.args=--api.base-url=$API_BASE_URL"
