#!/usr/bin/env bash
set -euo pipefail
cd /srv/taskflow

if [ -f .env ]; then
  set -a; source .env; set +a
fi

KC="docker compose --env-file .env --env-file tags.env exec -T keycloak /opt/keycloak/bin/kcadm.sh"
REALM=taskflow-realm

echo "Conectando ao Keycloak Admin CLI..."
$KC config credentials --server http://localhost:8080 --realm master \
  --user "$KC_ADMIN_USER" --password "$KC_ADMIN_PASSWORD"

echo "Ajustando configurações de produção para o Realm $REALM..."
# Remove frontendUrl de dev (permitindo que o KC_HOSTNAME do compose governe os links)
# Ativa proteção contra força bruta, política de senhas fortes e SMTP
$KC update "realms/$REALM" \
  -s 'attributes.frontendUrl=' \
  -s bruteForceProtected=true -s failureFactor=5 -s waitIncrementSeconds=60 \
  -s maxFailureWaitSeconds=900 -s permanentLockout=false \
  -s 'passwordPolicy=length(8) and upperCase(1) and lowerCase(1) and digits(1) and notUsername and notEmail' \
  -s "smtpServer.host=$SMTP_HOST" -s "smtpServer.port=$SMTP_PORT" \
  -s smtpServer.starttls=true -s smtpServer.auth=true \
  -s "smtpServer.user=$SMTP_USER" -s "smtpServer.password=$SMTP_PASSWORD" \
  -s "smtpServer.from=$SMTP_FROM"

echo "Ajustando o Client taskflow-frontend para o domínio oficial..."
CID=$($KC get clients -r "$REALM" -q clientId=taskflow-frontend --fields id --format csv --noquotes | tr -d '\r')
$KC update "clients/$CID" -r "$REALM" \
  -s "rootUrl=$FRONTEND_URL" \
  -s "redirectUris=[\"$FRONTEND_URL/*\"]" \
  -s "webOrigins=[\"$FRONTEND_URL\"]" \
  -s "attributes.\"post.logout.redirect.uris\"=$FRONTEND_URL/*" \
  -s directAccessGrantsEnabled=false

echo "Configurações de produção do Keycloak aplicadas com sucesso para $FRONTEND_URL!"
