#!/usr/bin/env bash
# =============================================================================
# SGL-DQR · Copia de seguridad de la base de datos PostgreSQL.
#
# Genera /var/backups/sgl-dqr/marvisur_db_AAAAMMDD_HHMM.sql.gz y borra las copias
# con más de DIAS_RETENCION días. instalar.sh lo programa todos los días a las 02:30
# (/etc/cron.d/sgl-dqr-respaldo). También se puede ejecutar a mano:
#   sudo bash deploy/ubuntu/respaldo-db.sh
#
# Restaurar una copia (borra los datos actuales de la base):
#   gunzip -c /var/backups/sgl-dqr/<archivo>.sql.gz | sudo -u postgres psql -d marvisur_db
# =============================================================================
set -euo pipefail

DB_NOMBRE="${DB_NOMBRE:-marvisur_db}"
DIR_RESPALDOS="${DIR_RESPALDOS:-/var/backups/sgl-dqr}"
DIAS_RETENCION="${DIAS_RETENCION:-14}"

[ "$(id -u)" -eq 0 ] || { echo "Ejecuta con sudo: sudo bash $0" >&2; exit 1; }

# Carpeta solo para postgres (700): las copias contienen datos personales de clientes.
install -d -m 700 -o postgres -g postgres "$DIR_RESPALDOS"

ARCHIVO="$DIR_RESPALDOS/${DB_NOMBRE}_$(date +%Y%m%d_%H%M).sql.gz"
(cd /tmp && sudo -u postgres pg_dump --clean --if-exists "$DB_NOMBRE") | gzip -9 > "$ARCHIVO.tmp"
mv "$ARCHIVO.tmp" "$ARCHIVO"
chown postgres:postgres "$ARCHIVO"
chmod 600 "$ARCHIVO"

find "$DIR_RESPALDOS" -maxdepth 1 -name "${DB_NOMBRE}_*.sql.gz" -mtime +"$DIAS_RETENCION" -delete

echo "Copia creada: $ARCHIVO ($(du -h "$ARCHIVO" | cut -f1))"
