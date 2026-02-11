-- V13__Fix_user_passwords.sql
-- Fix the BCrypt hashes for sample users
-- The V11 migration had incorrect password hashes
-- Correct hash for "password123": $2a$10$m9MurmpHiOOPtv7ahG4/veT9vZdgFLaep50nUQ1gsDBcar7EqD2sm

UPDATE "USER"
SET password_hash = '$2a$10$m9MurmpHiOOPtv7ahG4/veT9vZdgFLaep50nUQ1gsDBcar7EqD2sm'
WHERE username_or_email IN ('admin@cashtactics.com', 'user@cashtactics.com');
