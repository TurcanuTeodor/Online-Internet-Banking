-- V1__Create_transaction_enums.sql
-- Enum types for the transactions schema (schema-qualified check to avoid public schema collision)

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_type t JOIN pg_namespace n ON t.typnamespace = n.oid
        WHERE t.typname = 'transaction_type_enum' AND n.nspname = 'transactions'
    ) THEN
        CREATE TYPE TRANSACTION_TYPE_ENUM AS ENUM (
            'DEPOSIT',
            'WITHDRAWAL',
            'TRANSFER_INTERNAL',
            'TRANSFER_EXTERNAL'
        );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_type t JOIN pg_namespace n ON t.typnamespace = n.oid
        WHERE t.typname = 'transaction_category_enum' AND n.nspname = 'transactions'
    ) THEN
        CREATE TYPE TRANSACTION_CATEGORY_ENUM AS ENUM (
            'FOOD',
            'GROCERIES',
            'TRANSPORT',
            'SHOPPING',
            'ENTERTAINMENT',
            'HEALTH',
            'TRAVEL',
            'SUBSCRIPTIONS',
            'INCOME',
            'OTHERS'
        );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_type t JOIN pg_namespace n ON t.typnamespace = n.oid
        WHERE t.typname = 'currency_enum' AND n.nspname = 'transactions'
    ) THEN
        CREATE TYPE CURRENCY_ENUM AS ENUM (
            'EUR',
            'USD',
            'RON',
            'GBP'
        );
    END IF;
END$$;
