#!/usr/bin/env bash
set -euo pipefail
cd /srv/taskflow

if [ -f .env ]; then
  set -a; source .env; set +a
fi

LABEL="${1:-daily}"
TS="$(date +%Y%m%d-%H%M%S)"
DIR=/srv/taskflow/backups
mkdir -p "$DIR"
C="docker compose --env-file .env --env-file tags.env"

dump() {
  local service="$1"
  local user="$2"
  local db="$3"
  local prefix="$4"
  local file="$DIR/${prefix}_${LABEL}_${TS}.dump"

  echo "Executando pg_dump para $service ($db)..."
  $C exec -T "$service" pg_dump -U "$user" -d "$db" -Fc > "$file"

  # Valida se o arquivo de dump foi gerado corretamente
  $C exec -T "$service" pg_restore --list < "$file" > /dev/null
  echo "Dump validado com sucesso: $file"

  if [ -n "${OCI_BACKUP_BUCKET:-}" ] && command -v oci >/dev/null 2>&1; then
    echo "Enviando dump para o bucket OCI ($OCI_BACKUP_BUCKET)..."
    oci os object put --auth instance_principal --bucket-name "$OCI_BACKUP_BUCKET" \
      --file "$file" --name "$(date +%Y/%m)/$(basename "$file")" --no-multipart >/dev/null
    echo "Upload para OCI concluído."
  fi
}

dump db "$DB_USER"    "$DB_NAME"    api
dump db "$KC_DB_USER" "$KC_DB_NAME" keycloak

# Mantém os últimos 7 dias localmente no servidor
find "$DIR" -name '*.dump' -mtime +7 -delete || true
