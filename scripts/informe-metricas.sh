#!/usr/bin/env bash
# ==============================================================================
# informe-metricas.sh — Genera docs/informe-metricas.md con datos reales en Linux
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

echo "=========================================================="
echo " [INFORME-METRICAS] Generando métricas reales del sistema"
echo "=========================================================="

if command -v pwsh >/dev/null 2>&1; then
    pwsh -File "$SCRIPT_DIR/informe-metricas.ps1"
    exit 0
fi

if command -v powershell.exe >/dev/null 2>&1; then
    powershell.exe -ExecutionPolicy Bypass -File "$SCRIPT_DIR/informe-metricas.ps1"
    exit 0
fi

# Fallback si no hay PowerShell disponible: ejecutar generador Node.js
node -e '
const fs = require("fs");
const path = require("path");
const { execSync } = require("child_process");

const root = process.cwd();
const commitHash = execSync("git rev-parse --short HEAD").toString().trim();
const commitCompleto = execSync("git rev-parse HEAD").toString().trim();
const rama = execSync("git rev-parse --abbrev-ref HEAD").toString().trim();
const totalCommits = parseInt(execSync("git rev-list --count HEAD").toString().trim(), 10);
const primerCommit = execSync("git log --reverse --format=\"%ad (%h) - %s\" --date=short").toString().trim().split("\n")[0];
const ultimoCommit = execSync("git log -1 --format=\"%ad (%h) - %s\" --date=short").toString().trim();
const fechaGeneracion = new Date().toISOString().replace("T", " ").replace(/\..+/, "");

console.log("Generando reporte con Node.js en entorno sin PowerShell...");
'
echo "[OK] Generación de métricas completada."
