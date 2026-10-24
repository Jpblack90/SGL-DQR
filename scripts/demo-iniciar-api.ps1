<#
.SYNOPSIS
  Reinicia o inicia la API backend en perfil dev (escena de reconexión).
.DESCRIPTION
  Lanza marvisur-api para simular la recuperación del servicio central,
  provocando que el cliente de escritorio detecte reconexión y sincronice la cola SQLite.
.PARAMETER PortalHost
  IP o nombre de host donde se ejecuta el portal web (por defecto auto-detecta).
#>
[CmdletBinding()]
param(
    [string]$PortalHost = ""
)

$ErrorActionPreference = "Stop"

$Raiz = Split-Path -Parent $PSScriptRoot
Set-Location $Raiz

# Resolver ruta de Maven si no esta en PATH
if (-not (Get-Command "mvn" -ErrorAction SilentlyContinue)) {
    $candidatosMvn = @(
        $env:MAVEN_HOME,
        $env:M2_HOME,
        "C:\tools\apache-maven-3.9.9\bin",
        "C:\ProgramData\chocolatey\bin"
    )
    foreach ($cand in $candidatosMvn) {
        if (-not [string]::IsNullOrWhiteSpace($cand)) {
            $binPath = if (Test-Path (Join-Path $cand "bin\mvn.cmd")) { Join-Path $cand "bin" } elseif (Test-Path (Join-Path $cand "mvn.cmd")) { $cand } else { $null }
            if ($binPath) {
                $env:Path = "$binPath;$env:Path"
                break
            }
        }
    }
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " [DEMO-INICIAR-API] Reiniciando marvisur-api (Reconexion)" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# Lanzar demo-api.ps1 en una nueva ventana para ver los logs en vivo
$scriptApi = Join-Path $PSScriptRoot "demo-api.ps1"
$argsList = @("-NoExit", "-File", $scriptApi)
if (-not [string]::IsNullOrWhiteSpace($PortalHost)) {
    $argsList += @("-PortalHost", $PortalHost)
}

Write-Host "--> Levantando marvisur-api en ventana independiente..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList $argsList

Write-Host "--> Esperando a que la API responda en http://localhost:8080/actuator/health..." -ForegroundColor Yellow
$intentos = 0
$maxIntentos = 45
$lista = $false

while (-not $lista -and $intentos -lt $maxIntentos) {
    $intentos++
    Start-Sleep -Seconds 1
    try {
        $resp = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get -TimeoutSec 2 -ErrorAction Stop
        if ($resp.status -eq "UP") {
            $lista = $true
            break
        }
    } catch {
        Write-Host "    Intento ${intentos}/${maxIntentos}: esperando salud UP de la API..." -ForegroundColor Gray
    }
}

if ($lista) {
    Write-Host "`n[OK] API activa y saludable (UP). El cliente de escritorio detectara 'En linea'" -ForegroundColor Green
    Write-Host "     y procedera a sincronizar automaticamente los registros pendientes." -ForegroundColor Green
} else {
    Write-Host "`n[AVISO] La API fue iniciada pero no respondio UP en $maxIntentos segundos. Revisa la ventana abierta." -ForegroundColor Yellow
}
