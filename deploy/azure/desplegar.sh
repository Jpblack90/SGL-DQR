#!/usr/bin/env bash
# =============================================================================
# SGL-DQR · Despliegue en Microsoft Azure (Azure Cloud Shell, Bash)
#
#   API ........ Azure App Service (Linux, Java 21, plan B1, HTTPS obligatorio)
#   Base ....... Azure Database for PostgreSQL – Servidor flexible (B1ms, v16)
#   Portal ..... Azure Static Web Apps (plan Free, HTTPS y CDN incluidos)
#
# Uso, desde la raíz del repositorio clonado en Cloud Shell:
#   bash deploy/azure/desplegar.sh
#
# Pide la contraseña de la base sin mostrarla. El secreto JWT se genera aquí y
# solo se guarda en la configuración de la API. Se puede volver a ejecutar para
# publicar una versión nueva: los recursos que ya existen se conservan.
#
# Variables opcionales: SUFIJO (por defecto 318404), REGION (mexicocentral),
# GRUPO (sgl-dqr-rg). En Azure for Students solo se permiten algunas regiones
# y el plan B1 no tiene cuota en todas; mexicocentral sí la tuvo (17/11/2026).
# =============================================================================
set -euo pipefail

SUFIJO="${SUFIJO:-318404}"
REGION="${REGION:-mexicocentral}"
GRUPO="${GRUPO:-sgl-dqr-rg}"
PLAN="sgl-dqr-plan"
API="sgl-dqr-api-$SUFIJO"
PG="sgl-dqr-pg-$SUFIJO"
SWA="sgl-dqr-portal"
REGION_SWA="westus2"          # Static Web Apps solo admite algunas regiones
DB="marvisur_db"
DB_USUARIO="marvisur"

DIR_REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
paso() { printf '\n\033[1;32m==> %s\033[0m\n' "$1"; }

# -----------------------------------------------------------------------------
paso "1/7 Java 21 y compilación de la API"
if ! java -version 2>&1 | grep -q '"21'; then
    [ -d "$HOME/jdk21" ] || {
        curl -sL -o /tmp/jdk21.tgz https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse
        mkdir -p "$HOME/jdk21" && tar xzf /tmp/jdk21.tgz -C "$HOME/jdk21" --strip-components=1
    }
    export JAVA_HOME="$HOME/jdk21" PATH="$HOME/jdk21/bin:$PATH"
fi
(cd "$DIR_REPO" && mvn -B -q -pl backend-marvisur/marvisur-api -am package -DskipTests)
JAR="$(ls "$DIR_REPO"/backend-marvisur/marvisur-api/target/marvisur-api-*-exec.jar)"

# -----------------------------------------------------------------------------
paso "2/7 Grupo de recursos y plan de App Service"
az group create -n "$GRUPO" -l "$REGION" --tags proyecto=SGL-DQR -o none
az appservice plan show -g "$GRUPO" -n "$PLAN" -o none 2>/dev/null \
    || az appservice plan create -g "$GRUPO" -n "$PLAN" -l "$REGION" --is-linux --sku B1 -o none

# -----------------------------------------------------------------------------
paso "3/7 Base de datos PostgreSQL ($PG)"
if ! az postgres flexible-server show -g "$GRUPO" -n "$PG" -o none 2>/dev/null; then
    read -rs -p "Contraseña para el usuario $DB_USUARIO de la base (mín. 8, mayúscula, minúscula y número): " DBPASS; echo
    az postgres flexible-server create -g "$GRUPO" -n "$PG" -l "$REGION" \
        --tier Burstable --sku-name Standard_B1ms --storage-size 32 --version 16 \
        --admin-user "$DB_USUARIO" --admin-password "$DBPASS" \
        --public-access 0.0.0.0 --backup-retention 7 --yes -o none
    az postgres flexible-server db create -g "$GRUPO" -s "$PG" -n "$DB" -o none
fi

# -----------------------------------------------------------------------------
paso "4/7 App Service de la API ($API)"
if ! az webapp show -g "$GRUPO" -n "$API" -o none 2>/dev/null; then
    az webapp create -g "$GRUPO" -p "$PLAN" -n "$API" --runtime "JAVA:21-java21" -o none
    : "${DBPASS:?La base ya existía: exporta DBPASS con su contraseña antes de crear la API}"
    az webapp config appsettings set -g "$GRUPO" -n "$API" -o none --settings \
        SPRING_DATASOURCE_URL="jdbc:postgresql://$PG.postgres.database.azure.com:5432/$DB?sslmode=require" \
        SPRING_DATASOURCE_USERNAME="$DB_USUARIO" \
        SPRING_DATASOURCE_PASSWORD="$DBPASS" \
        JWT_SECRET="$(openssl rand -base64 48)"
fi
# Perfil dev = migraciones V1-V8 + catálogo y usuarios de demostración (V100).
# Endurecido para Internet: sin Swagger, log en INFO, límite público 30/min y
# IP real del cliente tras el proxy de Azure.
az webapp config appsettings set -g "$GRUPO" -n "$API" -o none --settings \
    SPRING_PROFILES_ACTIVE=dev \
    SERVER_PORT=80 \
    JAVA_OPTS="-Duser.timezone=America/Lima -Xmx1024m" \
    SPRINGDOC_SWAGGER_UI_ENABLED=false SPRINGDOC_API_DOCS_ENABLED=false \
    LOGGING_LEVEL_PE_MARVISUR=INFO \
    APP_PUBLICO_LIMITE_POR_MINUTO=30 APP_PUBLICO_CONFIAR_PROXY=true \
    MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=never
az webapp update -g "$GRUPO" -n "$API" --https-only true -o none
az webapp config set -g "$GRUPO" -n "$API" -o none \
    --always-on true --min-tls-version 1.2 --ftps-state Disabled --http20-enabled true \
    --generic-configurations '{"healthCheckPath":"/actuator/health"}'

# -----------------------------------------------------------------------------
paso "5/7 Portal web en Static Web Apps ($SWA)"
az staticwebapp show -g "$GRUPO" -n "$SWA" -o none 2>/dev/null \
    || az staticwebapp create -g "$GRUPO" -n "$SWA" -l "$REGION_SWA" --sku Free -o none
URL_PORTAL="https://$(az staticwebapp show -g "$GRUPO" -n "$SWA" --query defaultHostname -o tsv)"
URL_API="https://$(az webapp show -g "$GRUPO" -n "$API" --query defaultHostName -o tsv)"

(
    cd "$DIR_REPO/frontend-marvisur/portal-web"
    npm ci --no-audit --no-fund
    VITE_API_BASE_URL="$URL_API" npm run build
    cp "$DIR_REPO/deploy/azure/staticwebapp.config.json" dist/
    npx -y @azure/static-web-apps-cli@2 deploy ./dist --env production \
        --deployment-token "$(az staticwebapp secrets list -g "$GRUPO" -n "$SWA" --query properties.apiKey -o tsv)"
)

# -----------------------------------------------------------------------------
paso "6/7 Dirección del portal y CORS en la API"
az webapp config appsettings set -g "$GRUPO" -n "$API" -o none --settings \
    APP_PORTAL_URL_BASE="$URL_PORTAL" CORS_ORIGENES="$URL_PORTAL"

# -----------------------------------------------------------------------------
paso "7/7 Publicando la API"
az webapp deploy -g "$GRUPO" -n "$API" --src-path "$JAR" --type jar -o none
printf 'Esperando a que la API responda'
for _ in $(seq 1 60); do
    curl -fs "$URL_API/actuator/health" >/dev/null 2>&1 && { printf ' listo.\n'; break; }
    printf '.'; sleep 5
done

cat <<EOF

=============================================================
 SGL-DQR desplegado en Azure
=============================================================
 Portal web:        $URL_PORTAL
 API:               $URL_API
 Salud de la API:   $URL_API/actuator/health
 Escritorio:        api.base-url=$URL_API en escritorio.properties
 Base de datos:     $PG.postgres.database.azure.com (copias automáticas 7 días)
=============================================================
EOF
