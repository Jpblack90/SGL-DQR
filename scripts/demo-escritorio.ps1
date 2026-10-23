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

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " [DEMO-ESCRITORIO] Ejecutando Aplicacion JavaFX (SGL-DQR)" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "--> Conectando a API en: $ApiBaseUrl" -ForegroundColor Green

$mvnArgs = @(
    "javafx:run",
    "-pl", ":marvisur-escritorio",
    "-Dmarvisur.api.base-url=$ApiBaseUrl",
    "-Dapi.base-url=$ApiBaseUrl",
    "-Djavafx.args=--api.base-url=$ApiBaseUrl"
)

mvn @mvnArgs
