.PHONY: test run down logs terminal-db terminal-api

# executa testes unitários
test:
	./mvnw test

# sobe a api e o db
run:
	docker compose up --build

# desliga todos os services
down:
	docker compose down

# logs da api
logs:
	docker compose logs -f api

# acessa o terminal da api
terminal-api:
	docker exec -it taskflow_api bash

# acessa o terminal do banco postgres
terminal-db:
	docker exec -it taskflow_db psql -U root -d taskflow_db