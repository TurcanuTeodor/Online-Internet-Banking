DROP VIEW IF EXISTS transactions."VIEW_TRANSACTION";

ALTER TYPE transactions.TRANSACTION_TYPE_ENUM RENAME VALUE 'DEPOSIT' TO 'DEP';
ALTER TYPE transactions.TRANSACTION_TYPE_ENUM RENAME VALUE 'WITHDRAWAL' TO 'WDL';
ALTER TYPE transactions.TRANSACTION_TYPE_ENUM RENAME VALUE 'TRANSFER_INTERNAL' TO 'TR_INT';
ALTER TYPE transactions.TRANSACTION_TYPE_ENUM RENAME VALUE 'TRANSFER_EXTERNAL' TO 'TR_EXT';

CREATE OR REPLACE VIEW transactions."VIEW_TRANSACTION" AS
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
FROM transactions."TRANSACTION" t
ORDER BY t.transaction_date DESC;
