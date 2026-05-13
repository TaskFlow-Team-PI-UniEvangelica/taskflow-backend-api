# Taskflow API
API desenvolvida com foco no gerenciado de usuários e tarefas, utilizando Spring Boot e Java 21, para a execução do Projeto Integrativo e Projeto de Conclusão de Curso da Universidade Evangélica de Ánapolis.

## Dependências e Tecnologias Usadas
* **Linguagem:** Java 21+
* **Framework:** Spring Boot
* **Banco de Dados:** PostgreSQL
* **Migrações de Banco:** Flyway
* **Autenticação:** Spring Security e autenticação JWT com biblioteca Auth0
* **Infraestrutura:** Docker, Docker Compose e Makefile
* **Testes:** JUnit 5, Mockito, AssertJ
* **Utilitários:** Lombok, Spring DevTools

## Organização de Pastas
A arquitetura do projeto é baseada em uma arquitetura limpa visando uma maior organização de código e separação de responsabilidades.
```
taskflow-backend-api/
├── src/
│   ├── main/
│   │   ├── java/unievangelica/taskflow/api/
│   │   │   ├── controllers/                    # Controllers com as rotas da API REST
│   │   │   ├── domain/                         # Núcleo da aplicação (Regras e Banco)
│   │   │   │   ├── persistence/                # Camada de acesso a dados
│   │   │   │   │   ├── entities/               # Entidades mapeadas para o banco (JPA/Hibernate)
│   │   │   │   │   └── repositories/           # Interfaces de persistência (Spring Data JPA)
│   │   │   │   └── service/                    # Classes contendo as regras de negócio
│   │   │   ├── dto/                            # Objetos de Transferência de Dados (DTOs)
│   │   │   │   ├── request/                    # DTOs para payload de entrada (Validações)
│   │   │   │   └── response/                   # DTOs para formatação de saída
│   │   │   ├── infra/security/                 # Configurações do Spring Security e filtros JWT
│   │   │   └── ApiTaskflowApplication.java     # Classe de inicialização do Spring Boot
│   │   └── resources/                          # Arquivos estáticos e de configuração
│   │       ├── db/migration/                   # Scripts SQL de versionamento do Flyway
│   │       └── application.properties          # Configurações base do projeto
│   └── test/
│       └── java/unievangelica/taskflow/api/
│           ├── test/
│           │   ├── integration/                # Suítes de testes de integração
│           │   └── unit/                       # Suítes de testes unitários isolados
│           │       ├── controllers/
│           │       ├── entities/
│           │       ├── repositories/
│           │       └── services/                # Testes para validação de regras de negócios
│           └── ApiTaskflowApplicationTests.java # Contexto principal de testes
├── docker-compose.yml                           # Orquestração do Banco e API em containers
├── Dockerfile                                   # Build da imagem da API
├── Makefile                                     # Atalhos de automação (make run, make test)
├── .env                                         # Arquivo de variáveis de ambiente locais
├── pom.xml                                      # Gerenciador de dependências (Maven)
├── README.md                                    # Documentação principal do projeto
└── TaskFlow-API.postman_collection.json         # Collection de endpoints para uso no Postman
```

# Configuração do .env para execução via Docker
Configuração das variáveis de ambiente pelo arquivo (.env), crie um arquivo chamado .env na raiz do projeto, cole as informações abaixo no arquivo e adicione as suas configurações do banco de dados de acordo com o exemplo.
```
DB_URL=jdbc:postgresql://localhost:5433/nome_do_seu_banco
DB_USER=seu_usuario
DB_PASSWORD=sua_senha
JWT_SECRET=sua_assinatura_token
```

# Configuração do .env para execução manual
Configuração das variáveis de ambiente pelo arquivo (.env), crie um arquivo chamado .env na raiz do projeto, cole as informações abaixo no arquivo e adicione as suas configurações locais.
```
DB_URL=jdbc:postgresql://localhost:5432/nome_do_seu_banco
DB_USER=seu_usuario
DB_PASSWORD=sua_senha
JWT_SECRET=sua_assinatura_token
```
Aponte o .env como variáveis de ambiente usando o caminho do arquivo do .env no inteliJ conforme o vídeo:
https://drive.google.com/file/d/1DN_R-e5uknsZUgw9rTt7TvpMb6NjcUoc/view?usp=sharing

# Para problemas no uso do .env ou Docker
Caso problemas na configuração ou execução do Docker use hardcode no arquivo `application.properties`, subistitua as variáveis de ambiente pelo valor do seu banco de dados como no exemplo abaixo, vale lembrar a necessidade de ter o Java e o Postgres instalados:
```
spring.application.name=api-taskflow
spring.datasource.url=postgresql://localhost:5432/nome_do_seu_banco
spring.datasource.username=seu_usuario
spring.datasource.password=sua_senha
api.security.token.secret=${JWT_SECRET:sua_assinatura_token}
```
Localizado em `src/main/resources/application.properties`.

# Como executar a aplicação via Docker (Linux)
Para rodar o projeto localmente, certifique-se de ter os seguintes pré-requisitos instalados na sua máquina:
* **Docker, Docker Compose, Docker Desktop**
* **Make**
* Use a IDE **IntelliJ IDEA** para visualizar o backend.

### Passo a passo para execução no Linux:

1. **Clone o repositório:**
   Clone o repositório do backend seja usando terminal ou baixando o arquivo ZIP.

2. **Acesse a pasta do projeto pelo terminal:**
   ```bash
   cd taskflow-backend-api
   ```
   
3. **Garantia de acesso:**
   Garanta que o Docker tenha acesso de administrador na máquina para não ser necessário usar sudo.

4. **Env configurado:**
   Tenha o arquivo .env devidamente configurado como nos tópicos anteriores.

5. **Comandos Docker para execução e gerência da api e banco de dados:**
   #### Subir container:
   ```bash
   make run
   ```
   #### Derrubar container:
   ```bash
   make down
   ```
   #### Derrubar container e volume do banco de dados:
   ```bash
   make down-db
   ```
   #### Acessar logs do container:
   ```bash
   make logs
   ```
   #### Acessar terminal da API do container:
   ```bash
   make terminal
   ```
   Para sair digite ``` exit ``` no terminal.

# Como executar a aplicação via Docker (Windows)
Para rodar o projeto localmente, certifique-se de ter os seguintes pré-requisitos instalados na sua máquina:
* **Docker Desktop**
* Use a IDE **IntelliJ IDEA** para visualizar o backend.

### Passo a passo para execução no Windows:
1. **Clone o repositório:**
   Clone o repositório do backend seja usando terminal ou baixando o arquivo ZIP.

2. **Acesse a pasta do projeto pelo terminal:**
   ```bash
   cd taskflow-backend-api
   ```

3. **Garantia de acesso:**
   Garanta que o Docker tenha acesso de administrador na máquina.

4. **Env configurado:**
   Tenha o arquivo .env devidamente configurado como nos tópicos anteriores. 

5. **Comandos Docker para execução e gerência da api e banco de dados:**
   #### Subir container:
   ```cmd
   docker compose up --build
   ```
   #### Derrubar container:
   ```cmd
   docker compose down
   ```
   #### Derrubar container e volume do banco de dados:
   ```cmd
   docker compose down -v
   ```
   #### Acessar logs do container:
   ```cmd
   docker compose logs -f api
   ```
   #### Acessar terminal da API do container:
   ```cmd
   docker exec -it taskflow_api bash
   ```
   Para sair digite ``` exit ``` no terminal.

# Como executar a aplicação manualmente

Para rodar o projeto localmente, certifique-se de ter os seguintes pré-requisitos instalados na sua máquina:
* **Java 21 +**
* **PostgreSQL** (rodando na porta padrão 5432 senão tera que alterar a porta na url)
* Use a IDE **IntelliJ IDEA** para rodar o projeto e gerenciar as variáveis de ambiente.

### Passo a passo para execução manual:

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

# Como executar Testes Unitários

O projeto utiliza **JUnit 5**, **Mockito** e **AssertJ** para a cobertura de testes unitários, focando em garantir a integridade das regras de negócio (camada de *Service*) de forma isolada, sem a necessidade de instanciar o banco de dados.

### 1. Via Makefile (Linux)
Na raiz do projeto, sem estar executando o backend e nenhum container execute:
  ```bash
  make test
  ```

### 2. Via Terminal (Maven Wrapper)
Esta é a forma recomendada para validar todo o projeto de uma vez (muito útil para pipelines de CI/CD). Na raiz do projeto, execute:

* **Linux:**
  ```bash
  ./mvnw test
  ```

* **Windows:**
  ```cmd
  mvnw.cmd test
  ```

# Endpoints da API

Abaixo estão as rotas para testar via postman, a collection está na raiz do projeto:

* **Autenticação:**
    * `POST /auth/login` - Autentica o usuário e retorna o token JWT.
    * `POST /auth/register` - Cria uma nova conta de usuário.

* **Usuários Me (Funciona com base no Token do usuário):**
    * `GET /user/me` - Lista o perfil do usuário logado.
    * `PATCH /user/me/password` - Essa rota permite o próprio usuário aualizar sua senha passando a antiga como parâmetro.

* **Usuários (Somente admin, essas rotas serão consumidas posteriormente pelo frontend):**
    * `GET /user` - Lista todos os usuários (Essa rota não é para apenas admin).
    * `POST /user` - Adiciona um novo usuário.
    * `PUT /user/{id}` - Atualiza todos os dados de um usuário menos a senha.
    * `PATCH /user/{id}/password` - Permite atualizar a senha do usuário passando a antiga senha como paramêtro e salva em hash. (função deve ser alterada para funcionamento do admin posteriormente)
    * `DELETE /task/{id}` - Exclui usuário pelo id.

* **Tarefas (Requer token obtido pela rota de login):**
    * `GET /task` - Lista as tarefas de todos os usuário autenticado.
    * `POST /task` - Cria uma nova tarefa (apenas admin).
    * `PUT /task/{id}` - Atualiza os dados de uma tarefa.
    * `PATCH /task/{id}/status` - Atualiza os dados de status de uma tarefa, usado para o Kanban do Frontend.
    * `DELETE /task/{id}` - Exclui uma tarefa (apenas admin).