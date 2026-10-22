-- V9: Governança de exclusão de tarefas próprias, retenção de histórico de excluídas (configurável pelo OWNER)
-- e numeração sequencial imutável de tarefas por organização (1, 2, 3...)

-- 1. Novas colunas em organizations
ALTER TABLE organizations ADD COLUMN members_can_delete_own_tasks BOOLEAN DEFAULT false NOT NULL;
ALTER TABLE organizations ADD COLUMN deleted_tasks_retention_days INT DEFAULT 7;
ALTER TABLE organizations ADD COLUMN last_task_number INT DEFAULT 0 NOT NULL;

-- 2. Adicionar task_number por organização na tabela tasks
ALTER TABLE tasks ADD COLUMN task_number INT;

WITH numbered_tasks AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY organization_id ORDER BY id ASC) AS seq_num
    FROM tasks
)
UPDATE tasks t
SET task_number = nt.seq_num
FROM numbered_tasks nt
WHERE t.id = nt.id;

UPDATE tasks SET task_number = 1 WHERE task_number IS NULL;
ALTER TABLE tasks ALTER COLUMN task_number SET NOT NULL;
ALTER TABLE tasks ALTER COLUMN task_number SET DEFAULT 1;

-- Atualizar o contador last_task_number de cada organização com o maior número já emitido
UPDATE organizations o
SET last_task_number = COALESCE(
    (SELECT MAX(t.task_number) FROM tasks t WHERE t.organization_id = o.id),
    0
);

ALTER TABLE tasks ADD CONSTRAINT uq_tasks_org_task_number UNIQUE (organization_id, task_number);

-- 3. Adicionar snapshot de task_number na tabela task_history para preservar o número mesmo após exclusão
ALTER TABLE task_history ADD COLUMN task_number INT;

UPDATE task_history th
SET task_number = t.task_number
FROM tasks t
WHERE th.task_id = t.id;

CREATE INDEX IF NOT EXISTS idx_task_history_org_action_ts
    ON task_history (organization_id, action_type, timestamp DESC);
