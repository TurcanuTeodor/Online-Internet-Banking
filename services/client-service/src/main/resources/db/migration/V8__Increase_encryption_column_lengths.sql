-- V8__Increase_encryption_column_lengths.sql
-- Expand column lengths specifically to accommodate large payload Base64 strings from encrypted AES GCM output

DROP VIEW IF EXISTS "VIEW_CLIENT";

ALTER TABLE "CLIENT" ALTER COLUMN first_name TYPE VARCHAR(500);
ALTER TABLE "CLIENT" ALTER COLUMN last_name TYPE VARCHAR(500);

ALTER TABLE "CONTACT_INFO" ALTER COLUMN email TYPE VARCHAR(500);
ALTER TABLE "CONTACT_INFO" ALTER COLUMN phone TYPE VARCHAR(500);
ALTER TABLE "CONTACT_INFO" ALTER COLUMN contact_person TYPE VARCHAR(500);
ALTER TABLE "CONTACT_INFO" ALTER COLUMN website TYPE VARCHAR(500);
ALTER TABLE "CONTACT_INFO" ALTER COLUMN address TYPE VARCHAR(500);
ALTER TABLE "CONTACT_INFO" ALTER COLUMN city TYPE VARCHAR(500);
ALTER TABLE "CONTACT_INFO" ALTER COLUMN postal_code TYPE VARCHAR(500);

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
LEFT JOIN "CONTACT_INFO" ci ON c.id = ci.client_id;