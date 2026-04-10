-- V4__Add_step_up_required_to_fraud_status_enum.sql
-- Adds STEP_UP_REQUIRED to the existing PostgreSQL enum if missing.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_type t
        JOIN pg_namespace n ON t.typnamespace = n.oid
        JOIN pg_enum e ON e.enumtypid = t.oid
        WHERE n.nspname = 'fraud'
          AND t.typname = 'fraud_decision_status_enum'
          AND e.enumlabel = 'STEP_UP_REQUIRED'
    ) THEN
        ALTER TYPE fraud.fraud_decision_status_enum ADD VALUE 'STEP_UP_REQUIRED';
    END IF;
END$$;