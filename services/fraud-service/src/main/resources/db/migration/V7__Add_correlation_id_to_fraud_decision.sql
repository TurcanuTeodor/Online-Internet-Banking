-- V7__Add_correlation_id_to_fraud_decision.sql
-- Adds a correlation id to link pre-transaction fraud decisions with upstream flow requests.

ALTER TABLE fraud."FRAUD_DECISION"
    ADD COLUMN IF NOT EXISTS "CORRELATION_ID" VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_fd_correlation
    ON fraud."FRAUD_DECISION" ("CORRELATION_ID");
