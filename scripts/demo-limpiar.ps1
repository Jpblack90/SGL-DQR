<#
.SYNOPSIS
  Deja la base local (Docker) sin encomiendas para una demostración en vivo.
.DESCRIPTION
  Ejecuta database/postgres/utilidades/limpiar-movimientos.sql en el contenedor
  postgres de docker compose. Borra encomiendas, clientes, Doble QR, tracking,
  comprobantes y pre-registros; conserva sucursales, rutas, flota, viajes y los
  usuarios para iniciar sesión.

  Orden: .\scripts\demo-reset.ps1, luego .\scripts\demo-api.ps1 (aplica las
  migraciones y el seed) y por último este script.
#>
[CmdletBinding()]
param(
    [switch]$SinConfirmar
)

$ErrorActionPreference = "Stop"
$Raiz = Split-Path -Parent $PSScriptRoot
Set-Location $Raiz
$Sql = Join-Path $Raiz "database\postgres\utilidades\limpiar-movimientos.sql"

if (-not $SinConfirmar) {
    Write-Host "Se borraran TODAS las encomiendas y clientes de la base local." -ForegroundColor Yellow
    $r = Read-Host "Escribe BORRAR para continuar"
    if ($r -ne "BORRAR") { Write-Host "Cancelado."; exit 0 }
}

# psql lee el script por la entrada estándar: enviarlo en UTF-8 para conservar las tildes.
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
Get-Content -Raw -Encoding UTF8 $Sql | docker compose exec -T postgres psql -U marvisur -d marvisur_db -v ON_ERROR_STOP=1
if ($LASTEXITCODE -ne 0) { Write-Error "No se pudo limpiar la base. ¿Esta corriendo demo-reset.ps1 y la API ya inicio una vez?" }

Write-Host "`n[OK] Base local sin encomiendas: todo lo que registren se crea en vivo." -ForegroundColor Green
