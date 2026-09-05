# Taskflow API
API desenvolvida com foco no gerenciado de usuários e tarefas, utilizando Spring Boot e Java 21, para a execução do Projeto Integrativo e Projeto de Conclusão de Curso da Universidade Evangélica de Ánapolis.

- URL de deploy de testes, cole no navegador para acessar a aplicação sem executar localmente:
    ```
    a definir ainda
    ```

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

# Configuração do .env para Desenvolvimento (Local)
Crie um arquivo chamado `.env` na raiz do projeto. Ele será usado pelo `docker-compose.yml` (para Dev) e pelo IntelliJ.
```env
# ===============================
# BANCO DA API (PostgreSQL)
# ===============================
DB_NAME=taskflow
DB_USER=postgres
DB_PASSWORD=postgres_secret

# ===============================
# URLS EXCLUSIVAS PARA RODAR NO INTELLIJ
# ===============================
DB_URL=jdbc:postgresql://localhost:5433/taskflow
KEYCLOACK_URL_VITE=http://localhost:9090/realms/taskflow-realm

# ===============================
# CORS (Permissões de acesso do Frontend)
# ===============================
FRONTEND_URL=http://localhost
FRONTEND_URL_VITE=http://localhost:5173

# ===============================
# BANCO DO KEYCLOAK (PostgreSQL)
# ===============================
KC_DB_NAME=keycloak
KC_DB_USER=keycloak
KC_DB_PASSWORD=keycloak_secret

# ===============================
# KEYCLOAK ADMIN
# ===============================
KC_ADMIN_USER=admin
KC_ADMIN_PASSWORD=admin

# ===============================
# FERRAMENTAS EXTRAS DE DEV
# ===============================
PGADMIN_EMAIL=admin@taskflow.com
PGADMIN_PASSWORD=admin
```
Aponte o .env como variáveis de ambiente usando o caminho do arquivo do .env no IntelliJ conforme o vídeo:
https://drive.google.com/file/d/1DN_R-e5uknsZUgw9rTt7TvpMb6NjcUoc/view?usp=sharing

# Configuração do .env para Produção
No servidor de produção, o `.env` exigirá chaves de segurança estritas e domínios oficiais.
```env
DB_NAME=taskflow_prod
DB_USER=postgres
DB_PASSWORD=sua_senha_segura_aqui

KC_DB_NAME=keycloak_prod
KC_DB_USER=keycloak
KC_DB_PASSWORD=outra_senha_segura_aqui

KC_ADMIN_USER=admin
KC_ADMIN_PASSWORD=senha_admin_segura

# Domínios Oficiais (SEM barra no final)
KC_DOMAIN=auth.taskflow.com.br
FRONTEND_URL=https://taskflow.com.br

# Token do Cloudflare Zero Trust (Quando adquirir o domínio)
CLOUDFLARE_TUNNEL_TOKEN=seu_token_gigante_aqui
```



## Arquitetura Multi-Tenancy (Organizações)
O sistema evoluiu de uma gestão de tarefas individual para uma arquitetura Multi-Tenant (SaaS). 
Agora, a regra de negócios baseia-se em Organizações:
- Um Usuário pode pertencer a múltiplas Organizações (`OrganizationEntity`).
- As Tarefas (`TaskEntity`) pertencem primariamente a uma Organização, permitindo isolamento de dados entre diferentes equipes e empresas, resolvendo vulnerabilidades de BOLA (Broken Object Level Authorization).

## Just-In-Time (JIT) Provisioning
Para manter a sincronia com o provedor de identidade (Keycloak) sem depender de rotinas de sincronização complexas, a API utiliza a estratégia de JIT Provisioning.
Ao realizar o primeiro acesso autenticado, o `UserProvisioningFilter` intercepta a requisição, extrai as informações do Token JWT (Subject UUID, E-mail, Nome) e insere automaticamente o registro do usuário na tabela do PostgreSQL. Isso garante a amarração transacional com Tarefas e Organizações sem atrito.

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

5. **Comandos de Orquestração (Makefile):**
   O projeto utiliza um `Makefile` unificado para gerenciar separadamente os ambientes de Desenvolvimento e Produção. No Windows, você pode rodar os mesmos comandos se tiver o WSL ou ferramentas como Git Bash / Make for Windows instalados. Caso não tenha, os comandos brutos do Docker Compose estão comentados no próprio arquivo `Makefile`.

   ### Ambiente de Desenvolvimento (Sufixo `-dev`)
   Ideal para programar localmente. Sobe o Banco, Keycloak, PgAdmin e Mailpit com portas expostas.
   * `make run-dev`: Sobe todo o ambiente de desenvolvimento.
   * `make run-dev-infra`: Sobe apenas os bancos e ferramentas (Sem a API Java). Use isso para poder rodar o Java direto na IDE (IntelliJ) e aproveitar o Hot Reload.
   * `make down-db-all-dev`: Destrói todos os containers e apaga todos os bancos de dados de Dev.
   * `make down-db-api-dev`: Apaga somente o banco da API.
   * `make down-db-keycloak-dev`: Apaga somente o banco do Keycloak.
   * `make logs-dev`: Acompanha os logs em tempo real.
   * `make clean-dev`: Remove containers e imagens de desenvolvimento.

   ### Ambiente de Produção (Sem sufixo)
   Sobe a arquitetura blindada. O Keycloak roda em modo otimizado, bancos sem portas externas, pronto para o Cloudflare Tunnel.
   * `make run`: Sobe todo o ambiente de produção.
   * `make down`: Desliga o ambiente (mantendo os dados).
   * `make down-db-all`: Destrói todos os containers e apaga todos os bancos de Prod.
   * `make down-db-api`: Apaga somente o banco da API de Prod.
   * `make down-db-keycloak`: Apaga somente o banco do Keycloak de Prod.
   * `make logs`: Acompanha os logs em tempo real.
   * `make clean`: Remove containers e imagens de produção.

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
   Siga as instruções da seção `Configuração do .env` acima para garantir que a aplicação consiga se conectar ao banco de dados e ao frontend.

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


# Acessos Locais (Ambiente de Desenvolvimento)
Após executar `make run-dev` (ou `make run-dev-infra`), os seguintes serviços estarão disponíveis no seu navegador:

* **Keycloak (Painel Admin):** [http://localhost:9090](http://localhost:9090)
    * *Usuário padrão:* `admin` | *Senha:* `admin`
* **Mailpit (Caixa de E-mails Falsos):** [http://localhost:8025](http://localhost:8025)
    * *Não exige senha. Use para ler os e-mails de "Recuperar Senha".*
* **pgAdmin (Gerenciador de Banco de Dados):** [http://localhost:5050](http://localhost:5050)
    * *E-mail:* `admin@taskflow.com` | *Senha:* `admin`
    * *(Para conectar ao banco lá dentro, use o host `db` e a porta `5432`)*
* **API Spring Boot:** [http://localhost:8080](http://localhost:8080)


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
    * O sistema utiliza **Keycloak (OIDC)** como Identity Provider. O Backend não possui rotas públicas de login/registro. Todo fluxo de autenticação ocorre externamente e a API apenas valida o Token JWT via Resource Server.

* **Usuários Me (Funciona com base no Token do usuário):**
    * `GET /user/me` - Lista o perfil do usuário logado.

* **Usuários (Somente admin, essas rotas serão consumidas posteriormente pelo frontend):**
    * `GET /user` - Lista todos os usuários (Essa rota não é para apenas admin).
    * `POST /user` - Adiciona um novo usuário.
    * `PUT /user/{id}` - Atualiza todos os dados de um usuário menos a senha.
    * `DELETE /task/{id}` - Exclui usuário pelo id.

* **Tarefas (Requer token obtido pela rota de login):**
    * `GET /task` - Lista as tarefas de todos os usuário autenticado.
    * `POST /task` - Cria uma nova tarefa (apenas admin).
    * `PUT /task/{id}` - Atualiza os dados de uma tarefa.
    * `PATCH /task/{id}/status` - Atualiza os dados de status de uma tarefa, usado para o Kanban do Frontend.
    * `DELETE /task/{id}` - Exclui uma tarefa (apenas admin).