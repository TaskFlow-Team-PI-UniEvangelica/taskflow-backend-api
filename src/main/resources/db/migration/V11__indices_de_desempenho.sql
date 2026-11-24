-- ALT-27: índices para as consultas de pertencimento e de convites.
-- Somente cria índices (não altera dados). As chaves primárias de organization_users e
-- task_responsibles começam pela organização/tarefa e não atendem às buscas por usuário.

-- Organizações de um usuário, bloqueio de exclusão de OWNER e organização em comum (avatar)
CREATE INDEX IF NOT EXISTS idx_org_users_user_id ON organization_users (user_id);

-- Tarefas em que o usuário é responsável e remoção em cascata ao excluir o usuário
CREATE INDEX IF NOT EXISTS idx_task_resp_user_id ON task_responsibles (user_id);

-- Convites pendentes por e-mail: o Spring Data gera upper(invited_email) nas buscas IgnoreCase
CREATE INDEX IF NOT EXISTS idx_invites_upper_email_status ON tb_organization_invites (upper(invited_email), status);
