<#
.SYNOPSIS
  Reinicia los contenedores de desarrollo con datos semilla limpios.
.DESCRIPTION
  Ejecuta docker compose down -v, docker compose up -d y espera hasta que
  PostgreSQL en el puerto 5433 esté completamente listo y aceptando conexiones.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

$Raiz = Split-Path -Parent $PSScriptRoot
Set-Location $Raiz

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " [DEMO-RESET] Reiniciando entorno PostgreSQL y datos demo" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

Write-Host "`n--> Deteniendo y eliminando volumenes antiguos (docker compose down -v)..." -ForegroundColor Yellow
docker compose down -v

Write-Host "`n--> Levantando contenedor postgres (docker compose up -d)..." -ForegroundColor Yellow
docker compose up -d

Write-Host "`n--> Esperando a que PostgreSQL este listo para recibir conexiones..." -ForegroundColor Yellow
$intentos = 0
$maxIntentos = 30
$listo = $false

while (-not $listo -and $intentos -lt $maxIntentos) {
    $intentos++
    Start-Sleep -Seconds 1
    $salida = docker compose exec -T postgres pg_isready -U marvisur -d marvisur_db 2>&1
    if ($salida -match "accepting connections") {
        $listo = $true
        break
    }
    Write-Host "    Intento $intentos/$maxIntentos: esperando a PostgreSQL..." -ForegroundColor Gray
}

if ($listo) {
    Write-Host "`n[OK] PostgreSQL esta listo en localhost:5433 con la base 'marvisur_db'." -ForegroundColor Green
    Write-Host "     Las migraciones V1-V8 y datos demo V100 se aplicaran al iniciar marvisur-api." -ForegroundColor Green
} else {
    Write-Error "PostgreSQL no respondio a tiempo tras $maxIntentos segundos."
}
