#!/usr/bin/env bash
# =============================================================================
# SGL-DQR · Expreso Marvisur
# Despliegue en Ubuntu Server (24.04 LTS o superior)
#
# Instala las aplicaciones, crea la base de datos, compila la API y el portal,
# publica el portal con Nginx, deja la API corriendo con pm2 y asigna permisos.
#
# Uso (como usuario normal con sudo, NO como root), desde la raíz del repositorio:
#   bash deploy/ubuntu/instalar.sh
#
# Variables opcionales (se guardan en deploy/ubuntu/api.env la primera vez):
#   DB_CLAVE     Contraseña del usuario de PostgreSQL     (por defecto: marvisur_dev)
#   PORTAL_URL   Dirección con la que se abre el portal   (por defecto: http://localhost:8088)
#
# Se puede ejecutar de nuevo para actualizar: vuelve a compilar y a publicar.
# =============================================================================
set -euo pipefail

DIR_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIR_REPO="$(cd "$DIR_SCRIPT/../.." && pwd)"
DIR_PORTAL="$DIR_REPO/frontend-marvisur/portal-web"
DIR_WEB="/var/www/sgl-dqr"
ARCHIVO_ENV="$DIR_SCRIPT/api.env"
NOMBRE_PM2="sgl-dqr-api"

DB_NOMBRE="marvisur_db"
DB_USUARIO="marvisur"
DB_CLAVE="${DB_CLAVE:-marvisur_dev}"
PORTAL_URL="${PORTAL_URL:-http://localhost:8088}"

paso() { printf '\n\033[1;32m==> %s\033[0m\n' "$1"; }
aviso() { printf '\033[1;33m[aviso] %s\033[0m\n' "$1"; }
error() { printf '\033[1;31m[error] %s\033[0m\n' "$1" >&2; exit 1; }

[ "$(id -u)" -ne 0 ] || error "Ejecuta el script como tu usuario normal (usa sudo internamente), no como root."
[ -f "$DIR_REPO/pom.xml" ] || error "No se encontró el repositorio en $DIR_REPO."

# -----------------------------------------------------------------------------
paso "1/8 Instalando aplicaciones (git, OpenSSH, Java 21, Maven, Node.js, Nginx, PostgreSQL)"
sudo apt-get update
sudo apt-get install -y git openssh-server openjdk-21-jdk maven nodejs npm nginx postgresql curl
if ! command -v pm2 >/dev/null 2>&1; then
    sudo npm install -g pm2
fi
NODE_MAYOR="$(node -p 'process.versions.node.split(".")[0]')"
[ "$NODE_MAYOR" -ge 18 ] || error "Se necesita Node.js 18 o superior (instalado: $(node -v))."

# -----------------------------------------------------------------------------
paso "2/8 Creando la base de datos PostgreSQL ($DB_NOMBRE)"
sudo systemctl enable --now postgresql
psql_admin() { (cd /tmp && sudo -u postgres psql -v ON_ERROR_STOP=1 -tAc "$1"); }
if [ "$(psql_admin "SELECT 1 FROM pg_roles WHERE rolname = '$DB_USUARIO'")" != "1" ]; then
    psql_admin "CREATE USER $DB_USUARIO WITH PASSWORD '$DB_CLAVE'"
fi
if [ "$(psql_admin "SELECT 1 FROM pg_database WHERE datname = '$DB_NOMBRE'")" != "1" ]; then
    psql_admin "CREATE DATABASE $DB_NOMBRE OWNER $DB_USUARIO"
fi
# Las tablas y los datos de demostración los crea la API al arrancar (Flyway).

# -----------------------------------------------------------------------------
paso "3/8 Configuración de la API (deploy/ubuntu/api.env)"
if [ ! -f "$ARCHIVO_ENV" ]; then
    cat > "$ARCHIVO_ENV" <<EOF
# Configuración de la API de SGL-DQR en este servidor. No se sube a git.
SPRING_PROFILES_ACTIVE=dev
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/$DB_NOMBRE
SPRING_DATASOURCE_USERNAME=$DB_USUARIO
SPRING_DATASOURCE_PASSWORD=$DB_CLAVE
APP_PORTAL_URL_BASE=$PORTAL_URL
CORS_ORIGENES=$PORTAL_URL
EOF
else
    aviso "Ya existe $ARCHIVO_ENV; se conserva sin cambios."
fi
chmod 600 "$ARCHIVO_ENV"

# -----------------------------------------------------------------------------
paso "4/8 Compilando la API con Maven (la primera vez tarda de 5 a 10 minutos)"
(cd "$DIR_REPO" && mvn -B -q -pl backend-marvisur/marvisur-api -am package -DskipTests)
ls "$DIR_REPO"/backend-marvisur/marvisur-api/target/marvisur-api-*-exec.jar >/dev/null \
    || error "No se generó el archivo marvisur-api-*-exec.jar."

# -----------------------------------------------------------------------------
paso "5/8 Compilando el portal web con npm"
(cd "$DIR_PORTAL" && npm ci && npm run build)
[ -f "$DIR_PORTAL/dist/index.html" ] || error "No se generó la carpeta dist del portal."

# -----------------------------------------------------------------------------
paso "6/8 Publicando el portal con Nginx ($DIR_WEB)"
sudo mkdir -p "$DIR_WEB"
sudo find "$DIR_WEB" -mindepth 1 -delete
sudo cp -r "$DIR_PORTAL"/dist/. "$DIR_WEB"/
sudo cp "$DIR_SCRIPT/nginx-sgl-dqr.conf" /etc/nginx/sites-available/sgl-dqr
sudo ln -sf /etc/nginx/sites-available/sgl-dqr /etc/nginx/sites-enabled/sgl-dqr
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl enable nginx
sudo systemctl reload nginx

# -----------------------------------------------------------------------------
paso "7/8 Asignando dueños y permisos"
# Proyecto: dueño el usuario que despliega; carpetas 755 y scripts ejecutables.
chmod 755 "$DIR_SCRIPT/iniciar-api.sh" "$DIR_SCRIPT/instalar.sh"
# Portal publicado: dueño www-data (usuario de Nginx); carpetas 755 y archivos 644.
sudo chown -R www-data:www-data "$DIR_WEB"
sudo find "$DIR_WEB" -type d -exec chmod 755 {} \;
sudo find "$DIR_WEB" -type f -exec chmod 644 {} \;
# api.env tiene la contraseña de la base: solo el dueño lo lee (600).

# -----------------------------------------------------------------------------
paso "8/8 Iniciando la API con pm2 y activando el arranque automático"
pm2 delete "$NOMBRE_PM2" >/dev/null 2>&1 || true
pm2 start "$DIR_SCRIPT/iniciar-api.sh" --name "$NOMBRE_PM2" --interpreter bash
pm2 save
sudo env PATH="$PATH" "$(command -v pm2)" startup systemd -u "$USER" --hp "$HOME" >/dev/null

printf 'Esperando a que la API responda'
for _ in $(seq 1 60); do
    if curl -fs http://localhost:8080/actuator/health >/dev/null 2>&1; then
        printf ' listo.\n'
        break
    fi
    printf '.'
    sleep 3
done
curl -fs http://localhost:8080/actuator/health >/dev/null 2>&1 \
    || aviso "La API aún no responde. Revisa: pm2 logs $NOMBRE_PM2 --lines 80"

cat <<EOF

=============================================================
 SGL-DQR desplegado
=============================================================
 Portal (desde el servidor):  http://localhost
 Portal (desde Windows):      $PORTAL_URL   (con reenvío 8088 -> 80)
 Salud de la API:             curl http://localhost:8080/actuator/health
 Estado de la API:            pm2 status
 Permisos del portal:         ls -l $DIR_WEB
=============================================================
EOF
