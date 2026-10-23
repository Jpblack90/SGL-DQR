<#
.SYNOPSIS
  Detiene marvisur-api para simular la escena offline de la demostración.
.DESCRIPTION
  Identifica y termina el proceso de Spring Boot marvisur-api y libera el puerto 8080.
  Permite validar en vivo que la aplicación de escritorio conmuta al modo offline en <= 2 s (RNF-02).
#>
[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

$Raiz = Split-Path -Parent $PSScriptRoot
Set-Location $Raiz

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " [DEMO-PARAR-API] Deteniendo marvisur-api (Escena Offline)" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$detenidos = 0
$PidFile = Join-Path $PSScriptRoot ".api.pid"

# 1. Proceso guardado en .api.pid
if (Test-Path $PidFile) {
    try {
        $pidGuardado = (Get-Content $PidFile -ErrorAction SilentlyContinue | Out-String).Trim()
        if ($pidGuardado -match '^\d+$') {
            $p = Get-Process -Id [int]$pidGuardado -ErrorAction SilentlyContinue
            if ($p) {
                Write-Host "--> Deteniendo proceso PID $pidGuardado desde .api.pid..." -ForegroundColor Yellow
                Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue
                $detenidos++
            }
        }
    } catch {
        # continuar con otros metodos
    }
    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
}

# 2. Proceso escuchando en el puerto 8080
try {
    $conns = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
    foreach ($conn in $conns) {
        $procId = $conn.OwningProcess
        if ($procId -gt 0) {
            Write-Host "--> Deteniendo proceso en puerto 8080 (PID: $procId)..." -ForegroundColor Yellow
            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
            $detenidos++
        }
    }
} catch {
    # continuar
}

# 3. Procesos Java asociados a marvisur-api
try {
    $javaProcs = Get-CimInstance Win32_Process -Filter "CommandLine LIKE '%marvisur-api%'" -ErrorAction SilentlyContinue
    foreach ($j in $javaProcs) {
        Write-Host "--> Deteniendo proceso Java marvisur-api (PID: $($j.ProcessId))..." -ForegroundColor Yellow
        Stop-Process -Id $j.ProcessId -Force -ErrorAction SilentlyContinue
        $detenidos++
    }
} catch {
    # continuar
}

if ($detenidos -gt 0) {
    Write-Host "`n[OK] API detenida exitosamente. El puerto 8080 ha sido liberado." -ForegroundColor Green
    Write-Host "     La aplicacion de escritorio detectara la caida en <= 2 segundos y conmutara a 'Sin conexion'." -ForegroundColor Green
} else {
    Write-Host "`n[INFO] No se encontro ningun proceso activo de la API en el puerto 8080." -ForegroundColor Yellow
}
