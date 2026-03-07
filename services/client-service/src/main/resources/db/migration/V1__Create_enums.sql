-- V1__Create_enums.sql
-- Enums owned by user-service (clients schema)

DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_type t
        JOIN pg_namespace n ON t.typnamespace = n.oid
        WHERE t.typname = 'client_type_enum' AND n.nspname = 'clients'
    ) THEN
        CREATE TYPE CLIENT_TYPE_ENUM AS ENUM ('PF', 'PJ');
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_type t
        JOIN pg_namespace n ON t.typnamespace = n.oid
        WHERE t.typname = 'sex_type_enum' AND n.nspname = 'clients'
    ) THEN
        CREATE TYPE SEX_TYPE_ENUM AS ENUM ('M', 'F', 'O');
    END IF;
END $$;
