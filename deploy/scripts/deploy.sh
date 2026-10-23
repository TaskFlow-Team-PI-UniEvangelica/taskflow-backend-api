#!/usr/bin/env bash
set -euo pipefail
cd /srv/taskflow

TARGET="${1:?uso: deploy.sh backend|frontend <sha>}"
NEW_TAG="${2:?informe a tag/sha}"
C="docker compose --env-file .env --env-file tags.env"

case "$TARGET" in
  backend)  VAR=BACKEND_TAG;  SERVICES="keycloak api" ;;
  frontend) VAR=FRONTEND_TAG; SERVICES="frontend" ;;
  *) echo "Alvo inválido: use backend ou frontend"; exit 1 ;;
esac

OLD_TAG="$(grep "^${VAR}=" tags.env | cut -d= -f2 || echo "latest")"
set_tag() { sed -i "s/^${VAR}=.*/${VAR}=$1/" tags.env; }

wait_healthy() {
  for svc in $SERVICES; do
    echo "Aguardando saúde do serviço: $svc..."
    for _ in $(seq 1 40); do
      status="$(docker inspect -f '{{.State.Health.Status}}' "$($C ps -q "$svc")" 2>/dev/null || echo starting)"
      if [ "$status" = "healthy" ]; then
        echo "Serviço $svc está saudável (healthy)!"
        continue 2
      fi
      sleep 5
    done
    echo "ERRO: O serviço $svc não atingiu o estado healthy dentro do tempo limite."
    return 1
  done
}

# 1. Executa backup pré-deploy caso envolva backend/migrations
if [ "$TARGET" = "backend" ] && [ -f ./scripts/backup.sh ]; then
  echo "Gerando backup pré-deploy..."
  ./scripts/backup.sh pre-deploy || echo "Aviso: Backup pré-deploy falhou, verifique as permissões OCI."
fi

# 2. Atualiza a tag e realiza o pull da nova versão
echo "Atualizando tag $VAR para $NEW_TAG..."
set_tag "$NEW_TAG"
$C pull $SERVICES
$C up -d $SERVICES

# 3. Verifica a integridade dos serviços; se falhar, aciona rollback automático
if ! wait_healthy; then
  echo "ALERTA: Falha no deploy de $NEW_TAG. Executando rollback automático para $OLD_TAG..."
  $C logs --tail=100 $SERVICES || true
  set_tag "$OLD_TAG"
  $C up -d $SERVICES
  exit 1
fi

echo "Deploy do $TARGET finalizado com sucesso com a tag $NEW_TAG."
docker image prune -f >/dev/null 2>&1 || true
