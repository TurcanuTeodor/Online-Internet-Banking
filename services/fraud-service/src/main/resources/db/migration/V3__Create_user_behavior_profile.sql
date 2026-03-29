-- V3__Create_user_behavior_profile.sql
-- Aggregated behavioral features per client (updated after every transaction)

CREATE TABLE IF NOT EXISTS "USER_BEHAVIOR_PROFILE" (
    "ID"                        BIGSERIAL       PRIMARY KEY,
    "CLIENT_ID"                 BIGINT          NOT NULL UNIQUE,
    "AVG_TRANSACTION_AMOUNT"    DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    "MAX_TRANSACTION_AMOUNT"    DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    "TRANSACTION_COUNT"         BIGINT          NOT NULL DEFAULT 0,
    "AVG_DAILY_TRANSACTIONS"    DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    "TYPICAL_HOUR_START"        INT             NOT NULL DEFAULT 8,
    "TYPICAL_HOUR_END"          INT             NOT NULL DEFAULT 22,
    "COMMON_IBANS"              TEXT,
    "LAST_UPDATED"              TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ubp_client ON "USER_BEHAVIOR_PROFILE" ("CLIENT_ID");
