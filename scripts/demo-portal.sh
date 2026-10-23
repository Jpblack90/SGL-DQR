#!/usr/bin/env bash
# ==============================================================================
# demo-portal.sh — Inicia el portal web en modo host (frontend-marvisur/portal-web)
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
PORTAL_DIR="$ROOT_DIR/frontend-marvisur/portal-web"

cd "$PORTAL_DIR"

echo "=========================================================="
echo " [DEMO-PORTAL] Iniciando Portal Web en modo host"
echo "=========================================================="
echo "--> Ejecutando npm run dev -- --host en $PORTAL_DIR"

npm run dev -- --host
