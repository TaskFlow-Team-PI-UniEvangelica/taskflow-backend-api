# Taskflow API
API desenvolvida com foco no gerenciamento de usuários, organizações e tarefas, utilizando Spring Boot 4 e Java 21, para a execução do Projeto Integrativo e Projeto de Conclusão de Curso da Universidade Evangélica de Goiás (UniEVANGÉLICA).

- **URL oficial de produção:**
  - Frontend: `https://taskflow.party`
  - API: `https://api.taskflow.party`
  - Autenticação (Keycloak): `https://auth.taskflow.party`

## Configuração e Variáveis de Ambiente
Antes de rodar a aplicação, renomeie o arquivo `.env.example` para `.env` e preencha as variáveis corretamente (Banco de dados, Keycloak, SMTP, etc).

## Swagger / OpenAPI
Em desenvolvimento, a documentação interativa da API pode ser acessada através do Swagger UI. Como utilizamos o Keycloak para autenticação (Multi-Tenant RBAC), o Swagger está configurado para interagir com o OAuth2 (Authorization Code Flow com PKCE).
- URL do Swagger (dev): `http://localhost:8080/swagger-ui/index.html`
- Em produção (`SPRING_PROFILES_ACTIVE=prod`), o Swagger UI e `/v3/api-docs` ficam desativados por segurança.

## Dependências e Tecnologias Usadas
* **Linguagem:** Java 21
* **Framework:** Spring Boot 4.0.9
* **Banco de Dados:** PostgreSQL 16 (instância única em produção com papéis segregados por privilégio mínimo)
* **Migrações de Banco:** Flyway (`V1`–`V13`)
* **Autenticação & IAM:** Spring Security 7 Resource Server integrado ao Keycloak 26.8.2 (OAuth2, OpenID Connect, JWT com validação JWKS e audiência `aud=taskflow-api`)
* **Padronização de Erros:** RFC 9457 `ProblemDetail` (`application/problem+json`) via `GlobalExceptionHandler`
* **Infraestrutura & CI/CD:** Docker, Docker Compose, GNU Make (`Makefile`), GitHub Actions + Trivy + Oracle Cloud (ARM64) + Cloudflare Tunnel
* **Testes:** JUnit 5, Mockito, AssertJ, Spring Security Test e Testcontainers (PostgreSQL 16)
* **Utilitários:** Lombok, FreeMarker (e-mails assíncronos pós-commit), SpringDoc OpenAPI 3 (`3.0.1`)

## Organização de Pastas
A arquitetura do projeto segue uma arquitetura em camadas (`controllers`, `domain`, `dto` e `infra`), visando organização clara de código e separação de responsabilidades.
```
taskflow-backend-api/
├── src/
│   ├── main/
│   │   ├── java/unievangelica/taskflow/api/
│   │   │   ├── controllers/                    # Controllers com as rotas da API REST
│   │   │   ├── domain/                         # Núcleo da aplicação (Regras, Eventos e Banco)
│   │   │   │   ├── event/                      # Eventos de domínio pós-commit (InviteCreatedEvent)
│   │   │   │   ├── exception/                  # Exceções de domínio (Business, Conflict, ConviteExpirado, ResourceNotFound)
│   │   │   │   ├── persistence/                # Camada de acesso a dados
│   │   │   │   │   ├── entities/               # Entidades mapeadas (User, UserAvatar, Organization, Task, TaskHistory, Invites)
│   │   │   │   │   └── repositories/           # Interfaces de persistência (Spring Data JPA)
│   │   │   │   └── service/                    # Regras de negócio, e-mails pós-commit e jobs agendados
│   │   │   ├── dto/                            # Objetos de Transferência de Dados (DTOs)
│   │   │   │   ├── request/                    # DTOs para payload de entrada (Validações)
│   │   │   │   └── response/                   # DTOs para formatação de saída
│   │   │   ├── infra/                          # Configurações transversais de infraestrutura
│   │   │   │   ├── async/                      # Configuração de execução assíncrona (@Async)
│   │   │   │   ├── database/                   # Avisos e validações de papel do banco em startup
│   │   │   │   ├── exceptions/                 # GlobalExceptionHandler (ProblemDetail RFC 9457)
│   │   │   │   ├── scheduling/                 # Agendamento do expurgo de histórico (@Scheduled)
│   │   │   │   └── security/                   # Spring Security 7, filtro JIT, entry point 401 e OpenAPI
│   │   │   └── ApiTaskflowApplication.java     # Classe de inicialização do Spring Boot
│   │   └── resources/                          # Arquivos estáticos e de configuração
│   │       ├── db/migration/                   # Scripts SQL de versionamento do Flyway (V1–V13)
│   │       ├── templates/
│   │       │   └── invite.ftlh                 # Template FreeMarker de convite organizacional
│   │       ├── application.properties          # Configurações base do projeto
│   │       └── application-prod.properties     # Profile de produção (ddl-auto=validate, open-in-view=false)
│   └── test/
│       ├── java/unievangelica/taskflow/api/
│       │   ├── test/
│       │   │   ├── integration/                # Testes de integração com Testcontainers PostgreSQL 16 (BOLA, schema, avatares, convites)
│       │   │   └── unit/                       # Testes unitários isolados
│       │   │       ├── controllers/            # MockMvc & Spring Security Test
│       │   │       ├── database/               # Avisos de privilégio de banco
│       │   │       ├── entities/               # Entidades e enums
│       │   │       ├── exceptions/             # Contrato ProblemDetail RFC 9457
│       │   │       ├── security/               # Filtro JIT, SpringSecurityConfig e audiência JWT
│       │   │       └── services/               # Serviços, e-mails assíncronos e expurgo agendado
│       │   └── ApiTaskflowApplicationTests.java # Carregamento de contexto de testes
│       └── resources/
│           └── application-it.properties       # Perfil dos testes de integração
├── deploy/
│   ├── docker-compose.prod.yml                 # Stack de produção (PostgreSQL único, Keycloak 26, API, Frontend, Cloudflared)
│   └── scripts/                                # Scripts operacionais (deploy, backup, privilégio mínimo, setup Keycloak)
├── keycloak/                                   # Provisionamento, imagem otimizada e tema customizado do Keycloak 26
│   ├── Dockerfile                              # Build multi-stage com kc.sh build e tema embutido
│   ├── realm-export.json                       # Export do Realm TaskFlow (Clients, Roles, User Profile, Políticas)
│   └── theme/taskflow/                         # Tema customizado (Login, Registro, Account Console, E-mails)
├── Dockerfile                                  # Build da imagem da API (usuário não-root)
├── docker-compose.dev.yml                      # Orquestração de Desenvolvimento (Keycloak, DBs, Mailpit, PgAdmin)
├── Makefile                                    # Atalhos de automação para desenvolvimento e testes
├── .env.example                                # Modelo de variáveis de ambiente para dev/prod
├── pom.xml                                     # Gerenciador de dependências (Maven)
└── README.md                                   # Documentação principal do projeto
```

# Configuração do `.env` para Desenvolvimento (Local)
Crie um arquivo chamado `.env` na raiz do projeto (a partir do `.env.example`). Ele será usado pelo `docker-compose.dev.yml` e pela IDE (ex: IntelliJ).
```env
# ===============================
# BANCO DA API (PostgreSQL)
# ===============================
DB_NAME=taskflow
DB_USER=postgres
DB_PASSWORD=postgres_secret

# ===============================
# URLS EXCLUSIVAS PARA RODAR NO INTELLIJ / HOST
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
Aponte o `.env` como variáveis de ambiente na configuração de execução da sua IDE (por exemplo no IntelliJ conforme demonstrado em: https://drive.google.com/file/d/1DN_R-e5uknsZUgw9rTt7TvpMb6NjcUoc/view?usp=sharing).

# Configuração do `.env` para Produção (`/srv/taskflow/.env`)
Em produção, o arquivo `deploy/docker-compose.prod.yml` exige validação estrita das variáveis (sem senhas padrão) e papéis segregados por privilégio mínimo no PostgreSQL:
```env
GHCR_OWNER=taskflow-team-pi-unievangelica
FRONTEND_URL=https://taskflow.party
KC_DOMAIN=auth.taskflow.party

# Banco da API (superusuário de inicialização/backup + papéis de privilégio mínimo)
DB_NAME=taskflow
DB_USER=postgres_admin
DB_PASSWORD=gerar_com_openssl_rand_hex_32
DB_OWNER_USER=taskflow_owner
DB_OWNER_PASSWORD=gerar_com_openssl_rand_hex_32
DB_APP_USER=taskflow_app
DB_APP_PASSWORD=gerar_com_openssl_rand_hex_32

# Banco do Keycloak
KC_DB_NAME=keycloak
KC_DB_USER=keycloak
KC_DB_PASSWORD=gerar_com_openssl_rand_hex_32

KC_ADMIN_USER=bootstrap-admin
KC_ADMIN_PASSWORD=gerar_com_openssl_rand_hex_24

# SMTP (OCI Email Delivery)
SMTP_HOST=smtp.email.sa-saopaulo-1.oci.oraclecloud.com
SMTP_PORT=587
SMTP_USER=ocid1.user.oc1...
SMTP_PASSWORD=sua_senha_smtp_oracle
SMTP_FROM=nao-responda@taskflow.party

# Cloudflare Tunnel & Backups
CLOUDFLARE_TUNNEL_TOKEN=seu_token_cloudflare_tunnel
OCI_BACKUP_BUCKET=taskflow-backups
```

## Arquitetura Multi-Tenancy (Organizações) e Governança de Tarefas
O sistema opera em arquitetura Multi-Tenant orientada a Organizações:
- Um usuário pode pertencer a múltiplas Organizações (`OrganizationEntity`), com papéis hierárquicos gerenciados na aplicação: `OWNER` > `SUB_OWNER` > `MANAGER` > `MEMBER`.
- **Governança Granular por Organização:** O `OWNER` e o `SUB_OWNER` podem configurar as 9 flags de permissões dinâmicas para membros (`MEMBER`) relativas à criação, prazos, edição (todas vs. próprias/atribuídas), movimentação no Kanban (todas vs. próprias/atribuídas), retorno de concluídas e exclusão de tarefas. Apenas o `OWNER` pode alterar a retenção de auditoria (`deletedTasksRetentionDays`), transferir a posse ou excluir a organização.
- **Numeração Sequencial Monotônica:** Cada tarefa recebe um número sequencial único dentro de sua organização (`1 - Nome da Task`), mantido por incremento atômico no banco, que jamais se repete ou é reciclado.
- As Tarefas (`TaskEntity`) pertencem obrigatoriamente a uma Organização, garantindo isolamento estrito entre equipes e mitigação efetiva de BOLA/IDOR (Broken Object Level Authorization), validada por suíte de testes de integração.

## Just-In-Time (JIT) Provisioning
Para manter a sincronia com o Keycloak sem rotinas manuais, a API utiliza JIT Provisioning no `UserProvisioningFilter`:
- Ao autenticar com um JWT válido (`aud=taskflow-api`), o filtro exige que o token possua e-mail válido e verificado (`email_verified=true` quando aplicável na vinculação por e-mail).
- A vinculação automática de conta pré-existente por e-mail (case-insensitive) só ocorre quando o registro local ainda não possui `keycloakId` (`keycloakId == null`); em caso de colisão com outro usuário já vinculado, a API responde com `409 Conflict` (RFC 9457 `ProblemDetail`).
- Alterações de nome e e-mail verificado feitas no Console de Conta do Keycloak também são sincronizadas via `GET /user/me`.

# Como executar a aplicação via Docker (Linux)
Pré-requisitos:
* **Docker e Docker Compose V2**
* **Make**
* IDE (como **IntelliJ IDEA**) ou **Java 21** para desenvolvimento no host

### Comandos de Orquestração (`Makefile` — Desenvolvimento)
O `Makefile` gerencia o ambiente de desenvolvimento definido em `docker-compose.dev.yml`:
* `make run-dev`: Sobe todo o ambiente de desenvolvimento em containers (Bancos, Keycloak, Mailpit, PgAdmin e API).
* `make run-dev-infra`: Sobe Keycloak, Mailpit e PgAdmin (útil quando os bancos já estão rodando em separado).
* `make run-dev-infra-db`: Sobe os bancos (`db`, `keycloak_db`) e todas as ferramentas de infraestrutura (`keycloak`, `mailpit`, `pgadmin`) **sem a API**, ideal para rodar a API Spring Boot direto na IDE com Hot Reload.
* `make down`: Desliga os containers de desenvolvimento preservando os dados em volumes.
* `make down-db-all-dev`: Derruba todo o ambiente de desenvolvimento e apaga os volumes dos bancos.
* `make down-db-api-dev`: Apaga somente o container e o volume do banco da API de dev.
* `make down-db-keycloak-dev`: Apaga somente o container e o volume do banco do Keycloak de dev.
* `make logs-dev`: Acompanha os logs do ambiente de desenvolvimento em tempo real.
* `make clean-dev`: Remove containers e imagens de desenvolvimento.
* `make terminal-dev`: Abre um shell `bash` dentro do container `taskflow_api`.
* `make test`: Executa `./mvnw test`.

> **Nota sobre Produção:** O ambiente de produção é definido em `deploy/docker-compose.prod.yml` e implantado via pipeline GitHub Actions (`.github/workflows/ci-cd.yml`) + scripts em `deploy/scripts/`, com Keycloak 26 em modo `--optimized`, banco único PostgreSQL 16 com papéis de privilégio mínimo e Cloudflare Tunnel.

# Como executar a aplicação manualmente (Sem container da API)

1. Suba os bancos e o Keycloak de dev com:
   ```bash
   make run-dev-infra-db
   ```
   *(Ou aponte o `.env` para uma instância PostgreSQL 16 local na porta `5432` e crie o banco vazio; o Flyway aplica todas as migrations `V1`–`V13` automaticamente.)*
2. Configure o arquivo `.env` na raiz conforme o tópico **Configuração do `.env` para Desenvolvimento**.
3. Inicie a aplicação com Java 21:
   * **Via Terminal (Linux):**
     ```bash
     ./mvnw spring-boot:run
     ```
   * **Via Terminal (Windows):**
     ```cmd
     mvnw.cmd spring-boot:run
     ```
   * **Via IDE:** Carregue o `.env` nas variáveis de ambiente da configuração de execução da classe `ApiTaskflowApplication.java`.

# Acessos Locais (Ambiente de Desenvolvimento)
Após executar `make run-dev` ou `make run-dev-infra-db`, os seguintes serviços estarão disponíveis:
* **Keycloak (Painel Admin):** `http://localhost:9090` (usuário: `admin` | senha: `admin`)
* **Mailpit (Caixa de E-mails para Testes):** `http://localhost:8025` (captura e-mails de confirmação de conta, recuperação de senha e convites)
* **pgAdmin (Gerenciador de Banco de Dados):** `http://localhost:5050` (`admin@taskflow.com` | `admin`)
* **API Spring Boot:** `http://localhost:8080`

## Atualizar o client em um Keycloak já existente
O `realm-export.json` só é importado quando o realm ainda não existe. Quem já tinha o Keycloak local precisa preencher uma vez o **Root URL** e o **Home URL** do client `taskflow-frontend`, usados pelo tema para voltar ao TaskFlow após a confirmação de e-mail. Nenhum dado é apagado.
* **Pelo Painel Admin:** realm `taskflow-realm` → *Clients* → `taskflow-frontend` → *Settings* → *Root URL* e *Home URL* = `http://localhost:5173` → *Save*.
* **Via Terminal (Linux):** usa o administrador configurado no container.
  ```bash
  docker exec taskflow_keycloak bash -c '
  kc() { /opt/keycloak/bin/kcadm.sh "$@" --target-realm taskflow-realm --no-config --server http://localhost:8080 \
    --realm master --user "$KC_BOOTSTRAP_ADMIN_USERNAME" --password "$KC_BOOTSTRAP_ADMIN_PASSWORD"; }
  CID=$(kc get clients -q clientId=taskflow-frontend --fields id --format csv --noquotes) &&
  kc update "clients/$CID" -s rootUrl=http://localhost:5173 -s baseUrl=http://localhost:5173'
  ```

# Como executar Testes Automatizados

O projeto possui:
* **Testes Unitários (`unit/`):** JUnit 5, Mockito, AssertJ, MockMvc e Spring Security Test cobrindo regras de negócio, controllers, segurança JWT/audiência/JIT, `ProblemDetail`, e-mails assíncronos e expurgo agendado.
* **Testes de Integração (`integration/`):** Executados contra **PostgreSQL 16 real via Testcontainers**, validando isolamento multi-tenant (matriz BOLA), migrações Flyway (`V1`–`V13`), privilégio mínimo, avatares e expiração de convites.

### Via Terminal (Maven Wrapper — JDK 21)
* **Suíte completa — unitários + integração (Linux):** os testes com Testcontainers são ignorados se o Docker não estiver ativo.
  ```bash
  ./mvnw test
  ```
* **Somente unitários (Linux):**
  ```bash
  ./mvnw test -Dtest='unievangelica/taskflow/api/test/unit/**'
  ```
* **Suíte completa via Makefile (Linux):**
  ```bash
  make test
  ```

# Documentação da API (Swagger)
Em ambiente de desenvolvimento, a API gera sua documentação OpenAPI 3 no **Swagger UI** em `http://localhost:8080/swagger-ui/index.html`.
1. Clique em **Authorize** no canto superior direito.
2. Em **OAuth2**, mantenha o `client_id` como `taskflow-frontend` e clique em **Authorize**.
3. Autentique-se no Keycloak para que o Swagger passe a injetar automaticamente o token JWT com audiência `taskflow-api` nas requisições de teste.
