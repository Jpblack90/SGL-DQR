<#
.SYNOPSIS
    Pipeline automatizado de verificacion y cierre de version v0.3.0 de SGL-DQR.
.DESCRIPTION
    Ejecuta en secuencia estricta:
    1. Verificacion de backend (marvisur-core y marvisur-api con Testcontainers PostgreSQL 16).
    2. Pruebas de escritorio JavaFX (zona local y simulacion CI UTC/en_US).
    3. Pruebas unitarias y linter del Portal Web.
    4. Sincronizacion y validacion estricta de contratos OpenAPI 3 (7 rutas nuevas obligatorias).
    5. Generacion del informe exhaustivo de metricas (scripts/informe-metricas.ps1).
    6. Resumen consolidado final.
#>

[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

$swTotal = [System.Diagnostics.Stopwatch]::StartNew()
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = (Resolve-Path (Join-Path $scriptDir "..")).Path
Set-Location $repoRoot

function Format-Duracion([System.Diagnostics.Stopwatch]$sw) {
    return "{0:N2} s" -f ($sw.ElapsedMilliseconds / 1000.0)
}

function Escribir-Paso([string]$numero, [string]$titulo) {
    Write-Host ""
    Write-Host "================================================================================" -ForegroundColor Cyan
    Write-Host " PASO ${numero}: $titulo" -ForegroundColor Cyan
    Write-Host "================================================================================" -ForegroundColor Cyan
}

function Reportar-Fallo([string]$paso, [string]$mensaje, [System.Diagnostics.Stopwatch]$sw) {
    Write-Host ""
    Write-Host "[ERROR] EN PASO ${paso}: tras $(Format-Duracion $sw)" -ForegroundColor Red
    Write-Host "   Detalle: $mensaje" -ForegroundColor Red
    Write-Host "   El pipeline de cierre se ha detenido." -ForegroundColor Red
    exit 1
}

Write-Host "+------------------------------------------------------------------------------+" -ForegroundColor Green
Write-Host "|             SGL-DQR -- PIPELINE AUTOMATIZADO DE CIERRE v0.3.0                |" -ForegroundColor Green
Write-Host "+------------------------------------------------------------------------------+" -ForegroundColor Green
Write-Host "Directorio base: $repoRoot" -ForegroundColor DarkGray
Write-Host "Inicio: $((Get-Date).ToString('yyyy-MM-dd HH:mm:ss'))" -ForegroundColor DarkGray

# -----------------------------------------------------------------------------
# PASO 1: Backend (Core + API con PostgreSQL 16 Testcontainers)
# -----------------------------------------------------------------------------
Escribir-Paso "1/6" "Compilacion y verificacion backend (marvisur-core y marvisur-api)"
$swPaso1 = [System.Diagnostics.Stopwatch]::StartNew()

$origMavenOpts = $null
if (Test-Path "env:MAVEN_OPTS") {
    $origMavenOpts = ${env:MAVEN_OPTS}
}
${env:MAVEN_OPTS} = "-Xmx1g"

try {
    Write-Host "--> Ejecutando: mvn clean verify -f backend-marvisur/pom.xml (MAVEN_OPTS=-Xmx1g)..." -ForegroundColor Yellow
    mvn clean verify -f backend-marvisur/pom.xml
    if ($LASTEXITCODE -ne 0) {
        Reportar-Fallo "1/6" "Fallo la ejecucion de 'mvn clean verify -f backend-marvisur/pom.xml' (codigo de salida $LASTEXITCODE)." $swPaso1
    }
} finally {
    if ($null -ne $origMavenOpts) {
        ${env:MAVEN_OPTS} = $origMavenOpts
    } else {
        Remove-Item -Path "env:MAVEN_OPTS" -ErrorAction SilentlyContinue
    }
}
$swPaso1.Stop()
Write-Host "[OK] Paso 1/6 completado con exito en $(Format-Duracion $swPaso1)." -ForegroundColor Green

# -----------------------------------------------------------------------------
# PASO 2: Pruebas del modulo escritorio (JavaFX) en dos entornos de zona horaria
# -----------------------------------------------------------------------------
Escribir-Paso "2/6" "Pruebas de frontend-marvisur/escritorio (zona local y simulacion CI UTC/en_US)"
$swPaso2 = [System.Diagnostics.Stopwatch]::StartNew()

Write-Host "--> 2a. Ejecutando pruebas en zona horaria local..." -ForegroundColor Yellow
mvn -pl frontend-marvisur/escritorio -am test
if ($LASTEXITCODE -ne 0) {
    Reportar-Fallo "2/6" "Fallo 'mvn -pl frontend-marvisur/escritorio -am test' en zona local (codigo $LASTEXITCODE)." $swPaso2
}

Write-Host "--> 2b. Ejecutando pruebas en simulacion CI (UTC / en_US)..." -ForegroundColor Yellow
$origJavaToolOpts = $null
if (Test-Path "env:JAVA_TOOL_OPTIONS") {
    $origJavaToolOpts = ${env:JAVA_TOOL_OPTIONS}
}

try {
    ${env:JAVA_TOOL_OPTIONS} = "-Duser.timezone=UTC -Duser.language=en -Duser.country=US"
    mvn -pl frontend-marvisur/escritorio -am test
    if ($LASTEXITCODE -ne 0) {
        Reportar-Fallo "2/6" "Fallo 'mvn -pl frontend-marvisur/escritorio -am test' bajo UTC/en_US (codigo $LASTEXITCODE)." $swPaso2
    }
} finally {
    if ($null -ne $origJavaToolOpts) {
        ${env:JAVA_TOOL_OPTIONS} = $origJavaToolOpts
    } else {
        Remove-Item -Path "env:JAVA_TOOL_OPTIONS" -ErrorAction SilentlyContinue
    }
}
$swPaso2.Stop()
Write-Host "[OK] Paso 2/6 completado con exito en $(Format-Duracion $swPaso2)." -ForegroundColor Green

# -----------------------------------------------------------------------------
# PASO 3: Portal Web (React + Vite + Vitest + ESLint)
# -----------------------------------------------------------------------------
Escribir-Paso "3/6" "Pruebas unitarias y analisis estatico del Portal Web"
$swPaso3 = [System.Diagnostics.Stopwatch]::StartNew()

Push-Location (Join-Path $repoRoot "frontend-marvisur/portal-web")
try {
    Write-Host "--> Ejecutando vitest: npm test -- --run..." -ForegroundColor Yellow
    npm test -- --run
    if ($LASTEXITCODE -ne 0) {
        Reportar-Fallo "3/6" "Fallaron las pruebas unitarias de vitest (codigo $LASTEXITCODE)." $swPaso3
    }

    Write-Host "--> Ejecutando linter: npm run lint..." -ForegroundColor Yellow
    npm run lint
    if ($LASTEXITCODE -ne 0) {
        Reportar-Fallo "3/6" "Fallo la validacion de ESLint en portal-web (codigo $LASTEXITCODE)." $swPaso3
    }
} finally {
    Pop-Location
}
$swPaso3.Stop()
Write-Host "[OK] Paso 3/6 completado con exito en $(Format-Duracion $swPaso3)." -ForegroundColor Green

# -----------------------------------------------------------------------------
# PASO 4: Contratos OpenAPI 3 (Copia limpia sin BOM y verificacion de rutas)
# -----------------------------------------------------------------------------
Escribir-Paso "4/6" "Sincronizacion y validacion de especificaciones OpenAPI 3"
$swPaso4 = [System.Diagnostics.Stopwatch]::StartNew()

$targetPub = Join-Path $repoRoot "backend-marvisur/marvisur-api/target/openapi/openapi-publico.json"
$targetInt = Join-Path $repoRoot "backend-marvisur/marvisur-api/target/openapi/openapi-interno.json"
$docsPub = Join-Path $repoRoot "docs/openapi-publico.json"
$docsInt = Join-Path $repoRoot "docs/openapi-interno.json"

if (-not (Test-Path $targetPub)) {
    Reportar-Fallo "4/6" "No se encontro el archivo generado: $targetPub. Asegurate de que OpenApiExportTest haya ejecutado en el Paso 1." $swPaso4
}
if (-not (Test-Path $targetInt)) {
    Reportar-Fallo "4/6" "No se encontro el archivo generado: $targetInt. Asegurate de que OpenApiExportTest haya ejecutado en el Paso 1." $swPaso4
}

Write-Host "--> Copiando contratos OpenAPI a docs/ (bytes exactos UTF-8 sin BOM)..." -ForegroundColor Yellow
[System.IO.File]::Copy($targetPub, $docsPub, $true)
[System.IO.File]::Copy($targetInt, $docsInt, $true)

Write-Host "--> Validando estructura JSON y rutas nuevas requeridas..." -ForegroundColor Yellow
try {
    $jsonPub = [System.IO.File]::ReadAllText($docsPub, [System.Text.Encoding]::UTF8) | ConvertFrom-Json
} catch {
    Reportar-Fallo "4/6" "Error al parsear JSON de ${docsPub}: $($_.Exception.Message)" $swPaso4
}

try {
    $jsonInt = [System.IO.File]::ReadAllText($docsInt, [System.Text.Encoding]::UTF8) | ConvertFrom-Json
} catch {
    Reportar-Fallo "4/6" "Error al parsear JSON de ${docsInt}: $($_.Exception.Message)" $swPaso4
}

$rutasRequeridasPublicas = @(
    "/api/v1/publico/comprobante.pdf"
)

$rutasRequeridasInternas = @(
    "/api/v1/dashboard/resumen",
    "/api/v1/empleados",
    "/api/v1/auditoria",
    "/api/v1/vehiculos",
    "/api/v1/viajes/{id}/encomiendas",
    "/api/v1/encomiendas/{id}/comprobante.pdf"
)

$rutasPubEncontradas = @(($jsonPub.paths | Get-Member -MemberType NoteProperty).Name)
$rutasIntEncontradas = @(($jsonInt.paths | Get-Member -MemberType NoteProperty).Name)

foreach ($r in $rutasRequeridasPublicas) {
    if ($rutasPubEncontradas -notcontains $r) {
        Reportar-Fallo "4/6" "La ruta publica obligatoria '$r' no existe en docs/openapi-publico.json." $swPaso4
    }
    Write-Host "   [OK] Ruta publica verificada: $r" -ForegroundColor Green
}

foreach ($r in $rutasRequeridasInternas) {
    if ($rutasIntEncontradas -notcontains $r) {
        Reportar-Fallo "4/6" "La ruta interna obligatoria '$r' no existe en docs/openapi-interno.json." $swPaso4
    }
    Write-Host "   [OK] Ruta interna verificada: $r" -ForegroundColor Green
}

$swPaso4.Stop()
Write-Host "[OK] Paso 4/6 completado con exito en $(Format-Duracion $swPaso4)." -ForegroundColor Green

# -----------------------------------------------------------------------------
# PASO 5: Generacion del informe exhaustivo de metricas
# -----------------------------------------------------------------------------
Escribir-Paso "5/6" "Generacion de informe tecnico de metricas (scripts/informe-metricas.ps1)"
$swPaso5 = [System.Diagnostics.Stopwatch]::StartNew()

$scriptMetricas = Join-Path $repoRoot "scripts/informe-metricas.ps1"
if (-not (Test-Path $scriptMetricas)) {
    Reportar-Fallo "5/6" "No se encontro el script: $scriptMetricas" $swPaso5
}

Write-Host "--> Ejecutando scripts/informe-metricas.ps1..." -ForegroundColor Yellow
& $scriptMetricas
if ($LASTEXITCODE -ne 0) {
    Reportar-Fallo "5/6" "Fallo la ejecucion de scripts/informe-metricas.ps1 (codigo $LASTEXITCODE)." $swPaso5
}

$swPaso5.Stop()
Write-Host "[OK] Paso 5/6 completado con exito en $(Format-Duracion $swPaso5)." -ForegroundColor Green

# -----------------------------------------------------------------------------
# PASO 6: Resumen consolidado final
# -----------------------------------------------------------------------------
Escribir-Paso "6/6" "Resumen consolidado de version v0.3.0"

function Contar-Surefire([string]$rutaDir) {
    $archivos = Get-ChildItem -Path $rutaDir -Filter "TEST-*.xml" -ErrorAction SilentlyContinue
    $t = 0; $f = 0; $e = 0; $s = 0
    foreach ($arch in $archivos) {
        try {
            $xml = [xml](Get-Content $arch.FullName -Encoding utf8)
            $t += [int]$xml.testsuite.tests
            $f += [int]$xml.testsuite.failures
            $e += [int]$xml.testsuite.errors
            $s += [int]$xml.testsuite.skipped
        } catch {}
    }
    return @{ Total = $t; Fallos = $f; Errores = $e; Omitidas = $s }
}

$coreRes = Contar-Surefire (Join-Path $repoRoot "backend-marvisur/marvisur-core/target/surefire-reports")
$apiRes = Contar-Surefire (Join-Path $repoRoot "backend-marvisur/marvisur-api/target/surefire-reports")
$escRes = Contar-Surefire (Join-Path $repoRoot "frontend-marvisur/escritorio/target/surefire-reports")

$vitestJson = Join-Path $repoRoot "frontend-marvisur/portal-web/vitest-results.json"
$webTot = 0; $webFail = 0; $webSkip = 0
if (Test-Path $vitestJson) {
    try {
        $vData = Get-Content $vitestJson -Raw -Encoding utf8 | ConvertFrom-Json
        $webTot = [int]$vData.numTotalTests
        $webFail = [int]$vData.numFailedTests
        $webSkip = [int]$vData.numPendingTests
    } catch {}
}

function Contar-OperacionesOpenApi([object]$jsonObj) {
    $count = 0
    if ($jsonObj -and $jsonObj.paths) {
        foreach ($p in ($jsonObj.paths | Get-Member -MemberType NoteProperty).Name) {
            foreach ($m in ($jsonObj.paths.$p | Get-Member -MemberType NoteProperty).Name) {
                $count++
            }
        }
    }
    return $count
}

$opsPub = Contar-OperacionesOpenApi $jsonPub
$opsInt = Contar-OperacionesOpenApi $jsonInt
$totOps = $opsPub + $opsInt

$granTotal = $coreRes.Total + $apiRes.Total + $escRes.Total + $webTot
$granFallos = $coreRes.Fallos + $apiRes.Fallos + $escRes.Fallos + $webFail
$granErrores = $coreRes.Errores + $apiRes.Errores + $escRes.Errores
$granOmitidas = $coreRes.Omitidas + $apiRes.Omitidas + $escRes.Omitidas + $webSkip

$swTotal.Stop()
$duracionTotal = Format-Duracion $swTotal

Write-Host ""
Write-Host "+------------------------------------------------------------------------------+" -ForegroundColor Green
Write-Host "|              RESUMEN DE PRUEBAS AUTOMATIZADAS -- VERSION v0.3.0              |" -ForegroundColor Green
Write-Host "+----------------------------+----------+---------+---------+----------+-------+" -ForegroundColor Green
Write-Host ("| {0,-26} | {1,8} | {2,7} | {3,7} | {4,8} | {5,-5} |" -f "Modulo", "Pruebas", "Fallos", "Errores", "Omitidas", "Estado") -ForegroundColor White
Write-Host "+----------------------------+----------+---------+---------+----------+-------+" -ForegroundColor Gray
Write-Host ("| {0,-26} | {1,8} | {2,7} | {3,7} | {4,8} | {5,-5} |" -f "marvisur-core", $coreRes.Total, $coreRes.Fallos, $coreRes.Errores, $coreRes.Omitidas, "VERDE") -ForegroundColor White
Write-Host ("| {0,-26} | {1,8} | {2,7} | {3,7} | {4,8} | {5,-5} |" -f "marvisur-api", $apiRes.Total, $apiRes.Fallos, $apiRes.Errores, $apiRes.Omitidas, "VERDE") -ForegroundColor White
Write-Host ("| {0,-26} | {1,8} | {2,7} | {3,7} | {4,8} | {5,-5} |" -f "marvisur-escritorio", $escRes.Total, $escRes.Fallos, $escRes.Errores, $escRes.Omitidas, "VERDE") -ForegroundColor White
Write-Host ("| {0,-26} | {1,8} | {2,7} | {3,7} | {4,8} | {5,-5} |" -f "portal-web", $webTot, $webFail, 0, $webSkip, "VERDE") -ForegroundColor White
Write-Host "+----------------------------+----------+---------+---------+----------+-------+" -ForegroundColor Gray
Write-Host ("| {0,-26} | {1,8} | {2,7} | {3,7} | {4,8} | {5,-5} |" -f "TOTAL GLOBAL", $granTotal, $granFallos, $granErrores, $granOmitidas, "VERDE") -ForegroundColor Green
Write-Host "+----------------------------+----------+---------+---------+----------+-------+" -ForegroundColor Green

Write-Host ""
Write-Host "--- CONTRATOS OPENAPI 3 ---" -ForegroundColor Cyan
Write-Host "  * docs/openapi-publico.json : $opsPub operaciones" -ForegroundColor White
Write-Host "  * docs/openapi-interno.json : $opsInt operaciones" -ForegroundColor White
Write-Host "  * Total operaciones OpenAPI : $totOps operaciones" -ForegroundColor Green

Write-Host ""
Write-Host "--- DOCUMENTACION GENERADA ---" -ForegroundColor Cyan
Write-Host "  * Informe de metricas: docs/informe-metricas.md" -ForegroundColor White
Write-Host "  * Duracion total del pipeline de cierre: $duracionTotal" -ForegroundColor Green
Write-Host ""
