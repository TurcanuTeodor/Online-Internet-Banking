-- V2__Create_payment_table.sql
-- PAYMENT table — stores payment intents and their status

CREATE TABLE "PAYMENT" (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    currency CURRENCY_ENUM NOT NULL,
    status PAYMENT_STATUS_ENUM NOT NULL DEFAULT 'PENDING',
    stripe_payment_intent_id VARCHAR(255),
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_payment_client_id ON "PAYMENT"(client_id);
CREATE INDEX idx_payment_account_id ON "PAYMENT"(account_id);
CREATE INDEX idx_payment_status ON "PAYMENT"(status);
CREATE INDEX idx_payment_stripe_intent ON "PAYMENT"(stripe_payment_intent_id);
CREATE INDEX idx_payment_created_at ON "PAYMENT"(created_at DESC);
