-- V6__Create_transaction_table.sql

CREATE TABLE "TRANSACTION" (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    destination_account_id BIGINT,
    transaction_type TRANSACTION_TYPE_ENUM NOT NULL,
    category TRANSACTION_CATEGORY_ENUM NOT NULL DEFAULT 'OTHERS',
    amount NUMERIC(15, 2) NOT NULL,
    original_amount NUMERIC(15, 2) NOT NULL,
    original_currency CURRENCY_ENUM,
    sign VARCHAR(10) NOT NULL,
    merchant VARCHAR(255),
    details VARCHAR(255),
    risk_score NUMERIC(5, 4),
    flagged BOOLEAN NOT NULL DEFAULT false,
    transaction_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES "ACCOUNT"(id) ON DELETE CASCADE,
    FOREIGN KEY (destination_account_id) REFERENCES "ACCOUNT"(id) ON DELETE SET NULL
);

CREATE INDEX idx_transaction_account_id ON "TRANSACTION"(account_id);
CREATE INDEX idx_transaction_dest_account_id ON "TRANSACTION"(destination_account_id);
CREATE INDEX idx_transaction_type ON "TRANSACTION"(transaction_type);
CREATE INDEX idx_transaction_category ON "TRANSACTION"(category);
CREATE INDEX idx_transaction_date ON "TRANSACTION"(transaction_date DESC);
CREATE INDEX idx_transaction_flagged ON "TRANSACTION"(flagged);
CREATE INDEX idx_transaction_risk_score ON "TRANSACTION"(risk_score DESC);
