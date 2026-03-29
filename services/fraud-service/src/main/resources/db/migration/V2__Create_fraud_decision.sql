-- V2__Create_fraud_decision.sql
-- Central table: one row per evaluated transaction

CREATE TABLE IF NOT EXISTS "FRAUD_DECISION" (
    "ID"                    BIGSERIAL       PRIMARY KEY,
    "TRANSACTION_ID"        BIGINT,
    "ACCOUNT_ID"            BIGINT          NOT NULL,
    "CLIENT_ID"             BIGINT          NOT NULL,
    "STATUS"                FRAUD_DECISION_STATUS_ENUM NOT NULL DEFAULT 'ALLOW',
    "DECIDED_BY_TIER"       FRAUD_TIER_ENUM NOT NULL DEFAULT 'TIER1_RULES',
    "RISK_SCORE"            DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    "RULE_HITS"             TEXT,
    "EXPLANATION"           TEXT,
    "REVIEWED_BY_ADMIN"     VARCHAR(255),
    "ADMIN_NOTES"           TEXT,
    "CREATED_AT"            TIMESTAMP       NOT NULL DEFAULT NOW(),
    "UPDATED_AT"            TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fd_transaction ON "FRAUD_DECISION" ("TRANSACTION_ID");
CREATE INDEX IF NOT EXISTS idx_fd_client      ON "FRAUD_DECISION" ("CLIENT_ID");
CREATE INDEX IF NOT EXISTS idx_fd_status      ON "FRAUD_DECISION" ("STATUS");
