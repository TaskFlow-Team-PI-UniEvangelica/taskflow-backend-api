# Taskflow API
API desenvolvida com foco no gerenciado de usuários e tarefas, utilizando Spring Boot e Java 21, para a execução do Projeto Integrativo e Projeto de Conclusão de Curso da Universidade Evangélica de Ánapolis.

- URL de deploy de testes, cole no navegador para acessar a aplicação sem executar localmente:
    ```
    a definir ainda
    ```

## Configuração e Variáveis de Ambiente
Antes de rodar a aplicação, renomeie o arquivo `.env.example` para `.env` e preencha as variáveis corretamente (Banco de dados, Keycloak, etc).

## Swagger / OpenAPI
A documentação interativa da API pode ser acessada através do Swagger UI. Como utilizamos o Keycloak para autenticação (Multi-Tenant RBAC), o Swagger já está configurado para interagir com o OAuth2 (Authorization Code Flow) e redirecionar para a tela de login.
- URL do Swagger: `http://localhost:8080/swagger-ui/index.html`

## Dependências e Tecnologias Usadas
* **Linguagem:** Java 21+
* **Framework:** Spring Boot
* **Banco de Dados:** PostgreSQL
* **Migrações de Banco:** Flyway
* **Autenticação & IAM:** Spring Security 6 Resource Server integrado ao Keycloak (OAuth2, OpenID Connect e JWT com validação JWKS)
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
├── docker-compose.yml                           # Orquestração de Produção em containers
├── docker-compose.dev.yml                       # Orquestração de Desenvolvimento (Keycloak, DBs, Mailpit, PgAdmin)
├── Dockerfile                                   # Build da imagem da API
├── keycloak/                                    # Arquivos de provisionamento e temas do Keycloak
│   ├── realm-export.json                        # Export do Realm TaskFlow (Clients, Roles, Políticas)
│   └── theme/taskflow/                          # Tema customizado Taskflow (Login, Registro, E-mails, Verificação)
├── Makefile                                     # Atalhos de automação (make run, make test, etc.)
├── .env                                         # Arquivo de variáveis de ambiente locais
├── .env.example                                 # Modelo de variáveis de ambiente para dev/prod
├── pom.xml                                      # Gerenciador de dependências (Maven)
└── README.md                                    # Documentação principal do projeto
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
KEYCLOAK_JWKS_URI=http://localhost:9090/realms/taskflow-realm/protocol/openid-connect/certs

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
# FERRAMENTAS EXTRAS DE DEV (PGADMIN)
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
KC_DOMAIN=auth.taskflow.party
FRONTEND_URL=https://taskflow.party

# Token do Cloudflare Zero Trust (Quando adquirir o domínio)
CLOUDFLARE_TUNNEL_TOKEN=seu_token_gigante_aqui
```



## Arquitetura Multi-Tenancy (Organizações) e Governança de Tarefas
O sistema opera em uma arquitetura Multi-Tenant (SaaS) orientada a Organizações:
- Um Usuário pode pertencer a múltiplas Organizações (`OrganizationEntity`), com papéis hierárquicos gerenciados na aplicação: `OWNER` > `SUB_OWNER` > `MANAGER` > `MEMBER`.
- **Governança Granular por Organização:** O `OWNER` configura permissões para membros (`MEMBER`) relativas à criação, prazos, edição (todas vs. próprias/atribuídas), movimentação no Kanban (todas vs. próprias/atribuídas), retorno de concluídas e exclusão de tarefas, além de política de retenção para auditoria de tarefas excluídas.
- **Numeração Sequencial Monotônica:** Cada tarefa recebe um número sequencial único dentro de sua organização (`1 - Nome da Task`), que jamais se repete ou é reciclado.
- As Tarefas (`TaskEntity`) pertencem primariamente a uma Organização, garantindo estrito isolamento de dados entre equipes e prevenindo vulnerabilidades de BOLA (Broken Object Level Authorization).

## Just-In-Time (JIT) Provisioning
Para manter a sincronia com o Keycloak sem rotinas manuais, a API utiliza JIT Provisioning no `UserProvisioningFilter`. Ao autenticar, os dados do token (Keycloak UUID, e-mail, nome) são espelhados automaticamente no PostgreSQL. Alterações de nome e e-mail feitas no console de conta do Keycloak também são sincronizadas em tempo real via `GET /user/me`.

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
* **Mailpit (Caixa de E-mails para Testes):** [http://localhost:8025](http://localhost:8025)
    * *Não exige senha. Captura em tempo real os e-mails de confirmação de conta (`Verify Email`), recuperação de senha e convites para organizações.*
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

# Documentação da API (Swagger)

A API possui sua documentação gerada dinamicamente através do **Swagger UI** (OpenAPI 3). Nele, você pode visualizar todos os endpoints disponíveis, seus métodos, esquemas de requisição/resposta, validações e testar as requisições diretamente do navegador.

### Como acessar o Swagger:
1. Certifique-se de que a aplicação Spring Boot (backend) esteja rodando na sua máquina.
2. Acesse a seguinte URL no seu navegador:
   [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

### Como realizar requisições autenticadas no Swagger:
A API está perfeitamente integrada ao Keycloak. Você não precisa gerar o token manualmente e colar. Para testar as rotas fechadas:
1. Clique no botão **Authorize** (geralmente com um ícone de cadeado) no canto superior direito da tela do Swagger.
2. Na janela modal que abrir, role para baixo até a opção **OAuth2**.
3. Deixe o client_id como `taskflow-frontend` e clique em **Authorize**.
4. Você será redirecionado automaticamente para a tela de login do Keycloak. Faça login com suas credenciais.
5. Após o login, você será redirecionado de volta para o Swagger já autenticado. A partir desse momento, basta clicar em **"Try it out"** nas rotas e o Swagger injetará o token automaticamente.

*Nota:* Se houver erro de "Invalid redirect_uri", verifique se configurou a URL `http://localhost:8080/*` nos *Valid Redirect URIs* do Client `taskflow-frontend` no painel de administração do Keycloak, ou se iniciou um banco do zero após a última atualização do arquivo `realm-export.json`.