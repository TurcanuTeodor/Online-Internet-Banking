-- V7__Create_account_table.sql

CREATE TABLE "ACCOUNT" (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    iban VARCHAR(34) NOT NULL UNIQUE,
    balance NUMERIC(15, 2) NOT NULL DEFAULT 0,
    currency_type_id BIGINT NOT NULL,
    status ACCOUNT_STATUS_ENUM NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES "CLIENT"(id) ON DELETE CASCADE,
    FOREIGN KEY (currency_type_id) REFERENCES "CURRENCY_TYPE"(id)
);

CREATE INDEX idx_account_client_id ON "ACCOUNT"(client_id);
CREATE INDEX idx_account_iban ON "ACCOUNT"(iban);
CREATE INDEX idx_account_status ON "ACCOUNT"(status);
