-- V2__Create_client_table.sql

CREATE TABLE "CLIENT" (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    client_type CLIENT_TYPE_ENUM NOT NULL,
    sex_type SEX_TYPE_ENUM NOT NULL,
    risk_level VARCHAR(50) NOT NULL DEFAULT 'LOW',
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_client_last_name ON "CLIENT"(last_name);
CREATE INDEX idx_client_first_name ON "CLIENT"(first_name);
CREATE INDEX idx_client_active ON "CLIENT"(active);
CREATE INDEX idx_client_risk_level ON "CLIENT"(risk_level);
