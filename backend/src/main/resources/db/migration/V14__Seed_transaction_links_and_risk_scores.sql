-- V14__Seed_transaction_links_and_risk_scores.sql
-- Add destination accounts for internal transfers and seed risk scores for demo visibility

UPDATE "TRANSACTION"
SET destination_account_id = CASE
    WHEN transaction_type = 'TRANSFER_INTERNAL' THEN CASE WHEN account_id = 50 THEN 1 ELSE account_id + 1 END
    ELSE NULL
END;

UPDATE "TRANSACTION"
SET risk_score = CASE
    WHEN transaction_type = 'DEPOSIT' THEN 0.05
    WHEN transaction_type = 'WITHDRAWAL' THEN 0.15
    WHEN transaction_type = 'TRANSFER_INTERNAL' THEN 0.25
    ELSE 0.10
END;
