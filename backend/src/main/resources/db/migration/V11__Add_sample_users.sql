-- V11__Add_sample_users.sql
-- Insert sample admin and regular users

-- Admin user (for client_id 1)
INSERT INTO "USER" (client_id, username_or_email, password_hash, role, two_factor_enabled)
VALUES (
    1,
    'admin@cashtactics.com',
    -- Password: password
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
    'ADMIN',
    false
);

-- Regular user (for client_id 2)
INSERT INTO "USER" (client_id, username_or_email, password_hash, role, two_factor_enabled)
VALUES (
    2,
    'user@cashtactics.com',
    -- Password: password
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
    'USER',
    false
);

-- Additional test users for different clients
INSERT INTO "USER" (client_id, username_or_email, password_hash, role, two_factor_enabled) VALUES
(3, 'john.doe@example.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'USER', false),
(4, 'jane.smith@example.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'USER', false),
(5, 'michael.johnson@example.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'USER', false),
(6, 'sarah.williams@example.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'USER', false),
(7, 'robert.brown@example.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'USER', false),
(11, 'techstartup@example.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'USER', false),
(12, 'finance.corp@example.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'USER', false);
