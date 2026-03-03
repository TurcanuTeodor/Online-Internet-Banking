-- V1__Create_payment_enums.sql
-- Enum types for the payments schema

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_type t JOIN pg_namespace n ON t.typnamespace = n.oid
        WHERE t.typname = 'payment_status_enum' AND n.nspname = 'payments'
    ) THEN
        CREATE TYPE PAYMENT_STATUS_ENUM AS ENUM (
            'PENDING',
            'COMPLETED',
            'FAILED',
            'REFUNDED'
        );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_type t JOIN pg_namespace n ON t.typnamespace = n.oid
        WHERE t.typname = 'currency_enum' AND n.nspname = 'payments'
    ) THEN
        CREATE TYPE CURRENCY_ENUM AS ENUM (
            'EUR',
            'USD',
            'RON',
            'GBP'
        );
    END IF;
END$$;
