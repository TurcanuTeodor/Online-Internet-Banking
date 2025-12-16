-- V4__Create_client_table.sql

CREATE TABLE "CLIENT" (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    client_type_id BIGINT NOT NULL,
    sex_type_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_type_id) REFERENCES "CLIENT_TYPE"(id),
    FOREIGN KEY (sex_type_id) REFERENCES "SEX_TYPE"(id)
);

CREATE INDEX idx_client_last_name ON "CLIENT"(last_name);
CREATE INDEX idx_client_first_name ON "CLIENT"(first_name);
CREATE INDEX idx_client_active ON "CLIENT"(active);
