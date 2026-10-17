-- ============================================================================
-- V10: Governança de Edição e Movimentação de Tarefas para Membros (MEMBER)
-- ============================================================================

ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS members_can_edit_all_tasks BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS members_can_edit_own_or_assigned_tasks BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS members_can_move_all_tasks BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS members_can_move_own_or_assigned_tasks BOOLEAN NOT NULL DEFAULT TRUE;

-- Garantir exclusividade mútua inicial caso algum banco possua estados customizados
UPDATE organizations
SET members_can_edit_own_or_assigned_tasks = FALSE
WHERE members_can_edit_all_tasks = TRUE;

UPDATE organizations
SET members_can_move_own_or_assigned_tasks = FALSE
WHERE members_can_move_all_tasks = TRUE;

-- Garantir que retornar tarefas concluídas só fique ativo se ao menos uma opção de mover estiver ativa
UPDATE organizations
SET members_can_return_tasks = FALSE
WHERE members_can_move_all_tasks = FALSE
  AND members_can_move_own_or_assigned_tasks = FALSE;
