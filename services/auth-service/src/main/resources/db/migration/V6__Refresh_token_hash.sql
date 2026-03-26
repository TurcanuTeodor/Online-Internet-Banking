-- Store only SHA-256 (Base64) hash of refresh JWT; existing refresh sessions are cleared.
DELETE FROM auth."REFRESH_TOKENS";

ALTER TABLE auth."REFRESH_TOKENS" DROP COLUMN token;

ALTER TABLE auth."REFRESH_TOKENS" ADD COLUMN token_hash VARCHAR(100) NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_refresh_tokens_token_hash ON auth."REFRESH_TOKENS"(token_hash);
