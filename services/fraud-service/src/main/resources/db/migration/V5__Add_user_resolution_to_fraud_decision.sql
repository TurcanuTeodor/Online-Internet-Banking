-- V5__Add_user_resolution_to_fraud_decision.sql
-- Stores client-side fraud alert acknowledgements and resolution timestamps.

ALTER TABLE "FRAUD_DECISION"
    ADD COLUMN IF NOT EXISTS "USER_RESOLUTION" VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS "USER_RESOLUTION_NOTES" TEXT,
    ADD COLUMN IF NOT EXISTS "USER_RESOLVED_AT" TIMESTAMP;