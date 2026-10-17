-- Evolução da tabela task_history para preservar auditoria após exclusão de tarefas/usuários
-- e suportar consulta de histórico por organização.

ALTER TABLE task_history ADD COLUMN organization_id INT;
ALTER TABLE task_history ADD COLUMN task_title VARCHAR(255);

-- Backfill dos registros existentes a partir da tabela tasks
UPDATE task_history th
SET organization_id = t.organization_id,
    task_title = t.titulo
FROM tasks t
WHERE th.task_id = t.id;

-- Permitir que task_id e user_id fiquem NULL caso a tarefa ou o usuário sejam removidos no futuro
ALTER TABLE task_history ALTER COLUMN task_id DROP NOT NULL;
ALTER TABLE task_history ALTER COLUMN user_id DROP NOT NULL;

-- Substituir FKs com ON DELETE CASCADE por ON DELETE SET NULL para preservar o histórico
ALTER TABLE task_history DROP CONSTRAINT IF EXISTS fk_history_task;
ALTER TABLE task_history ADD CONSTRAINT fk_history_task
    FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE SET NULL;

ALTER TABLE task_history DROP CONSTRAINT IF EXISTS fk_history_user;
ALTER TABLE task_history ADD CONSTRAINT fk_history_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL;

-- Vincular o histórico à organização (se a organização inteira for excluída, limpa o histórico dela)
ALTER TABLE task_history ADD CONSTRAINT fk_history_organization
    FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE;

-- Índices para acelerar a busca de linha do tempo por tarefa e por organização
CREATE INDEX IF NOT EXISTS idx_task_history_task_id ON task_history (task_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_task_history_org_id ON task_history (organization_id, timestamp DESC);
