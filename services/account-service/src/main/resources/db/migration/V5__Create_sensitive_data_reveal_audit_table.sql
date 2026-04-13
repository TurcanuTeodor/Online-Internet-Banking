-- V5__Create_sensitive_data_reveal_audit_table.sql
-- Persistent report data for admin sensitive-data reveal audits.

CREATE TABLE "SENSITIVE_DATA_REVEAL_AUDIT" (
    id BIGSERIAL PRIMARY KEY,
    actor_username VARCHAR(100) NOT NULL,
    actor_client_id BIGINT,
    actor_role VARCHAR(30) NOT NULL,
    scope VARCHAR(80) NOT NULL,
    target_type VARCHAR(80) NOT NULL,
    target_id VARCHAR(120) NOT NULL,
    reason_code VARCHAR(60) NOT NULL,
    reason_details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sensitive_reveal_audit_created_at ON "SENSITIVE_DATA_REVEAL_AUDIT"(created_at DESC);
CREATE INDEX idx_sensitive_reveal_audit_reason_code ON "SENSITIVE_DATA_REVEAL_AUDIT"(reason_code);
