-- V1__Create_fraud_enums.sql
-- Enum types for the fraud schema

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_type t JOIN pg_namespace n ON t.typnamespace = n.oid
        WHERE t.typname = 'fraud_decision_status_enum' AND n.nspname = 'fraud'
    ) THEN
        CREATE TYPE FRAUD_DECISION_STATUS_ENUM AS ENUM (
            'ALLOW',
            'FLAG',
            'BLOCK',
            'MANUAL_REVIEW'
        );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_type t JOIN pg_namespace n ON t.typnamespace = n.oid
        WHERE t.typname = 'fraud_tier_enum' AND n.nspname = 'fraud'
    ) THEN
        CREATE TYPE FRAUD_TIER_ENUM AS ENUM (
            'TIER1_RULES',
            'TIER2_BEHAVIORAL',
            'TIER3_LLM'
        );
    END IF;
END$$;
