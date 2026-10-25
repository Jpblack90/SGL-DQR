<#
.SYNOPSIS
    Script de empaquetado autocontenido (jlink + jpackage) para la aplicación de escritorio SGL-DQR.
.DESCRIPTION
    Genera un paquete portátil (app-image) con JRE mínimo embebido que incluye JavaFX 21 y módulos requeridos,
    permitiendo la ejecución autónoma en equipos sin Java ni Maven instalados (RNF-04 / Tarea O4).
    Si WiX Toolset está instalado, genera opcionalmente el instalador MSI.
#>
[CmdletBinding()]
param(
    [switch]$SkipMsi = $false
)

$ErrorActionPreference = "Stop"

Write-Host "====================================================" -ForegroundColor Cyan
Write-Host "  SGL-DQR: Empaquetado de Aplicación de Escritorio  " -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan

# 1. Resolución de Rutas Base
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$rootDir = (Resolve-Path "$scriptDir\..").Path
$escritorioDir = Join-Path $rootDir "frontend-marvisur\escritorio"
$targetDir = Join-Path $escritorioDir "target"
$distDir = Join-Path $targetDir "dist"
$runtimeDir = Join-Path $targetDir "runtime"
$libsDir = Join-Path $targetDir "libs"
$appJarsDir = Join-Path $targetDir "app-jars"

Write-Host "Directorio raíz: $rootDir"
Write-Host "Directorio escritorio: $escritorioDir"

# 2. Resolución de Herramientas (Maven, Java, jlink, jpackage, WiX)
# Resolver Maven
$mvnCmd = $null
if (Get-Command "mvn.cmd" -ErrorAction SilentlyContinue) {
    $mvnCmd = (Get-Command "mvn.cmd").Source
} elseif (Get-Command "mvn" -ErrorAction SilentlyContinue) {
    $mvnCmd = (Get-Command "mvn").Source
} elseif ($env:MAVEN_HOME -and (Test-Path "$env:MAVEN_HOME\bin\mvn.cmd")) {
    $mvnCmd = "$env:MAVEN_HOME\bin\mvn.cmd"
} elseif (Test-Path "C:\tools\apache-maven-3.9.9\bin\mvn.cmd") {
    $mvnCmd = "C:\tools\apache-maven-3.9.9\bin\mvn.cmd"
} else {
    throw "No se encontró el ejecutable de Maven (mvn.cmd). Configure PATH o MAVEN_HOME."
}
Write-Host "Maven resuelto: $mvnCmd" -ForegroundColor Green

# Resolver JAVA_HOME y binarios JDK
$javaHome = $env:JAVA_HOME
if (-not $javaHome -or -not (Test-Path "$javaHome\bin\jpackage.exe")) {
    $jpackageFromPath = Get-Command "jpackage.exe" -ErrorAction SilentlyContinue
    if ($jpackageFromPath) {
        $javaHome = (Resolve-Path (Join-Path (Split-Path -Parent $jpackageFromPath.Source) "..")).Path
    } elseif (Test-Path "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin\jpackage.exe") {
        $javaHome = "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
    } else {
        throw "No se encontró jpackage.exe en JAVA_HOME ni en PATH."
    }
}
$env:JAVA_HOME = $javaHome
$jlinkCmd = Join-Path $javaHome "bin\jlink.exe"
$jpackageCmd = Join-Path $javaHome "bin\jpackage.exe"

if (-not (Test-Path $jlinkCmd)) {
    throw "No se encontró jlink.exe en: $jlinkCmd"
}
if (-not (Test-Path $jpackageCmd)) {
    throw "No se encontró jpackage.exe en: $jpackageCmd"
}
Write-Host "JAVA_HOME: $javaHome" -ForegroundColor Green
Write-Host "jlink: $jlinkCmd" -ForegroundColor Green
Write-Host "jpackage: $jpackageCmd" -ForegroundColor Green

# 3. Extraer Versión desde pom.xml
[xml]$pomXml = Get-Content (Join-Path $escritorioDir "pom.xml")
$versionRaw = $pomXml.project.version
if (-not $versionRaw -and $pomXml.project.parent) {
    $versionRaw = $pomXml.project.parent.version
}
# jpackage en Windows requiere formato de versión numérico x.y.z
$appVersion = "0.1.0"
if ($versionRaw -match '^([0-9]+(\.[0-9]+)*)') {
    $appVersion = $matches[1]
    if ($appVersion.Split('.').Count -eq 2) {
        $appVersion = "$appVersion.0"
    }
}
Write-Host "Versión de la aplicación: $appVersion (POM: $versionRaw)" -ForegroundColor Green

# 4. Limpieza de Directorios Previos
if (Test-Path $distDir) {
    Remove-Item $distDir -Recurse -Force
}
if (Test-Path $runtimeDir) {
    Remove-Item $runtimeDir -Recurse -Force
}
if (Test-Path $appJarsDir) {
    Remove-Item $appJarsDir -Recurse -Force
}
if (Test-Path $libsDir) {
    Remove-Item $libsDir -Recurse -Force
}
New-Item -ItemType Directory -Path $distDir -Force | Out-Null
New-Item -ItemType Directory -Path $appJarsDir -Force | Out-Null

# 5. Compilación del JAR y Copia de Dependencias de Ejecución
Write-Host "`n--- [Paso 1/5] Compilando JAR de la aplicación con Maven ---" -ForegroundColor Yellow
$env:MAVEN_OPTS = "-Xmx1g"
& $mvnCmd -pl "frontend-marvisur/escritorio" package "-DskipTests"
if ($LASTEXITCODE -ne 0) {
    throw "Fallo en 'mvn package' del módulo escritorio."
}

Write-Host "`n--- [Paso 2/5] Copiando dependencias de runtime con Maven ---" -ForegroundColor Yellow
& $mvnCmd -pl "frontend-marvisur/escritorio" dependency:copy-dependencies "-DincludeScope=runtime" "-DoutputDirectory=target/libs"
if ($LASTEXITCODE -ne 0) {
    throw "Fallo en 'mvn dependency:copy-dependencies'."
}

# 6. Creación del Runtime Mínimo Embebido con jlink
Write-Host "`n--- [Paso 3/5] Generando JRE mínimo embebido con jlink ---" -ForegroundColor Yellow
$jmodsPath = Join-Path $javaHome "jmods"
$modulePath = "$jmodsPath;$libsDir"

# Módulos requeridos determinados por jdeps y arquitectura:
# JavaFX: base, graphics, controls, fxml
# Java SE: base, desktop (AWT/BufferedImage para ticket térmico), logging, net.http, sql (JDBC), xml, naming, management, jdk.crypto.ec, jdk.unsupported
$modulos = @(
    "java.base",
    "java.desktop",
    "java.logging",
    "java.net.http",
    "java.sql",
    "java.xml",
    "java.naming",
    "java.management",
    "jdk.crypto.ec",
    "jdk.unsupported",
    "javafx.base",
    "javafx.graphics",
    "javafx.controls",
    "javafx.fxml"
) -join ","

Write-Host "Módulos a incluir en runtime: $modulos"
& $jlinkCmd `
    --module-path $modulePath `
    --add-modules $modulos `
    --output $runtimeDir `
    --no-header-files `
    --no-man-pages `
    --strip-debug

if ($LASTEXITCODE -ne 0) {
    throw "Fallo al generar runtime con jlink."
}
Write-Host "Runtime mínimo generado exitosamente en: $runtimeDir" -ForegroundColor Green

# 7. Preparación de JARs de la Aplicación para el Classpath
Write-Host "`n--- [Paso 4/5] Preparando JARs de la aplicación (target/app-jars) ---" -ForegroundColor Yellow
$mainJarFile = Get-ChildItem (Join-Path $targetDir "marvisur-escritorio-*.jar") | Where-Object { $_.Name -notmatch '-tests?\.jar$' } | Select-Object -First 1
if (-not $mainJarFile) {
    throw "No se encontró el JAR principal de marvisur-escritorio en $targetDir."
}
Copy-Item $mainJarFile.FullName -Destination $appJarsDir
Write-Host "JAR principal: $($mainJarFile.Name)"

# Copiar dependencias excluyendo los módulos JavaFX que ya están en el runtime
Get-ChildItem -Path $libsDir -Filter "*.jar" | Where-Object { $_.Name -notmatch '^javafx-' } | ForEach-Object {
    Copy-Item $_.FullName -Destination $appJarsDir
}
Write-Host "JARs copiados al directorio de entrada de jpackage."

# 8. Empaquetado con jpackage (--type app-image)
Write-Host "`n--- [Paso 5/5] Creando paquete portátil con jpackage ---" -ForegroundColor Yellow
$appName = "SGL-DQR"
$mainClass = "pe.marvisur.sgldqr.escritorio.Launcher"

$jpackageArgs = @(
    "--type", "app-image",
    "--name", $appName,
    "--app-version", $appVersion,
    "--dest", $distDir,
    "--input", $appJarsDir,
    "--main-jar", $mainJarFile.Name,
    "--main-class", $mainClass,
    "--runtime-image", $runtimeDir,
    "--java-options", "-Dmarvisur.api.base-url=http://localhost:8080",
    "--java-options", "-Dapp.empaquetada=true"
)

# Si existiese icono en el repositorio, se vincularía
$icoFile = Get-ChildItem -Path $rootDir -Filter "*.ico" -Recurse -File | Select-Object -First 1
if ($icoFile) {
    Write-Host "Ícono detectado: $($icoFile.FullName)"
    $jpackageArgs += @("--icon", $icoFile.FullName)
} else {
    Write-Host "Sin ícono .ico personalizado en repositorio (usando icono de sistema)."
}

& $jpackageCmd @jpackageArgs
if ($LASTEXITCODE -ne 0) {
    throw "Fallo en jpackage --type app-image."
}

# Plantilla de configuración externa junto al ejecutable
$appImageDir = Join-Path $distDir $appName
$configTemplate = Join-Path $appImageDir "escritorio.properties"
$configContent = @"
# ==============================================================================
# Configuración Externa para SGL-DQR (Cliente de Escritorio Expreso Marvisur)
# ==============================================================================
# Modifique este archivo para apuntar a un servidor central o VPS distinto.
# Si este archivo se omite, se utiliza por defecto: http://localhost:8080
#
api.base-url=http://localhost:8080
http.timeout-segundos=2
portal.url-base=http://localhost:5173
login.mostrar-demo=false
"@
Set-Content -Path $configTemplate -Value $configContent -Encoding UTF8

Write-Host "`n>>> Paquete portátil generado exitosamente en: $appImageDir" -ForegroundColor Green

# 9. Verificación de WiX para Instalador MSI
Write-Host "`n--- Verificando disponibilidad de WiX Toolset para generar MSI ---" -ForegroundColor Yellow
$candleCmd = Get-Command "candle.exe" -ErrorAction SilentlyContinue
$wixCmd = Get-Command "wix.exe" -ErrorAction SilentlyContinue

if ($candleCmd -or $wixCmd) {
    Write-Host "WiX Toolset detectado. Generando instalador MSI..." -ForegroundColor Green
    $msiArgs = @(
        "--type", "msi",
        "--name", $appName,
        "--app-version", $appVersion,
        "--dest", $distDir,
        "--app-image", $appImageDir,
        "--win-shortcut",
        "--win-menu",
        "--win-menu-group", "Expreso Marvisur"
    )
    if ($icoFile) {
        $msiArgs += @("--icon", $icoFile.FullName)
    }
    & $jpackageCmd @msiArgs
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Instalador MSI generado exitosamente en: $distDir" -ForegroundColor Green
    } else {
        Write-Warning "No se pudo generar el instalador MSI con jpackage."
    }
} else {
    Write-Host "WiX Toolset (candle.exe / light.exe / wix.exe) NO está instalado en el sistema." -ForegroundColor Yellow
    Write-Host "El paquete autocontenido portátil (app-image) está completo y funcional en target/dist/SGL-DQR."
    Write-Host "Para generar instaladores .msi en Windows:"
    Write-Host "  1. Instale WiX Toolset 3.11+: 'winget install WiX.Toolset' o descargue de https://github.com/wixtoolset/wix3/releases"
    Write-Host "  2. Añada la carpeta bin de WiX a la variable PATH de entorno."
    Write-Host "  3. Vuelva a ejecutar este script."
}

# 10. Resumen de Tamaño y Contenido
$exePath = Join-Path $appImageDir "$appName.exe"
if (Test-Path $exePath) {
    $totalSize = (Get-ChildItem -Path $appImageDir -Recurse | Measure-Object -Property Length -Sum).Sum
    $sizeMB = [math]::Round($totalSize / 1MB, 2)
    Write-Host "`n====================================================" -ForegroundColor Cyan
    Write-Host "  RESUMEN DE EMPAQUETADO SGL-DQR                     " -ForegroundColor Cyan
    Write-Host "====================================================" -ForegroundColor Cyan
    Write-Host "Ejecutable principal: $exePath"
    Write-Host "Tamaño total del paquete: $sizeMB MB"
    Write-Host "Archivos en raíz del paquete:"
    Get-ChildItem -Path $appImageDir | ForEach-Object {
        Write-Host "  - $($_.Name)"
    }
    Write-Host "====================================================" -ForegroundColor Cyan
} else {
    throw "No se encontró el ejecutable principal en: $exePath"
}
