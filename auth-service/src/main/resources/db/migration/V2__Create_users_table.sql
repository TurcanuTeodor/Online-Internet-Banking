-- V2__Create_users_table.sql
-- User table for auth-service (no FK to CLIENT — that lives in user-service)

CREATE TABLE IF NOT EXISTS "USER" (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL UNIQUE,
    username_or_email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ROLE_ENUM NOT NULL DEFAULT 'USER',
    two_factor_enabled BOOLEAN NOT NULL DEFAULT false,
    two_factor_secret VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- NOTE: No FOREIGN KEY (client_id) REFERENCES "CLIENT"(id)
-- In distributed arch, CLIENT table is owned by user-service.
-- client_id is just a reference ID, validated via HTTP call if needed.

CREATE INDEX IF NOT EXISTS idx_user_username_email ON "USER"(username_or_email);
CREATE INDEX IF NOT EXISTS idx_user_role ON "USER"(role);
