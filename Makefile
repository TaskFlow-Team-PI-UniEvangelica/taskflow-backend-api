.PHONY: test run down down-db clean clean-db logs terminal

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

# desliga todos os services e apaga as imagens
clean:
	docker compose down --rmi all

# desliga todos os services, apaga o volume e apaga as imagens
clean-db:
	docker compose down -v --rmi all

# logs da api
logs:
	docker compose logs -f api

# acessa o terminal da api
terminal:
	docker exec -it taskflow_api bash