-- V8__Add_amount_and_currency_to_fraud_decision.sql
-- Adds AMOUNT and CURRENCY_CODE columns to FRAUD_DECISION.
-- These store the evaluated transaction value so charts can visualize
-- risk vs amount without needing a join through TRANSACTION_ID
-- (which is null at evaluation time, since fraud check runs before
-- the transaction record is created in transaction-service).

ALTER TABLE fraud."FRAUD_DECISION"
    ADD COLUMN IF NOT EXISTS "AMOUNT"        DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS "CURRENCY_CODE" VARCHAR(10);
