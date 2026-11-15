#!/usr/bin/env bash
# ALT-30: aplica os papéis de banco com privilégio mínimo (ver db-privilegio-minimo.sql).
# Executar no servidor, em janela de manutenção, DEPOIS de um backup (scripts/backup.sh).
# Idempotente. Depois de executar, defina no .env DB_APP_USER/DB_APP_PASSWORD e
# DB_OWNER_USER/DB_OWNER_PASSWORD (já usados aqui) e reinicie a API (docker compose up -d api).
set -euo pipefail
cd /srv/taskflow

if [ -f .env ]; then
  set -a; source .env; set +a
fi

: "${DB_USER:?defina DB_USER (superusuário atual)}" "${DB_NAME:?defina DB_NAME}"
: "${KC_DB_NAME:?defina KC_DB_NAME}" "${KC_DB_USER:?defina KC_DB_USER}"
: "${DB_OWNER_USER:?defina DB_OWNER_USER no .env}" "${DB_OWNER_PASSWORD:?defina DB_OWNER_PASSWORD no .env}"
: "${DB_APP_USER:?defina DB_APP_USER no .env}" "${DB_APP_PASSWORD:?defina DB_APP_PASSWORD no .env}"

C="docker compose --env-file .env --env-file tags.env"

echo "Aplicando privilégio mínimo no banco $DB_NAME (dono: $DB_OWNER_USER, API: $DB_APP_USER)..."
$C exec -T db psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_NAME" \
  -v app_db="$DB_NAME" -v kc_db="$KC_DB_NAME" -v kc_user="$KC_DB_USER" \
  -v owner_user="$DB_OWNER_USER" -v owner_pass="$DB_OWNER_PASSWORD" \
  -v app_user="$DB_APP_USER" -v app_pass="$DB_APP_PASSWORD" \
  -f - < scripts/db-privilegio-minimo.sql

echo "Concluído. Reinicie a API para usar os novos papéis: $C up -d api"
