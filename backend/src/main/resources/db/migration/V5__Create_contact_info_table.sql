-- V5__Create_contact_info_table.sql

CREATE TABLE "CONTACT_INFO" (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL UNIQUE,
    email VARCHAR(255),
    phone VARCHAR(20),
    contact_person VARCHAR(100),
    website VARCHAR(100),
    address VARCHAR(255),
    city VARCHAR(100),
    postal_code VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES "CLIENT"(id) ON DELETE CASCADE
);

CREATE INDEX idx_contact_info_email ON "CONTACT_INFO"(email);
CREATE INDEX idx_contact_info_phone ON "CONTACT_INFO"(phone);
