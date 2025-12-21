-- V9__Create_views.sql

-- View for clients (read-only)
CREATE OR REPLACE VIEW "VIEW_CLIENT" AS
SELECT 
    c.id AS client_id,
    c.first_name AS client_first_name,
    c.last_name AS client_last_name,
    ct.name AS client_type_name,
    st.name AS sex_type_name,
    c.active AS client_active,
    c.created_at,
    c.updated_at
FROM "CLIENT" c
JOIN "CLIENT_TYPE" ct ON c.client_type_id = ct.id
JOIN "SEX_TYPE" st ON c.sex_type_id = st.id;

-- View for accounts (read-only)
CREATE OR REPLACE VIEW "VIEW_ACCOUNT" AS
SELECT 
    a.id AS account_id,
    a.iban AS account_iban,
    a.balance AS account_balance,
    cur.code AS currency_type_code,
    a.client_id,
    c.last_name AS client_last_name,
    c.first_name AS client_first_name,
    a.status AS account_status,
    a.created_at AS account_created_at,
    a.updated_at AS account_updated_at
FROM "ACCOUNT" a
JOIN "CURRENCY_TYPE" cur ON a.currency_type_id = cur.id
JOIN "CLIENT" c ON a.client_id = c.id;

-- View for transactions (read-only)
CREATE OR REPLACE VIEW "VIEW_TRANSACTION" AS
SELECT 
    t.id AS transaction_id,
    a.iban AS account_iban,
    tt.name AS transaction_type_name,
    t.amount AS transaction_amount,
    t.original_amount AS transaction_original_amount,
    t.sign AS transaction_sign,
    cur.code AS currency_type_code,
    t.transaction_date,
    t.details AS transaction_details
FROM "TRANSACTION" t
JOIN "ACCOUNT" a ON t.account_id = a.id
JOIN "TRANSACTION_TYPE" tt ON t.transaction_type_id = tt.id
LEFT JOIN "CURRENCY_TYPE" cur ON t.original_currency_type_id = cur.id;
