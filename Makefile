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

# desliga todos os services incluindo o volume do db
down-db:
	docker compose down -v

# logs da api
logs:
	docker compose logs -f api

# acessa o terminal da api
terminal:
	docker exec -it taskflow_api bash