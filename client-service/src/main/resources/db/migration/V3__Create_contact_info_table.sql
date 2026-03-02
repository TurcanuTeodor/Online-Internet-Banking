-- V3__Create_contact_info_table.sql

CREATE TABLE IF NOT EXISTS "CONTACT_INFO" (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL UNIQUE,
    email VARCHAR(100),
    phone VARCHAR(20),
    contact_person VARCHAR(100),
    website VARCHAR(255),
    address VARCHAR(255),
    city VARCHAR(100),
    postal_code VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES "CLIENT"(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_contact_info_email ON "CONTACT_INFO"(email);
CREATE INDEX IF NOT EXISTS idx_contact_info_phone ON "CONTACT_INFO"(phone);
