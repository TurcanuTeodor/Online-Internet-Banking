-- V14__Make_updated_at_nullable.sql
-- Make updated_at column nullable for all tables to allow JPA @PreUpdate to handle it

ALTER TABLE "CLIENT" ALTER COLUMN updated_at DROP NOT NULL;
ALTER TABLE "CONTACT_INFO" ALTER COLUMN updated_at DROP NOT NULL;
ALTER TABLE "USER" ALTER COLUMN updated_at DROP NOT NULL;
ALTER TABLE "ACCOUNT" ALTER COLUMN updated_at DROP NOT NULL;
ALTER TABLE "TRANSACTION" ALTER COLUMN updated_at DROP NOT NULL;
