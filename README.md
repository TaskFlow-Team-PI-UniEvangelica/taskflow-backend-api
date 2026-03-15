# Taskflow API
API desenvolvida com foco no gerenciado de usuários e tarefas, utilizando Spring Boot e Java 21, para a execução do Projeto Integrativo e Projeto de Conclusão de Curso da Universidade Evangélica de Ánapolis.

## Dependências e Tecnologias Usadas

* **Linguagem:** Java 21+
* **Framework:** Spring Boot
* **Banco de Dados:** PostgreSQL
* **Migrações de Banco:** Flyway
* **Autenticação:** Spring Security e autenticação JWT com biblioteca Auth0
* **Utilitários:** Lombok, Spring DevTools

## Organização de Pastas

A arquitetura do projeto é baseada em uma arquitetura limpa visando uma maior organização de código e separação de responsabilidades.

```
src/main/java/unievangelica/taskflow/api
├── controllers/            # Controllers com as rotas REST
├── domain/                 # Pasta que engloba os principais módulos do projeto
│   ├── persistence/ 
│   │   ├── entities/       # Entidades JPA
│   │   └── repositories/   # Repositórios (Interfaces)
│   └── service/            # Regras de negócio
├── dto/                    # DTOs de request e response
├── infra/security/         # Filtros e serviços de autenticação JWT
└── ApiTaskflowApplication  # Classe de inicialização da aplicação
```

# Configuração do .env

Configuração das variáveis de ambiente pelo arquivo (.env), crie um arquivo chamado .env na raiz do projeto, cole as informações abaixo no arquivo e adicione as suas configurações locais.

```
DB_URL=jdbc:postgresql://localhost:5432/nome_do_seu_banco
DB_USER=seu_usuario
DB_PASSWORD=sua_senha
JWT_SECRET=sua_assinatura_token
```

Aponte o .env como variáveis de ambiente usando o caminho do arquivo do .env no inteliJ conforme o vídeo:
https://drive.google.com/file/d/1DN_R-e5uknsZUgw9rTt7TvpMb6NjcUoc/view?usp=sharing

Caso problemas na configuração use hardcode no arquivo `application.properties`, subistitua as variáveis de ambiente pelo valor do seu banco de dados como no exemplo abaixo:

```
spring.application.name=api-taskflow
spring.datasource.url=postgresql://localhost:5432/nome_do_seu_banco
spring.datasource.username=seu_usuario
spring.datasource.password=sua_senha
api.security.token.secret=${JWT_SECRET:sua_assinatura_token}
```

Localizado em `src/main/resources/application.properties`.

# Como executar a aplicação

Para rodar o projeto localmente, certifique-se de ter os seguintes pré-requisitos instalados na sua máquina:
* **Java 21 +**
* **PostgreSQL** (rodando na porta padrão 5432 senão tera que alterar a porta na url)
* Use a IDE **IntelliJ IDEA** para rodar o projeto e gerenciar as variáveis de ambiente.

### Passo a passo para execução:

1. **Clone o repositório:**
Clone o repositório do backend seja usando terminal ou baixando o arquivo ZIP.

2. **Acesse a pasta do projeto pelo terminal:**
   ```bash
   cd taskflow-backend-api
   ```

3. **Crie o banco de dados:**
Acesse o painel do postgress, use algo como o pgAdmin ou o psql no terminal e crie um banco de dados vazio com o mesmo nome que você definiu nas variáveis de ambiente (ex: `taskflow_db`).
   > **Nota:** Não é necessário criar as tabelas manualmente. O **Flyway** cuida das migrations das tabelas.

4. **Configure as Variáveis de Ambiente:**
   Siga as instruções da seção `Configuração do .env` acima para garantir que a aplicação consiga se conectar ao banco de dados.

5. **Inicie a aplicação:**
    * **Via Terminal (Linux):**
      ```bash
      ./mvnw spring-boot:run
      ```
    * **Via Terminal (Windows):**
      ```cmd
      mvnw.cmd spring-boot:run
      ```
    * **Via IDE:** Localize a classe `ApiTaskflowApplication.java` no caminho `src/main/java/unievangelica/taskflow/api/` e rode a classe.

Após inicializar a classe acesse o caminho da api `http://localhost:8080`.

# Endpoints da API

Abaixo estão as rotas para testar via postman, a collection está na raiz do projeto:

* **Autenticação:**
    * `POST /auth/login` - Autentica o usuário e retorna o token JWT.
    * `POST /auth/register` - Cria uma nova conta de usuário.

* **Tarefas (Requer token obtido pela rota de login):**
    * `GET /task` - Lista as tarefas de todos os usuário autenticado.
    * `POST /task` - Cria uma nova tarefa (apenas admin).
    * `PUT /task/{id}` - Atualiza os dados de uma tarefa.
    * `DELETE /task/{id}` - Exclui uma tarefa (apenas admin).

* **Usuários (Somente admin, rota desatualizada pois não salva usuário com senha hash):**
  * `GET /user` - Lista todos os usuário.
  * `POST /user` - Adiciona um novo usuário.