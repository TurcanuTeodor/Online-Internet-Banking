-- V1__Create_enums.sql
-- Only the enum needed by auth-service

DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_type t
        JOIN pg_namespace n ON t.typnamespace = n.oid
        WHERE t.typname = 'role_enum' AND n.nspname = 'auth'
    ) THEN
        CREATE TYPE ROLE_ENUM AS ENUM ('USER', 'ADMIN');
    END IF;
END $$;
