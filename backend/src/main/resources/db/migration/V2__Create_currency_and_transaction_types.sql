-- V2__Create_currency_and_transaction_types.sql

CREATE TABLE "CURRENCY_TYPE" (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "TRANSACTION_TYPE" (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert default currencies
INSERT INTO "CURRENCY_TYPE" (code, name) VALUES
    ('EUR', 'Euro'),
    ('USD', 'US Dollar'),
    ('RON', 'Romanian Leu'),
    ('GBP', 'British Pound');

-- Insert default transaction types (short codes)
INSERT INTO "TRANSACTION_TYPE" (code, name) VALUES
    ('DEP', 'Deposit'),
    ('RET', 'Withdrawal'),
    ('TRF', 'Transfer');
