CREATE TABLE organizations (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela que vincula usuários a uma ou mais organizações (Multi-tenant)
CREATE TABLE organization_users (
    organization_id INT NOT NULL,
    user_id INT NOT NULL,
    PRIMARY KEY (organization_id, user_id),
    CONSTRAINT fk_ou_org FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT fk_ou_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Isolando as tarefas por organização
ALTER TABLE tasks ADD COLUMN organization_id INT;
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_org FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE;

-- Expurgo das credenciais e adição do elo com Keycloak
ALTER TABLE users DROP COLUMN senha;
ALTER TABLE users DROP COLUMN cargo;
ALTER TABLE users ADD COLUMN keycloak_id VARCHAR(255) UNIQUE;