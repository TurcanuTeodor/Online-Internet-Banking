-- V12__Convert_types_to_enums.sql
-- Replace lookup tables with native enum columns

CREATE TYPE CLIENT_TYPE_ENUM AS ENUM ('PF', 'PJ');
CREATE TYPE SEX_TYPE_ENUM AS ENUM ('M', 'F', 'O');
CREATE TYPE CURRENCY_ENUM AS ENUM ('EUR', 'USD', 'RON', 'GBP');
CREATE TYPE TRANSACTION_TYPE_ENUM AS ENUM ('DEP', 'RET', 'TRF');

-- CLIENT: convert client_type_id, sex_type_id -> enum columns
ALTER TABLE "CLIENT" ADD COLUMN client_type CLIENT_TYPE_ENUM;
ALTER TABLE "CLIENT" ADD COLUMN sex_type SEX_TYPE_ENUM;

UPDATE "CLIENT" c
SET client_type = ct.code::CLIENT_TYPE_ENUM,
    sex_type = st.code::SEX_TYPE_ENUM
FROM "CLIENT_TYPE" ct, "SEX_TYPE" st
WHERE c.client_type_id = ct.id
  AND c.sex_type_id = st.id;

ALTER TABLE "CLIENT" ALTER COLUMN client_type SET NOT NULL;
ALTER TABLE "CLIENT" ALTER COLUMN sex_type SET NOT NULL;

ALTER TABLE "CLIENT" DROP COLUMN client_type_id CASCADE;
ALTER TABLE "CLIENT" DROP COLUMN sex_type_id CASCADE;

-- ACCOUNT: convert currency_type_id -> enum column
ALTER TABLE "ACCOUNT" ADD COLUMN currency_code CURRENCY_ENUM;

UPDATE "ACCOUNT" a
SET currency_code = cur.code::CURRENCY_ENUM
FROM "CURRENCY_TYPE" cur
WHERE a.currency_type_id = cur.id;

ALTER TABLE "ACCOUNT" ALTER COLUMN currency_code SET NOT NULL;
ALTER TABLE "ACCOUNT" DROP COLUMN currency_type_id CASCADE;

-- TRANSACTION: convert transaction_type_id + original_currency_type_id -> enum columns
ALTER TABLE "TRANSACTION" ADD COLUMN transaction_type_code TRANSACTION_TYPE_ENUM;
ALTER TABLE "TRANSACTION" ADD COLUMN original_currency_code CURRENCY_ENUM;

UPDATE "TRANSACTION" t
SET transaction_type_code = tt.code::TRANSACTION_TYPE_ENUM
FROM "TRANSACTION_TYPE" tt
WHERE t.transaction_type_id = tt.id;

UPDATE "TRANSACTION" t
SET original_currency_code = cur.code::CURRENCY_ENUM
FROM "CURRENCY_TYPE" cur
WHERE t.original_currency_type_id = cur.id;

ALTER TABLE "TRANSACTION" ALTER COLUMN transaction_type_code SET NOT NULL;
ALTER TABLE "TRANSACTION" DROP COLUMN transaction_type_id CASCADE;
ALTER TABLE "TRANSACTION" DROP COLUMN original_currency_type_id CASCADE;

-- Drop lookup tables
DROP TABLE "TRANSACTION_TYPE";
DROP TABLE "CURRENCY_TYPE";
DROP TABLE "CLIENT_TYPE";
DROP TABLE "SEX_TYPE";

-- Recreate views using enum columns
CREATE OR REPLACE VIEW "VIEW_CLIENT" AS
SELECT 
    c.id AS client_id,
    c.first_name AS client_first_name,
    c.last_name AS client_last_name,
    CASE c.client_type
        WHEN 'PF' THEN 'INDIVIDUAL'
        WHEN 'PJ' THEN 'COMPANY'
    END AS client_type_name,
    c.active AS client_active,
    COALESCE(ci.phone, '') AS phone,
    COALESCE(ci.email, '') AS email,
    c.created_at,
    c.updated_at
FROM "CLIENT" c
LEFT JOIN "CONTACT_INFO" ci ON c.id = ci.client_id;

CREATE OR REPLACE VIEW "VIEW_ACCOUNT" AS
SELECT 
    a.id AS account_id,
    a.iban AS account_iban,
    a.balance AS account_balance,
    a.currency_code AS currency_type_code,
    a.client_id,
    c.last_name AS client_last_name,
    c.first_name AS client_first_name,
    a.status AS account_status,
    a.created_at AS account_created_at,
    a.updated_at AS account_updated_at
FROM "ACCOUNT" a
JOIN "CLIENT" c ON a.client_id = c.id;

CREATE OR REPLACE VIEW "VIEW_TRANSACTION" AS
SELECT 
    t.id AS transaction_id,
    a.iban AS account_iban,
    CASE t.transaction_type_code
        WHEN 'DEP' THEN 'Deposit'
        WHEN 'RET' THEN 'Withdrawal'
        WHEN 'TRF' THEN 'Transfer'
    END AS transaction_type_name,
    t.amount AS transaction_amount,
    t.original_amount AS transaction_original_amount,
    t.sign AS transaction_sign,
    t.original_currency_code AS currency_type_code,
    t.transaction_date,
    t.details AS transaction_details
FROM "TRANSACTION" t
JOIN "ACCOUNT" a ON t.account_id = a.id;

CREATE INDEX IF NOT EXISTS idx_client_type ON "CLIENT"(client_type);
CREATE INDEX IF NOT EXISTS idx_sex_type ON "CLIENT"(sex_type);
CREATE INDEX IF NOT EXISTS idx_account_currency ON "ACCOUNT"(currency_code);
CREATE INDEX IF NOT EXISTS idx_transaction_type ON "TRANSACTION"(transaction_type_code);
