#!/usr/bin/env bash
set -e

# Este script executa apenas na primeira inicialização do container Postgres,
# criando o usuário e o banco de dados dedicado ao Keycloak dentro da mesma instância.

if [ -n "${KC_DB_NAME:-}" ] && [ -n "${KC_DB_USER:-}" ] && [ -n "${KC_DB_PASSWORD:-}" ]; then
  echo "Criando usuário e banco de dados para o Keycloak: $KC_DB_NAME..."
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
      DO \$\$
      BEGIN
          IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '$KC_DB_USER') THEN
              CREATE USER $KC_DB_USER WITH ENCRYPTED PASSWORD '$KC_DB_PASSWORD';
          END IF;
      END
      \$\$;

      SELECT 'CREATE DATABASE $KC_DB_NAME OWNER $KC_DB_USER'
      WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$KC_DB_NAME')\gexec

      GRANT ALL PRIVILEGES ON DATABASE $KC_DB_NAME TO $KC_DB_USER;
EOSQL
  echo "Banco de dados do Keycloak criado com sucesso!"
fi
