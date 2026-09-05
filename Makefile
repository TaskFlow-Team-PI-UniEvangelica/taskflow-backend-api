.PHONY: test run run-dev down down-db down-db-dev down-db-all down-db-all-dev down-db-api down-db-api-dev down-db-keycloak down-db-keycloak-dev clean clean-dev logs logs-dev terminal terminal-dev

# executa testes unitários
test:
	./mvnw test

# ==========================================
# SUBIR AMBIENTES
# ==========================================
# sobe o ambiente de Produção
run:
	docker compose up --build

# sobe o ambiente de Desenvolvimento
# sobe SOMENTE a Infraestrutura de Dev (Sem a API) para você rodar o Java na IDE
run-dev-infra:
	docker compose up keycloak mailpit pgadmin -d

run-dev:
	docker compose -f docker-compose.dev.yml up --build


# ==========================================
# DESLIGAR (PRESERVA DADOS)
# ==========================================
# desliga todos os serviços (ambientes)
down:
	docker compose down
	docker compose -f docker-compose.prod.yml down


# ==========================================
# DESTRUIÇÃO DE BANCOS DE DADOS (PRODUÇÃO)
# (Sem sufixo)
# ==========================================
# derruba o ambiente de Prod inteiro e apaga todos os bancos de Prod
down-db-all:
	docker compose -f docker-compose.prod.yml down -v

# derruba apenas o contêiner da API de Prod e apaga o seu banco
down-db-api:
	docker rm -f taskflow_db_prod || true
	docker volume rm taskflow-backend-api_taskflow_pgdata_prod || true

# derruba apenas o contêiner do Keycloak de Prod e apaga o seu banco
down-db-keycloak:
	docker rm -f taskflow_keycloak_db_prod || true
	docker volume rm taskflow-backend-api_keycloak_pgdata_prod || true


# ==========================================
# DESTRUIÇÃO DE BANCOS DE DADOS (DESENVOLVIMENTO)
# (Com sufixo -dev)
# ==========================================
# derruba o ambiente de Dev inteiro e apaga todos os bancos de Dev
down-db-all-dev:
	docker compose down -v

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
clean:
	docker compose -f docker-compose.prod.yml down --rmi all

clean-dev:
	docker compose down --rmi all

logs:
	docker compose -f docker-compose.prod.yml logs -f

logs-dev:
	docker compose logs -f

terminal:
	docker exec -it taskflow_api_prod bash

terminal-dev:
	docker exec -it taskflow_api bash
