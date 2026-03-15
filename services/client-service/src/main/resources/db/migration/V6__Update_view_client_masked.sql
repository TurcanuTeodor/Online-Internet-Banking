-- V6__Update_view_client_masked.sql
-- VIEW_CLIENT: datele de contact sunt criptate in DB (AES-256).
-- VIEW-ul expune doar campurile non-sensitive in clar.
-- Decriptarea se face in aplicatie (ClientService) la nevoie.

CREATE OR REPLACE VIEW "VIEW_CLIENT" AS
SELECT
    c.id          AS client_id,
    c.first_name  AS client_first_name,
    c.last_name   AS client_last_name,
    c.client_type AS client_type_name,
    c.active      AS client_active,
    c.created_at  AS client_created_at,
    -- Date criptate: expuse ca atare, nu se decripteaza in SQL
    ci.email      AS email_encrypted,
    ci.phone      AS phone_encrypted,
    ci.address    AS address_encrypted,
    ci.city       AS city_encrypted,
    ci.postal_code AS postal_code_encrypted
FROM "CLIENT" c
LEFT JOIN "CONTACT_INFO" ci ON c.id = ci.client_id
ORDER BY c.created_at DESC;