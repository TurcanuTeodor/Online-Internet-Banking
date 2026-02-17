-- V8__Create_fraud_detection_tables.sql
-- Tables for fraud detection and alerting system

CREATE TABLE "FRAUD_SCORE" (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL UNIQUE,
    score NUMERIC(5, 4) NOT NULL CHECK (score >= 0 AND score <= 1),
    risk_level VARCHAR(20) NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    
    -- Component scores
    amount_risk NUMERIC(3, 2) DEFAULT 0,
    time_risk NUMERIC(3, 2) DEFAULT 0,
    category_risk NUMERIC(3, 2) DEFAULT 0,
    frequency_risk NUMERIC(3, 2) DEFAULT 0,
    device_risk NUMERIC(3, 2) DEFAULT 0,
    
    -- Explainability (stored as JSON array of strings)
    reasons TEXT[],
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    FOREIGN KEY (transaction_id) REFERENCES "TRANSACTION"(id) ON DELETE CASCADE
);

CREATE TABLE "FRAUD_ALERT" (
    id BIGSERIAL PRIMARY KEY,
    fraud_score_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'REVIEWED', 'CONFIRMED', 'FALSE_POSITIVE')),
    
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP,
    review_notes TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    FOREIGN KEY (fraud_score_id) REFERENCES "FRAUD_SCORE"(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES "USER"(id) ON DELETE SET NULL
);

CREATE TABLE "FRAUD_SETTING" (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Insert default fraud detection thresholds
INSERT INTO "FRAUD_SETTING" (setting_key, setting_value, description) VALUES
    ('threshold_medium', '0.3', 'Score threshold for MEDIUM risk (0.00 - 1.00)'),
    ('threshold_high', '0.7', 'Score threshold for HIGH risk (0.00 - 1.00)'),
    ('threshold_critical', '0.9', 'Score threshold for CRITICAL risk (0.00 - 1.00)'),
    ('amount_multiplier_alert', '10', 'Alert if transaction is X times user average'),
    ('night_hours_start', '22', 'Night hours start (24h format)'),
    ('night_hours_end', '6', 'Night hours end (24h format)'),
    ('auto_block_enabled', 'false', 'Automatically block CRITICAL transactions'),
    ('alert_enabled', 'true', 'Enable fraud alerting system');

-- Create indexes
CREATE INDEX idx_fraud_score_risk_level ON "FRAUD_SCORE"(risk_level);
CREATE INDEX idx_fraud_score_score ON "FRAUD_SCORE"(score DESC);
CREATE INDEX idx_fraud_score_transaction ON "FRAUD_SCORE"(transaction_id);
CREATE INDEX idx_fraud_alert_status ON "FRAUD_ALERT"(status);
CREATE INDEX idx_fraud_alert_created ON "FRAUD_ALERT"(created_at DESC);
CREATE INDEX idx_fraud_alert_fraud_score ON "FRAUD_ALERT"(fraud_score_id);
CREATE INDEX idx_fraud_setting_key ON "FRAUD_SETTING"(setting_key);
