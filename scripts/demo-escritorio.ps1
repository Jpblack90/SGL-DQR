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

# Resolver Java 21 (JAVA_HOME) si no esta configurado
function Test-Jdk21([string]$dir) {
    if ([string]::IsNullOrWhiteSpace($dir) -or -not (Test-Path (Join-Path $dir "bin\java.exe"))) { return $false }
    $rel = Join-Path $dir "release"
    return (Test-Path $rel) -and ((Get-Content $rel -Raw) -match 'JAVA_VERSION="21')
}
if (-not (Test-Jdk21 $env:JAVA_HOME)) {
    $candidatosJdk = @()
    $cmdJava = Get-Command "java" -ErrorAction SilentlyContinue
    if ($cmdJava) { $candidatosJdk += (Split-Path -Parent (Split-Path -Parent $cmdJava.Source)) }
    foreach ($base in @("$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Java", "$env:ProgramFiles\Microsoft", "$env:ProgramFiles\Zulu", "$env:ProgramFiles\Amazon Corretto", "$env:USERPROFILE\.jdks")) {
        if (Test-Path $base) { $candidatosJdk += (Get-ChildItem $base -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | ForEach-Object FullName) }
    }
    $jdk = $candidatosJdk | Where-Object { Test-Jdk21 $_ } | Select-Object -First 1
    if (-not $jdk) {
        Write-Host "[ERROR] No se encontro Java 21 (JDK). Instalalo con:" -ForegroundColor Red
        Write-Host "        winget install EclipseAdoptium.Temurin.21.JDK" -ForegroundColor Yellow
        Write-Host "        y abre una ventana nueva de PowerShell antes de volver a ejecutar este script." -ForegroundColor Yellow
        exit 1
    }
    $env:JAVA_HOME = $jdk
    $env:Path = "$jdk\bin;$env:Path"
    Write-Host "--> Java 21 encontrado en: $jdk" -ForegroundColor Green
}

# Sin Maven instalado: usar el Maven Wrapper del proyecto (descarga Maven 3.9.9 la primera vez; requiere Java 21)
$MvnExe = if (Get-Command "mvn" -ErrorAction SilentlyContinue) { "mvn" } else { Join-Path $Raiz "mvnw.cmd" }
if ($MvnExe -ne "mvn") {
    Write-Host "--> Maven no esta instalado: se usa el Maven Wrapper del proyecto (mvnw.cmd)" -ForegroundColor Yellow
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
    & $MvnExe @mvnArgs
}
finally {
    if ($habiaJavaToolOptions) {
        $env:JAVA_TOOL_OPTIONS = $prevJavaToolOptions
    } else {
        Remove-Item env:JAVA_TOOL_OPTIONS -ErrorAction SilentlyContinue
    }
}
