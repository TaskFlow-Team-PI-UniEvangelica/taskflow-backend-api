CREATE TABLE tb_organization_invites (
    id SERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    inviter_id BIGINT NOT NULL,
    invited_email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_org_invite_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_org_invite_inviter FOREIGN KEY (inviter_id) REFERENCES users(id) ON DELETE CASCADE
);
