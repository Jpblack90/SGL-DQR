<#
.SYNOPSIS
  Ejecuta la aplicación de escritorio JavaFX con la URL base de la API configurada por parámetro.
.DESCRIPTION
  Lanza el cliente JavaFX pasando la URL base de la API mediante argumentos de sistema y CLI.
.PARAMETER ApiBaseUrl
  URL base de la API backend (por defecto: http://localhost:8080).
#>
[CmdletBinding()]
param(
    [string]$ApiBaseUrl = "http://localhost:8080"
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
Write-Host " [DEMO-ESCRITORIO] Ejecutando Aplicacion JavaFX (SGL-DQR)" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "--> Conectando a API en: $ApiBaseUrl" -ForegroundColor Green

$mvnArgs = @(
    "javafx:run",
    "-pl", ":marvisur-escritorio",
    "-Dmarvisur.api.base-url=$ApiBaseUrl",
    "-Dapi.base-url=$ApiBaseUrl",
    "-Djavafx.args=--api.base-url=$ApiBaseUrl",
    '-Dmarvisur.login.mostrar-demo="true"'
)

$habiaJavaToolOptions = Test-Path env:JAVA_TOOL_OPTIONS
$prevJavaToolOptions = $env:JAVA_TOOL_OPTIONS
$banderaDemo = "-Dmarvisur.login.mostrar-demo=true"

if ($habiaJavaToolOptions -and -not [string]::IsNullOrWhiteSpace($prevJavaToolOptions)) {
    $env:JAVA_TOOL_OPTIONS = "$prevJavaToolOptions $banderaDemo"
} else {
    $env:JAVA_TOOL_OPTIONS = $banderaDemo
}

try {
    mvn @mvnArgs
}
finally {
    if ($habiaJavaToolOptions) {
        $env:JAVA_TOOL_OPTIONS = $prevJavaToolOptions
    } else {
        Remove-Item env:JAVA_TOOL_OPTIONS -ErrorAction SilentlyContinue
    }
}
