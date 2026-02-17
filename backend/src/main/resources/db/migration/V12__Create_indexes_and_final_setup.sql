-- V12__Create_indexes_and_final_setup.sql
-- Final indexes and optimization

-- Additional indexes for frequently queried columns
CREATE INDEX idx_user_client_id ON "USER"(client_id);
CREATE INDEX idx_account_client_id_currency ON "ACCOUNT"(client_id, currency_code);

-- Indexes for fraud detection performance
CREATE INDEX idx_fraud_score_created_at ON "FRAUD_SCORE"(created_at DESC);
CREATE INDEX idx_fraud_alert_created_at_status ON "FRAUD_ALERT"(created_at DESC, status);

-- Transaction queries optimization
CREATE INDEX idx_transaction_account_date ON "TRANSACTION"(account_id, transaction_date DESC);
CREATE INDEX idx_transaction_dest_account_date ON "TRANSACTION"(destination_account_id, transaction_date DESC);

-- Category rules lookup optimization
CREATE INDEX idx_category_keyword_lower ON "CATEGORY_RULE"(LOWER(keyword));

-- View materialization support (if needed in future)
-- CLUSTER "TRANSACTION" USING idx_transaction_account_date;
-- CLUSTER "FRAUD_SCORE" USING idx_fraud_score_created_at;

-- Grant appropriate permissions (if needed)
-- GRANT SELECT ON "VIEW_CLIENT" TO banking_user;
-- GRANT SELECT ON "VIEW_ACCOUNT" TO banking_user;
-- GRANT SELECT ON "VIEW_TRANSACTION" TO banking_user;
-- GRANT SELECT ON "VIEW_FRAUD_DASHBOARD" TO banking_user;
