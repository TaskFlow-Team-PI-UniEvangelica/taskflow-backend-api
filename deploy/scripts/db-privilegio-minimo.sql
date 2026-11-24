-- =============================================================================
-- ALT-30: papéis de banco com privilégio mínimo para a API do TaskFlow
-- =============================================================================
-- Executar como superusuário, conectado ao banco da API, via psql (usa variáveis do psql):
--
--   psql -U <superusuario> -d <banco_api> \
--        -v app_db=<banco_api> -v kc_db=<banco_keycloak> -v kc_user=<usuario_keycloak> \
--        -v owner_user=<dono> -v owner_pass=<senha> -v app_user=<api> -v app_pass=<senha> \
--        -f db-privilegio-minimo.sql
--
-- Idempotente: pode ser executado mais de uma vez (também redefine as senhas informadas).
--
-- Resultado:
--   owner_user  dono do banco, do schema public e dos objetos da aplicação; sem superusuário.
--               Usado apenas pelo Flyway (spring.flyway.user) para aplicar migrations.
--   app_user    só SELECT/INSERT/UPDATE/DELETE nas tabelas e uso das sequências; sem DDL e sem
--               acesso ao flyway_schema_history. Usado pela API em execução.
--   O banco da API e o do Keycloak deixam de aceitar conexão de PUBLIC (cada um só do seu dono/usuário).
-- =============================================================================

\set ON_ERROR_STOP on

-- 1. Papéis (cria se não existirem) sem superusuário e sem poderes administrativos
SELECT format('CREATE ROLE %I LOGIN', :'owner_user')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'owner_user') \gexec
SELECT format('CREATE ROLE %I LOGIN', :'app_user')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'app_user') \gexec

SELECT format('ALTER ROLE %I WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD %L',
              :'owner_user', :'owner_pass') \gexec
SELECT format('ALTER ROLE %I WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS NOINHERIT PASSWORD %L',
              :'app_user', :'app_pass') \gexec

-- 2. Banco da API: dono = owner_user; conexão só para o dono e a API
SELECT format('ALTER DATABASE %I OWNER TO %I', :'app_db', :'owner_user') \gexec
SELECT format('REVOKE ALL ON DATABASE %I FROM PUBLIC', :'app_db') \gexec
SELECT format('GRANT CONNECT, TEMPORARY ON DATABASE %I TO %I', :'app_db', :'app_user') \gexec

-- 3. Banco do Keycloak (se existir): conexão só para o usuário do Keycloak
SELECT format('REVOKE ALL ON DATABASE %I FROM PUBLIC', :'kc_db')
WHERE EXISTS (SELECT 1 FROM pg_database WHERE datname = :'kc_db') \gexec
SELECT format('GRANT CONNECT, TEMPORARY ON DATABASE %I TO %I', :'kc_db', :'kc_user')
WHERE EXISTS (SELECT 1 FROM pg_database WHERE datname = :'kc_db')
  AND EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'kc_user') \gexec

-- 4. Schema public: dono = owner_user; ninguém mais cria objetos nele
SELECT format('ALTER SCHEMA public OWNER TO %I', :'owner_user') \gexec
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
SELECT format('GRANT USAGE ON SCHEMA public TO %I', :'app_user') \gexec

-- 5. Posse dos objetos existentes da aplicação (tabelas, views, sequências avulsas, tipos e funções).
--    Não usa REASSIGN OWNED: o superusuário de bootstrap também é dono de objetos do sistema.
SELECT set_config('taskflow.owner_user', :'owner_user', false);

DO $$
DECLARE
    dono text := current_setting('taskflow.owner_user');
    obj record;
BEGIN
    -- Tabelas, views e materialized views (sequências ligadas a colunas acompanham a tabela)
    FOR obj IN
        SELECT c.relname, c.relkind
        FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p', 'v', 'm', 'f')
          AND pg_get_userbyid(c.relowner) <> dono
    LOOP
        EXECUTE format('ALTER %s public.%I OWNER TO %I',
                       CASE obj.relkind WHEN 'v' THEN 'VIEW' WHEN 'm' THEN 'MATERIALIZED VIEW'
                                        WHEN 'f' THEN 'FOREIGN TABLE' ELSE 'TABLE' END,
                       obj.relname, dono);
    END LOOP;

    -- Sequências que não pertencem a uma coluna (as de serial/identity mudam junto com a tabela)
    FOR obj IN
        SELECT c.relname
        FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'public' AND c.relkind = 'S'
          AND pg_get_userbyid(c.relowner) <> dono
          AND NOT EXISTS (SELECT 1 FROM pg_depend d
                          WHERE d.objid = c.oid AND d.classid = 'pg_class'::regclass
                            AND d.deptype IN ('a', 'i'))
    LOOP
        EXECUTE format('ALTER SEQUENCE public.%I OWNER TO %I', obj.relname, dono);
    END LOOP;

    -- Tipos (enums e domains) da aplicação
    FOR obj IN
        SELECT t.typname
        FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace
        WHERE n.nspname = 'public' AND t.typtype IN ('e', 'd')
          AND pg_get_userbyid(t.typowner) <> dono
    LOOP
        EXECUTE format('ALTER TYPE public.%I OWNER TO %I', obj.typname, dono);
    END LOOP;

    -- Funções e procedures da aplicação
    FOR obj IN
        SELECT p.oid::regprocedure AS assinatura
        FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
        WHERE n.nspname = 'public' AND pg_get_userbyid(p.proowner) <> dono
    LOOP
        EXECUTE format('ALTER ROUTINE %s OWNER TO %I', obj.assinatura, dono);
    END LOOP;
END
$$;

-- 6. Privilégios da API: só manipulação de dados
SELECT format('GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO %I', :'app_user') \gexec
SELECT format('GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO %I', :'app_user') \gexec

-- O histórico do Flyway é só do dono (a API não altera o registro de migrations)
SELECT format('REVOKE ALL ON TABLE public.flyway_schema_history FROM %I', :'app_user')
WHERE to_regclass('public.flyway_schema_history') IS NOT NULL \gexec

-- 7. Objetos criados no futuro pelo Flyway (como owner_user) já nascem com os privilégios da API
SELECT format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %I',
              :'owner_user', :'app_user') \gexec
SELECT format('ALTER DEFAULT PRIVILEGES FOR ROLE %I IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO %I',
              :'owner_user', :'app_user') \gexec

\echo 'Privilégio mínimo aplicado com sucesso.'
