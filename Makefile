.PHONY: test run-dev run-dev-infra run-dev-infra-db down down-db-all-dev down-db-api-dev down-db-keycloak-dev clean-dev logs-dev terminal-dev

# executa testes unitários
test:
	./mvnw test

# ==========================================
# SUBIR AMBIENTE DE DESENVOLVIMENTO
# (produção: deploy/docker-compose.prod.yml, publicado pelo CI)
# ==========================================
# sobe SOMENTE a Infraestrutura de Dev (Sem a API) para você rodar o Java na IDE
run-dev-infra:
	docker compose -f docker-compose.dev.yml up keycloak mailpit pgadmin

run-dev-infra-db:
	docker compose -f docker-compose.dev.yml up db keycloak_db keycloak mailpit pgadmin

run-dev:
	docker compose -f docker-compose.dev.yml up --build


# ==========================================
# DESLIGAR (PRESERVA DADOS)
# ==========================================
# desliga o ambiente de desenvolvimento
down:
	docker compose -f docker-compose.dev.yml down --remove-orphans


# ==========================================
# DESTRUIÇÃO DE BANCOS DE DADOS (DESENVOLVIMENTO)
# (Com sufixo -dev)
# ==========================================
# derruba o ambiente de Dev inteiro e apaga todos os bancos de Dev
down-db-all-dev:
	docker compose -f docker-compose.dev.yml down --remove-orphans -v

# derruba apenas o contêiner da API de Dev e apaga o seu banco
down-db-api-dev:
	docker rm -f taskflow_db || true
	docker volume rm taskflow-backend-api_taskflow_pgdata || true

# derruba apenas o contêiner do Keycloak de Dev e apaga o seu banco
down-db-keycloak-dev:
	docker rm -f taskflow_keycloak_db || true
	docker volume rm taskflow-backend-api_keycloak_pgdata || true


# ==========================================
# LIMPEZA PROFUNDA E LOGS
# ==========================================
clean-dev:
	docker compose -f docker-compose.dev.yml down --rmi all

logs-dev:
	docker compose -f docker-compose.dev.yml logs -f

terminal-dev:
	docker exec -it taskflow_api bash
