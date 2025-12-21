-- V8__Create_transaction_table.sql

CREATE TABLE "TRANSACTION" (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    transaction_type_id BIGINT NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    original_amount NUMERIC(15, 2) NOT NULL,
    original_currency_type_id BIGINT,
    sign VARCHAR(10) NOT NULL,
    details VARCHAR(255),
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES "ACCOUNT"(id) ON DELETE CASCADE,
    FOREIGN KEY (transaction_type_id) REFERENCES "TRANSACTION_TYPE"(id),
    FOREIGN KEY (original_currency_type_id) REFERENCES "CURRENCY_TYPE"(id)
);

CREATE INDEX idx_transaction_account_id ON "TRANSACTION"(account_id);
CREATE INDEX idx_transaction_type_id ON "TRANSACTION"(transaction_type_id);
CREATE INDEX idx_transaction_date ON "TRANSACTION"(transaction_date);
