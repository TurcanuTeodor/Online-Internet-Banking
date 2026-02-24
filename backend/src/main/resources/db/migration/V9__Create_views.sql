-- V9__Create_views.sql
-- Read-only views for admin dashboard

CREATE OR REPLACE VIEW "VIEW_CLIENT" AS
SELECT 
    c.id as client_id,
    c.first_name as client_first_name,
    c.last_name as client_last_name,
    c.client_type as client_type_name,
    c.active as client_active,
    c.created_at as client_created_at,
    ci.email,
    ci.phone,
    ci.address,
    ci.city,
    ci.postal_code,
    u.username_or_email as username_email
FROM "CLIENT" c
LEFT JOIN "CONTACT_INFO" ci ON c.id = ci.client_id
LEFT JOIN "USER" u ON c.id = u.client_id
ORDER BY c.created_at DESC;

CREATE OR REPLACE VIEW "VIEW_ACCOUNT" AS
SELECT 
    a.id as account_id,
    a.iban as account_iban,
    a.balance as account_balance,
    a.currency_code as currency_type_code,
    a.status as account_status,
    c.id as client_id,
    c.first_name as client_first_name,
    c.last_name as client_last_name,
    COUNT(t.id) as transaction_count,
    MAX(t.transaction_date) as last_transaction_date,
    a.created_at as account_created_at,
    a.updated_at as account_updated_at
FROM "ACCOUNT" a
JOIN "CLIENT" c ON a.client_id = c.id
LEFT JOIN "TRANSACTION" t ON a.id = t.account_id
GROUP BY a.id, a.iban, a.balance, a.currency_code, a.status,
         c.id, c.first_name, c.last_name, a.created_at, a.updated_at
ORDER BY a.created_at DESC;

CREATE OR REPLACE VIEW "VIEW_TRANSACTION" AS
SELECT 
    t.id as transaction_id,
    src_acc.iban as source_iban,
    src_c.id as source_client_id,
    src_c.first_name as source_first_name,
    src_c.last_name as source_last_name,
    dst_acc.iban as dest_iban,
    dst_c.id as dest_client_id,
    dst_c.first_name as dest_first_name,
    dst_c.last_name as dest_last_name,
    t.transaction_type as transaction_type_name,
    t.amount as transaction_amount,
    t.original_amount as transaction_original_amount,
    t.sign as transaction_sign,
    t.original_currency as currency_type_code,
    t.transaction_date,
    t.details as transaction_details,
    t.risk_score as fraud_score
FROM "TRANSACTION" t
JOIN "ACCOUNT" src_acc ON t.account_id = src_acc.id
JOIN "CLIENT" src_c ON src_acc.client_id = src_c.id
LEFT JOIN "ACCOUNT" dst_acc ON t.destination_account_id = dst_acc.id
LEFT JOIN "CLIENT" dst_c ON dst_acc.client_id = dst_c.id
ORDER BY t.transaction_date DESC;

CREATE OR REPLACE VIEW "VIEW_FRAUD_DASHBOARD" AS
SELECT 
    fs.id as fraud_score_id,
    fs.score,
    fs.risk_level,
    fs.amount_risk,
    fs.time_risk,
    fs.category_risk,
    fs.frequency_risk,
    fs.device_risk,
    fs.reasons,
    fs.created_at as detection_date,
    
    t.id as transaction_id,
    t.amount,
    t.merchant,
    t.transaction_type,
    t.category,
    t.transaction_date,
    
    src_acc.iban as source_iban,
    src_c.first_name as source_first_name,
    src_c.last_name as source_last_name,
    src_c.risk_level as source_risk_level,
    
    dst_acc.iban as dest_iban,
    dst_c.first_name as dest_first_name,
    dst_c.last_name as dest_last_name,
    dst_c.risk_level as dest_risk_level,
    
    fa.id as alert_id,
    fa.status as alert_status,
    fa.reviewed_by,
    fa.reviewed_at,
    fa.review_notes
    
FROM "FRAUD_SCORE" fs
JOIN "TRANSACTION" t ON fs.transaction_id = t.id
JOIN "ACCOUNT" src_acc ON t.account_id = src_acc.id
JOIN "CLIENT" src_c ON src_acc.client_id = src_c.id
LEFT JOIN "ACCOUNT" dst_acc ON t.destination_account_id = dst_acc.id
LEFT JOIN "CLIENT" dst_c ON dst_acc.client_id = dst_c.id
LEFT JOIN "FRAUD_ALERT" fa ON fs.id = fa.fraud_score_id
ORDER BY fs.created_at DESC;
