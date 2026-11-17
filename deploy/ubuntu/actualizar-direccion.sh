#!/usr/bin/env bash
# =============================================================================
# SGL-DQR · Cambia la dirección con la que se abre el portal, sin recompilar.
#
# En Hyper-V (conmutador «Default Switch») la IP de la máquina puede cambiar al
# reiniciar Windows. Los enlaces de la Billetera Digital y el CORS de la API usan
# esa dirección, así que hay que actualizarla en deploy/ubuntu/api.env.
#
# Uso, desde la raíz del repositorio:
#   bash deploy/ubuntu/actualizar-direccion.sh                  # usa la IP actual
#   bash deploy/ubuntu/actualizar-direccion.sh http://mi-dominio  # o una dirección dada
# =============================================================================
set -euo pipefail

DIR_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ARCHIVO_ENV="$DIR_SCRIPT/api.env"
NOMBRE_PM2="sgl-dqr-api"

[ -f "$ARCHIVO_ENV" ] || { echo "No existe $ARCHIVO_ENV: primero ejecuta deploy/ubuntu/instalar.sh" >&2; exit 1; }

IP_ACTUAL="$(hostname -I | awk '{print $1}')"
NUEVA_URL="${1:-http://$IP_ACTUAL}"
ANTERIOR="$(sed -n 's/^APP_PORTAL_URL_BASE=//p' "$ARCHIVO_ENV")"

if [ "$ANTERIOR" = "$NUEVA_URL" ]; then
    echo "La dirección ya es $NUEVA_URL; no hay nada que cambiar."
    exit 0
fi

sed -i -e "s|^APP_PORTAL_URL_BASE=.*|APP_PORTAL_URL_BASE=$NUEVA_URL|" \
       -e "s|^CORS_ORIGENES=.*|CORS_ORIGENES=$NUEVA_URL|" "$ARCHIVO_ENV"
chmod 600 "$ARCHIVO_ENV"
pm2 restart "$NOMBRE_PM2" --update-env >/dev/null

echo "Dirección del portal: $ANTERIOR -> $NUEVA_URL"
echo "API reiniciada. Abre $NUEVA_URL desde el navegador de Windows."
