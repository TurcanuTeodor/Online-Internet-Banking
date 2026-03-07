-- V3__Create_view_account.sql
-- Read-only view for admin dashboard (account data only)
-- Distributed: no JOIN to CLIENT (lives in 'clients' schema, owned by client-service)
-- Frontend enriches client names via separate REST call to client-service

CREATE OR REPLACE VIEW "VIEW_ACCOUNT" AS
SELECT 
    a.id          AS account_id,
    a.iban        AS account_iban,
    a.balance     AS account_balance,
    a.currency_code AS currency_type_code,
    a.client_id   AS client_id,
    a.status      AS account_status,
    a.created_at  AS account_created_at,
    a.updated_at  AS account_updated_at
FROM "ACCOUNT" a
ORDER BY a.created_at DESC;
