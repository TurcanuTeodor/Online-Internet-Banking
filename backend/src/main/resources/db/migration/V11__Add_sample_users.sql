-- Add sample users (one admin, one regular user)
-- Password for both: "password123" (hashed with BCrypt)
-- Using client_id 2 and 3 (client_id 1 already has a user)

INSERT INTO "USER" (client_id, username_or_email, password_hash, role, two_factor_enabled, two_factor_secret)
SELECT 2, 'admin@cashtactics.com', '$2a$10$N9qo8uLOickgx2ZrVzY6oeOMaAIxJjPL5JvYmDxG3j0hj.r3iFFoO', 'ADMIN', false, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM "USER" WHERE client_id = 2 OR username_or_email = 'admin@cashtactics.com'
);

INSERT INTO "USER" (client_id, username_or_email, password_hash, role, two_factor_enabled, two_factor_secret)
SELECT 3, 'user@cashtactics.com', '$2a$10$N9qo8uLOickgx2ZrVzY6oeOMaAIxJjPL5JvYmDxG3j0hj.r3iFFoO', 'USER', false, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM "USER" WHERE client_id = 3 OR username_or_email = 'user@cashtactics.com'
);

-- Admin login: admin@cashtactics.com / password123
-- User login: user@cashtactics.com / password123
