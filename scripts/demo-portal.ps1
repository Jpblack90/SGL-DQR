<#
.SYNOPSIS
  Arranca el servidor de desarrollo del portal web con exposición en la red local.
.DESCRIPTION
  Ejecuta npm run dev -- --host en la carpeta frontend-marvisur/portal-web.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

$Raiz = Split-Path -Parent $PSScriptRoot
$PortalDir = Join-Path $Raiz "frontend-marvisur\portal-web"

if (-not (Test-Path $PortalDir)) {
    Write-Error "No se encontro el directorio del portal web en: $PortalDir"
}

Set-Location $PortalDir

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " [DEMO-PORTAL] Iniciando Portal Web en modo host" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Directorio: $PortalDir" -ForegroundColor Gray
Write-Host "--> Ejecutando: npm run dev -- --host`n" -ForegroundColor Yellow

npm run dev -- --host
