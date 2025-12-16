-- V1__Create_basic_types.sql
-- Create enum types for critical/fixed data

CREATE TYPE ROLE_ENUM AS ENUM ('USER', 'ADMIN');
CREATE TYPE ACCOUNT_STATUS_ENUM AS ENUM ('ACTIVE', 'CLOSED', 'SUSPENDED');
