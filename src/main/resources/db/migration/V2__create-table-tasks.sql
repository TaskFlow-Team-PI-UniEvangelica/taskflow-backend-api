CREATE TYPE status_tarefa AS ENUM ('pendente', 'em_andamento', 'concluida');
CREATE TYPE prioridade_tarefa AS ENUM ('baixa', 'media', 'alta');
CREATE TABLE tasks (
    id SERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT,
    status status_tarefa NULL DEFAULT 'pendente',
    prioridade prioridade_tarefa NOT NULL DEFAULT 'media',
    prazo DATE,
    id_criador INT,
    CONSTRAINT fk_tasks_users
        FOREIGN KEY (id_criador)
        REFERENCES users (id)
        ON DELETE SET NULL
);