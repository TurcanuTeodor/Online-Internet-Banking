-- V3__Create_payment_method_table.sql
-- PAYMENT_METHOD table — stores tokenized card references (Stripe PaymentMethod IDs)

CREATE TABLE "PAYMENT_METHOD" (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    stripe_payment_method_id VARCHAR(255) NOT NULL,
    card_brand VARCHAR(50),
    card_last4 VARCHAR(4),
    expiry_month INTEGER,
    expiry_year INTEGER,
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pm_client_id ON "PAYMENT_METHOD"(client_id);
CREATE INDEX idx_pm_stripe_id ON "PAYMENT_METHOD"(stripe_payment_method_id);
