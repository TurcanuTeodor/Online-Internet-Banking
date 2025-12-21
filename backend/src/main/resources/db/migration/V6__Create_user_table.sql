-- V6__Create_user_table.sql

CREATE TABLE "USER" (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL UNIQUE,
    username_or_email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ROLE_ENUM NOT NULL DEFAULT 'USER',
    two_factor_enabled BOOLEAN NOT NULL DEFAULT false,
    two_factor_secret VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES "CLIENT"(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_username_or_email ON "USER"(username_or_email);
CREATE INDEX idx_user_role ON "USER"(role);
CREATE INDEX idx_user_two_factor_enabled ON "USER"(two_factor_enabled);
