-- ALT-31: avatar em tabela própria.
-- Na tabela users, a coluna bytea era lida em toda consulta de usuário (o @Basic(fetch = LAZY)
-- não tem efeito sem bytecode enhancement). Separado, o avatar só é lido quando pedido.

CREATE TABLE user_avatars (
    user_id      INT          PRIMARY KEY,
    content      BYTEA        NOT NULL,
    content_type VARCHAR(50)  NOT NULL,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_avatars_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Move os avatares existentes (o tipo era detectado no upload; image/jpeg só como garantia)
INSERT INTO user_avatars (user_id, content, content_type)
SELECT id, avatar, COALESCE(avatar_type, 'image/jpeg')
FROM users
WHERE avatar IS NOT NULL;

ALTER TABLE users DROP COLUMN avatar;
ALTER TABLE users DROP COLUMN avatar_type;
