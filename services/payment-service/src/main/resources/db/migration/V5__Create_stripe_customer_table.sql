-- V5__Create_stripe_customer_table.sql
-- Maps internal clients to Stripe Customers for reusable saved-card payments.

CREATE TABLE "STRIPE_CUSTOMER" (
    client_id BIGINT PRIMARY KEY,
    stripe_customer_id VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stripe_customer_customer_id ON "STRIPE_CUSTOMER"(stripe_customer_id);

