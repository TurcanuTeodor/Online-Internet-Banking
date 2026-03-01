-- V3__Create_refresh_tokens.sql
-- Refresh tokens for JWT session management

CREATE TABLE IF NOT EXISTS "REFRESH_TOKENS" (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(2048) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expiry_date TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES "USER"(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON "REFRESH_TOKENS"(token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON "REFRESH_TOKENS"(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_valid ON "REFRESH_TOKENS"(user_id, revoked_at, expiry_date);
