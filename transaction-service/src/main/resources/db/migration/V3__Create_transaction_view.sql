-- V3__Create_transaction_view.sql
-- Simplified view: no cross-schema JOINs to CLIENT or ACCOUNT
-- Contains only transaction data with account IDs

CREATE OR REPLACE VIEW "VIEW_TRANSACTION" AS
SELECT 
    t.id AS transaction_id,
    t.account_id,
    t.destination_account_id,
    t.transaction_type::text AS transaction_type,
    t.category::text AS category,
    t.amount,
    t.original_amount,
    t.original_currency::text AS original_currency,
    t.sign,
    t.merchant,
    t.details,
    t.risk_score,
    t.flagged,
    t.transaction_date
FROM "TRANSACTION" t
ORDER BY t.transaction_date DESC;
