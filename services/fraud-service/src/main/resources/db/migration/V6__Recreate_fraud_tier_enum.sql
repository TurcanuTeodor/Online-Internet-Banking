-- V6__Recreate_fraud_tier_enum.sql
-- Drop the existing DECIDED_BY_TIER column and the FRAUD_TIER_ENUM type
-- Recreate the enum without TIER3_LLM, since the DB is fresh and it's no longer used
-- Add the DECIDED_BY_TIER column back with the new enum type

ALTER TABLE fraud."FRAUD_DECISION" DROP COLUMN IF EXISTS "DECIDED_BY_TIER";
DROP TYPE IF EXISTS fraud.FRAUD_TIER_ENUM;

CREATE TYPE fraud.FRAUD_TIER_ENUM AS ENUM (
    'TIER1_RULES',
    'TIER2_BEHAVIORAL',
    'TIER3_ML'
);

ALTER TABLE fraud."FRAUD_DECISION"
    ADD COLUMN "DECIDED_BY_TIER" fraud.FRAUD_TIER_ENUM NOT NULL DEFAULT 'TIER1_RULES';
