-- Adicionando configurações dinâmicas na organização
ALTER TABLE organizations ADD COLUMN members_can_delete_tasks BOOLEAN DEFAULT false NOT NULL;
ALTER TABLE organizations ADD COLUMN members_can_change_deadlines BOOLEAN DEFAULT false NOT NULL;
ALTER TABLE organizations ADD COLUMN members_can_return_tasks BOOLEAN DEFAULT false NOT NULL;
ALTER TABLE organizations ADD COLUMN members_can_create_tasks BOOLEAN DEFAULT true NOT NULL;

-- Adicionando o cargo (Role) do usuário na organização
ALTER TABLE organization_users ADD COLUMN role VARCHAR(50) DEFAULT 'MEMBER' NOT NULL;

-- Criando tabela de histórico de tarefas (Task Audit History)
CREATE TABLE task_history (
    id SERIAL PRIMARY KEY,
    task_id INT NOT NULL,
    user_id INT NOT NULL,
    action_type VARCHAR(50) NOT NULL, -- CREATED, MOVED, EDITED, RETURNED, DELETED
    old_value TEXT,
    new_value TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_history_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE,
    CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
