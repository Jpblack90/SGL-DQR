#!/usr/bin/env bash
# =============================================================================
# SGL-DQR · Dejar la base de Azure lista para una demostración en vivo
#
#   bash deploy/azure/limpiar-datos.sh
#
# Borra todas las encomiendas, clientes, Doble QR, tracking, comprobantes y
# pre-registros, y conserva el catálogo (sucursales, rutas, flota, viajes) y
# los usuarios para iniciar sesión. Ver database/postgres/utilidades/limpiar-movimientos.sql.
#
# Pide escribir BORRAR para confirmar y la contraseña de la base (no se muestra).
# Abre el cortafuegos de la base solo para la IP de este Cloud Shell y lo vuelve
# a cerrar al terminar.
# =============================================================================
set -euo pipefail

SUFIJO="${SUFIJO:-318404}"
GRUPO="${GRUPO:-sgl-dqr-rg}"
PG="sgl-dqr-pg-$SUFIJO"
DB="marvisur_db"
DB_USUARIO="marvisur"
REGLA="cloudshell-limpieza"
DIR_REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SQL="$DIR_REPO/database/postgres/utilidades/limpiar-movimientos.sql"

if [ "$(az postgres flexible-server show -g "$GRUPO" -n "$PG" --query state -o tsv)" != "Ready" ]; then
    echo "La base está detenida. Primero: bash deploy/azure/ahorro.sh reanudar"; exit 1
fi

printf '\n\033[1;33mSe borrarán TODAS las encomiendas y clientes de %s (no se puede deshacer).\033[0m\n' "$PG"
read -r -p "Escribe BORRAR para continuar: " CONFIRMA
[ "$CONFIRMA" = "BORRAR" ] || { echo "Cancelado."; exit 0; }
read -rs -p "Contraseña del usuario $DB_USUARIO de la base: " PGPASSWORD; echo
export PGPASSWORD

IP="$(curl -fs https://api.ipify.org)"
echo "Abriendo el cortafuegos solo para $IP..."
az postgres flexible-server firewall-rule create -g "$GRUPO" -n "$PG" --rule-name "$REGLA" \
    --start-ip-address "$IP" --end-ip-address "$IP" -o none
trap 'echo "Cerrando el cortafuegos..."; az postgres flexible-server firewall-rule delete -g "$GRUPO" -n "$PG" --rule-name "$REGLA" --yes -o none' EXIT

psql "host=$PG.postgres.database.azure.com port=5432 dbname=$DB user=$DB_USUARIO sslmode=require" \
    -v ON_ERROR_STOP=1 -f "$SQL"

echo
echo "Listo: la base quedó sin encomiendas. Todo lo que registren desde ahora se crea en vivo."
