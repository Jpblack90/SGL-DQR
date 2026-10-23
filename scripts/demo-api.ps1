<#
.SYNOPSIS
  Arranca marvisur-api en perfil dev con soporte para detección de IP LAN y portal web.
.DESCRIPTION
  Acepta -PortalHost <IP> que fija la variable de entorno APP_PORTAL_URL_BASE como
  http://<IP>:5173 (según .env.example). Si no se pasa, detecta la IP LAN automáticamente.
.PARAMETER PortalHost
  IP o nombre de host donde se ejecuta el portal web (por defecto detecta la IP LAN).
.PARAMETER Background
  Si se especifica, arranca el proceso en segundo plano y guarda el PID en scripts/.api.pid.
#>
[CmdletBinding()]
param(
    [string]$PortalHost = "",
    [switch]$Background
)

$ErrorActionPreference = "Stop"

$Raiz = Split-Path -Parent $PSScriptRoot
Set-Location $Raiz

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " [DEMO-API] Arrancando marvisur-api en perfil dev" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Determinar PortalHost
if ([string]::IsNullOrWhiteSpace($PortalHost)) {
    try {
        $ipObj = Get-NetIPAddress -AddressFamily IPv4 | Where-Object {
            $_.InterfaceAlias -notmatch 'vEthernet|Loopback|WSL|Virtual|Hyper-V|docker' -and
            $_.IPAddress -notlike '127.*' -and
            $_.IPAddress -notlike '169.254.*'
        } | Select-Object -First 1

        if ($ipObj -and $ipObj.IPAddress) {
            $PortalHost = $ipObj.IPAddress
            Write-Host "--> IP LAN detectada automaticamente para el portal: $PortalHost" -ForegroundColor Green
        } else {
            $PortalHost = "localhost"
            Write-Host "--> No se detecto IP LAN externa; usando: localhost" -ForegroundColor Yellow
        }
    } catch {
        $PortalHost = "localhost"
        Write-Host "--> Usando: localhost ($($_.Exception.Message))" -ForegroundColor Yellow
    }
} else {
    Write-Host "--> PortalHost configurado manualmente: $PortalHost" -ForegroundColor Green
}

$PortalUrl = "http://${PortalHost}:5173"
Write-Host "--> APP_PORTAL_URL_BASE configurada a: $PortalUrl" -ForegroundColor Cyan

# Configurar variables de entorno según .env.example y application-dev.properties
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:APP_PORTAL_URL_BASE = $PortalUrl

# Guardar PID en scripts/.api.pid si arranca en background o registrar PID actual
$PidFile = Join-Path $PSScriptRoot ".api.pid"

if ($Background) {
    Write-Host "--> Iniciando API en segundo plano..." -ForegroundColor Yellow
    $mvnArgs = "spring-boot:run", "-pl", ":marvisur-api", "-Dspring-boot.run.profiles=dev", "-Dspring-boot.run.jvmArguments=-Dapp.portal.url-base=$PortalUrl"
    $proc = Start-Process -FilePath "mvn" -ArgumentList $mvnArgs -WorkingDirectory $Raiz -PassThru -WindowStyle Hidden
    $proc.Id | Out-File -FilePath $PidFile -Encoding ascii -Force
    Write-Host "[OK] Proceso iniciado con PID $($proc.Id). PID guardado en $PidFile" -ForegroundColor Green
    Write-Host "     Verifica disponibilidad en http://localhost:8080/actuator/health" -ForegroundColor Green
} else {
    $PID | Out-File -FilePath $PidFile -Encoding ascii -Force
    Write-Host "--> PID del lanzador ($PID) guardado en $PidFile" -ForegroundColor Gray
    Write-Host "--> Iniciando Spring Boot en consola interactiva (Ctrl+C para detener)...`n" -ForegroundColor Yellow

    mvn spring-boot:run -pl :marvisur-api -Dspring-boot.run.profiles=dev "-Dspring-boot.run.jvmArguments=-Dapp.portal.url-base=$PortalUrl"
}
