-- V4__Create_view_client.sql
-- Read-only view for admin dashboard (no JOIN with USER — that's in auth schema)

CREATE OR REPLACE VIEW "VIEW_CLIENT" AS
SELECT 
    c.id AS client_id,
    c.first_name AS client_first_name,
    c.last_name AS client_last_name,
    c.client_type AS client_type_name,
    c.active AS client_active,
    c.created_at AS client_created_at,
    ci.email,
    ci.phone,
    ci.address,
    ci.city,
    ci.postal_code
FROM "CLIENT" c
LEFT JOIN "CONTACT_INFO" ci ON c.id = ci.client_id
ORDER BY c.created_at DESC;
