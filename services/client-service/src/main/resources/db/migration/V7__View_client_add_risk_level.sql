-- Expose risk_level on admin view (non-PII aggregate field)
DROP VIEW IF EXISTS "VIEW_CLIENT";

CREATE OR REPLACE VIEW "VIEW_CLIENT" AS
SELECT
    c.id          AS client_id,
    c.first_name  AS client_first_name,
    c.last_name   AS client_last_name,
    c.client_type AS client_type_name,
    c.risk_level  AS risk_level,
    c.active      AS client_active,
    c.created_at  AS client_created_at,
    ci.email      AS email_encrypted,
    ci.phone      AS phone_encrypted,
    ci.address    AS address_encrypted,
    ci.city       AS city_encrypted,
    ci.postal_code AS postal_code_encrypted
FROM "CLIENT" c
LEFT JOIN "CONTACT_INFO" ci ON c.id = ci.client_id
ORDER BY c.created_at DESC;
