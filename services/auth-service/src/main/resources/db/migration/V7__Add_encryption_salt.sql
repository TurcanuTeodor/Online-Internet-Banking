-- V7__Add_encryption_salt.sql
-- Per-user salt for client-derived encryption (Privacy-by-Design)
-- The encryption key is derived from the user's password + this salt via PBKDF2.
-- The salt is NOT secret — it prevents rainbow-table attacks on the derived key.

ALTER TABLE "USER" ADD COLUMN encryption_salt VARCHAR(64);

-- Backfill existing users with unique placeholder salts (no pgcrypto needed).
-- The application generates cryptographic salts at register/login; these are
-- deterministic placeholders so the NOT NULL constraint can be applied.
UPDATE "USER" SET encryption_salt = encode(('seed-salt-' || id || '-cashtactics')::bytea, 'base64')
    WHERE encryption_salt IS NULL;

-- Make it NOT NULL after backfill
ALTER TABLE "USER" ALTER COLUMN encryption_salt SET NOT NULL;
