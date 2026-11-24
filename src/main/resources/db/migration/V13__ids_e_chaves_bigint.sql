-- Alinha os IDs e as chaves estrangeiras com as entidades JPA (Long = BIGINT).
-- As migrations antigas criaram essas colunas como INT/SERIAL; com
-- spring.jpa.hibernate.ddl-auto=validate (profile prod) a aplicação não iniciava
-- ("wrong column type ... found int4, but expecting bigint").
-- Os valores não mudam; cada ALTER reescreve a tabela (bloqueio exclusivo por poucos segundos
-- com o volume atual). tb_organization_invites.organization_id/inviter_id já eram BIGINT.

ALTER TABLE users                   ALTER COLUMN id TYPE BIGINT;
ALTER TABLE organizations           ALTER COLUMN id TYPE BIGINT;
ALTER TABLE tasks                   ALTER COLUMN id TYPE BIGINT,
                                    ALTER COLUMN id_criador TYPE BIGINT,
                                    ALTER COLUMN organization_id TYPE BIGINT;
ALTER TABLE organization_users      ALTER COLUMN organization_id TYPE BIGINT,
                                    ALTER COLUMN user_id TYPE BIGINT;
ALTER TABLE task_responsibles       ALTER COLUMN task_id TYPE BIGINT,
                                    ALTER COLUMN user_id TYPE BIGINT;
ALTER TABLE task_history            ALTER COLUMN id TYPE BIGINT,
                                    ALTER COLUMN organization_id TYPE BIGINT,
                                    ALTER COLUMN task_id TYPE BIGINT,
                                    ALTER COLUMN user_id TYPE BIGINT;
ALTER TABLE tb_organization_invites ALTER COLUMN id TYPE BIGINT;
ALTER TABLE user_avatars            ALTER COLUMN user_id TYPE BIGINT;

-- As sequências dos SERIAL continuam limitadas a INT se não forem alteradas
ALTER SEQUENCE users_id_seq                   AS BIGINT;
ALTER SEQUENCE organizations_id_seq           AS BIGINT;
ALTER SEQUENCE tasks_id_seq                   AS BIGINT;
ALTER SEQUENCE task_history_id_seq            AS BIGINT;
ALTER SEQUENCE tb_organization_invites_id_seq AS BIGINT;
