#!/usr/bin/env bash
# =============================================================================
# SGL-DQR · Modo ahorro en Azure (Azure Cloud Shell, Bash)
#
#   bash deploy/azure/ahorro.sh pausar     # tras una exposición: casi 0 USD/mes
#   bash deploy/azure/ahorro.sh reanudar   # el día antes de volver a exponer
#   bash deploy/azure/ahorro.sh estado     # ver cómo está todo
#
# pausar:   detiene la API, baja el plan de App Service de B1 a F1 (gratis) y
#           detiene PostgreSQL. Solo se sigue cobrando el disco de la base.
# reanudar: vuelve a B1 con «Always On», enciende la base y la API, y espera a
#           que la API responda. Los datos, la configuración y las direcciones
#           no cambian.
#
# OJO: Azure vuelve a encender sola una base PostgreSQL detenida a los 7 días.
# Mientras dure la pausa, ejecutar «estado» (o «pausar») una vez por semana.
# =============================================================================
set -euo pipefail

SUFIJO="${SUFIJO:-318404}"
GRUPO="${GRUPO:-sgl-dqr-rg}"
PLAN="sgl-dqr-plan"
API="sgl-dqr-api-$SUFIJO"
PG="sgl-dqr-pg-$SUFIJO"

paso() { printf '\n\033[1;32m==> %s\033[0m\n' "$1"; }
aviso() { printf '\033[1;33m%s\033[0m\n' "$1"; }

estado_pg()  { az postgres flexible-server show -g "$GRUPO" -n "$PG" --query state -o tsv; }
estado_api() { az webapp show -g "$GRUPO" -n "$API" --query state -o tsv; }
sku_plan()   { az appservice plan show -g "$GRUPO" -n "$PLAN" --query sku.name -o tsv; }

mostrar_estado() {
    local pg api sku
    pg="$(estado_pg)"; api="$(estado_api)"; sku="$(sku_plan)"
    printf '\n  Plan de la API:  %s\n  API:             %s\n  Base de datos:   %s\n' "$sku" "$api" "$pg"
    if [ "$sku" = "F1" ] && [ "$pg" = "Ready" ]; then
        aviso "  La base se encendió sola (pasan 7 días detenida). Ejecuta: bash deploy/azure/ahorro.sh pausar"
    fi
}

case "${1:-estado}" in
pausar)
    paso "1/3 Deteniendo la API"
    az webapp stop -g "$GRUPO" -n "$API" -o none
    paso "2/3 Plan de App Service a F1 (gratis)"
    if [ "$(sku_plan)" != "F1" ]; then
        az webapp config set -g "$GRUPO" -n "$API" --always-on false -o none
        az appservice plan update -g "$GRUPO" -n "$PLAN" --sku F1 -o none \
            || aviso "No se pudo bajar a F1 en esta región. La API queda detenida, pero el plan B1 se sigue cobrando."
    fi
    paso "3/3 Deteniendo PostgreSQL"
    case "$(estado_pg)" in
        Stopped|Stopping) echo "Ya estaba detenida." ;;
        *) az postgres flexible-server stop -g "$GRUPO" -n "$PG" -o none ;;
    esac
    mostrar_estado
    aviso "Recuerda: Azure enciende sola la base a los 7 días. Revisa con «estado» cada semana."
    ;;
reanudar)
    paso "1/3 Encendiendo PostgreSQL (tarda unos minutos)"
    case "$(estado_pg)" in
        Ready) echo "Ya estaba encendida." ;;
        *) az postgres flexible-server start -g "$GRUPO" -n "$PG" -o none ;;
    esac
    paso "2/3 Plan de App Service a B1 con Always On"
    [ "$(sku_plan)" = "B1" ] || az appservice plan update -g "$GRUPO" -n "$PLAN" --sku B1 -o none
    az webapp config set -g "$GRUPO" -n "$API" --always-on true -o none
    paso "3/3 Encendiendo la API"
    az webapp start -g "$GRUPO" -n "$API" -o none
    URL_API="https://$(az webapp show -g "$GRUPO" -n "$API" --query defaultHostName -o tsv)"
    printf 'Esperando a que la API responda'
    for _ in $(seq 1 60); do
        curl -fs "$URL_API/actuator/health" >/dev/null 2>&1 && { printf ' listo.\n'; break; }
        printf '.'; sleep 5
    done
    mostrar_estado
    ;;
estado)
    mostrar_estado
    ;;
*)
    echo "Uso: bash deploy/azure/ahorro.sh pausar|reanudar|estado"; exit 1 ;;
esac
