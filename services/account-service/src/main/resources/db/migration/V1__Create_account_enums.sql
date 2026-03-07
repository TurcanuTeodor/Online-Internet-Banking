-- V1__Create_account_enums.sql
-- Owned by account-service (schema: accounts)

CREATE TYPE CURRENCY_ENUM AS ENUM ('EUR', 'USD', 'RON', 'GBP');
CREATE TYPE ACCOUNT_STATUS_ENUM AS ENUM ('ACTIVE', 'CLOSED', 'SUSPENDED');
