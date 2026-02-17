-- V10__Seed_sample_data.sql
-- Sample dataset: 25 clients, 50 accounts, 50 transactions with fraud detection support

-- 1) Clients (20 PF individuals + 5 PJ companies)
INSERT INTO "CLIENT" (first_name, last_name, client_type, sex_type, risk_level, active) VALUES
    ('John', 'Doe', 'PF', 'M', 'LOW', true),
    ('Jane', 'Smith', 'PF', 'F', 'LOW', true),
    ('Alex', 'Johnson', 'PF', 'M', 'MEDIUM', true),
    ('Maria', 'Popescu', 'PF', 'F', 'LOW', true),
    ('Andrei', 'Ionescu', 'PF', 'M', 'LOW', true),
    ('Laura', 'Dumitrescu', 'PF', 'F', 'HIGH', true),
    ('Vlad', 'Georgescu', 'PF', 'M', 'LOW', true),
    ('Irina', 'Stan', 'PF', 'F', 'MEDIUM', true),
    ('Cosmin', 'Enache', 'PF', 'M', 'LOW', true),
    ('Ana', 'Neagu', 'PF', 'F', 'LOW', true),
    ('Robert', 'Marin', 'PF', 'M', 'CRITICAL', true),
    ('Cristina', 'Tudor', 'PF', 'F', 'LOW', true),
    ('Bogdan', 'Petrescu', 'PF', 'M', 'LOW', true),
    ('Diana', 'Serban', 'PF', 'F', 'MEDIUM', true),
    ('Mihai', 'Radu', 'PF', 'M', 'LOW', true),
    ('Elena', 'Matei', 'PF', 'F', 'LOW', true),
    ('George', 'Dragan', 'PF', 'M', 'HIGH', true),
    ('Raluca', 'Voicu', 'PF', 'F', 'LOW', true),
    ('Stefan', 'Nistor', 'PF', 'M', 'LOW', true),
    ('Oana', 'Pavel', 'PF', 'F', 'MEDIUM', true),
    ('Paul', 'Sandu', 'PJ', 'M', 'LOW', true),
    ('Teodora', 'Ilie', 'PJ', 'F', 'LOW', true),
    ('Iulian', 'Barbu', 'PJ', 'M', 'MEDIUM', true),
    ('Monica', 'Nedelcu', 'PJ', 'F', 'HIGH', true),
    ('Daniel', 'Balan', 'PJ', 'M', 'LOW', true);

-- 2) Contact info (optional fields kept simple)
INSERT INTO "CONTACT_INFO" (client_id, email, phone, address, city, postal_code) VALUES
    (1, 'john.doe@example.com', '+4011111111', 'Strada 1', 'Bucuresti', '010001'),
    (2, 'jane.smith@example.com', '+4011111112', 'Strada 2', 'Bucuresti', '010002'),
    (3, 'alex.johnson@example.com', '+4011111113', 'Strada 3', 'Cluj', '400001'),
    (4, 'maria.popescu@example.com', '+4011111114', 'Strada 4', 'Cluj', '400002'),
    (5, 'andrei.ionescu@example.com', '+4011111115', 'Strada 5', 'Iasi', '700001'),
    (6, 'laura.dumitrescu@example.com', '+4011111116', 'Strada 6', 'Iasi', '700002'),
    (7, 'vlad.georgescu@example.com', '+4011111117', 'Strada 7', 'Timisoara', '300001'),
    (8, 'irina.stan@example.com', '+4011111118', 'Strada 8', 'Timisoara', '300002'),
    (9, 'cosmin.enache@example.com', '+4011111119', 'Strada 9', 'Brasov', '500001'),
    (10, 'ana.neagu@example.com', '+4011111120', 'Strada 10', 'Brasov', '500002'),
    (11, 'robert.marin@example.com', '+4011111121', 'Strada 11', 'Constanta', '900001'),
    (12, 'cristina.tudor@example.com', '+4011111122', 'Strada 12', 'Constanta', '900002'),
    (13, 'bogdan.petrescu@example.com', '+4011111123', 'Strada 13', 'Sibiu', '550001'),
    (14, 'diana.serban@example.com', '+4011111124', 'Strada 14', 'Sibiu', '550002'),
    (15, 'mihai.radu@example.com', '+4011111125', 'Strada 15', 'Oradea', '410001'),
    (16, 'elena.matei@example.com', '+4011111126', 'Strada 16', 'Oradea', '410002'),
    (17, 'george.dragan@example.com', '+4011111127', 'Strada 17', 'Ploiesti', '100001'),
    (18, 'raluca.voicu@example.com', '+4011111128', 'Strada 18', 'Ploiesti', '100002'),
    (19, 'stefan.nistor@example.com', '+4011111129', 'Strada 19', 'Galati', '800001'),
    (20, 'oana.pavel@example.com', '+4011111130', 'Strada 20', 'Galati', '800002'),
    (21, 'paul.sandu@example.com', '+4011111131', 'Strada 21', 'Bucuresti', '010021'),
    (22, 'teodora.ilie@example.com', '+4011111132', 'Strada 22', 'Bucuresti', '010022'),
    (23, 'iulian.barbu@example.com', '+4011111133', 'Strada 23', 'Cluj', '400023'),
    (24, 'monica.nedelcu@example.com', '+4011111134', 'Strada 24', 'Cluj', '400024'),
    (25, 'daniel.balan@example.com', '+4011111135', 'Strada 25', 'Iasi', '700025');

-- 3) Accounts (50 total)
-- Clients 1-5: 3 accounts each (different currencies)
-- Clients 6-20: 2 accounts each (second in different currency)
-- Clients 21-25: 1 account each
INSERT INTO "ACCOUNT" (client_id, iban, balance, currency_code, status) VALUES
    -- Client 1 (accounts 1-3)
    (1, 'RO49BANK0000000001EUR', 2500.00, 'EUR', 'ACTIVE'),
    (1, 'RO49BANK0000000001USD', 1800.00, 'USD', 'ACTIVE'),
    (1, 'RO49BANK0000000001RON', 9500.00, 'RON', 'ACTIVE'),
    -- Client 2 (accounts 4-6)
    (2, 'RO49BANK0000000002EUR', 3200.00, 'EUR', 'ACTIVE'),
    (2, 'RO49BANK0000000002GBP', 2100.00, 'GBP', 'ACTIVE'),
    (2, 'RO49BANK0000000002RON', 7200.00, 'RON', 'ACTIVE'),
    -- Client 3 (accounts 7-9)
    (3, 'RO49BANK0000000003USD', 1400.00, 'USD', 'ACTIVE'),
    (3, 'RO49BANK0000000003EUR', 2750.00, 'EUR', 'ACTIVE'),
    (3, 'RO49BANK0000000003RON', 6100.00, 'RON', 'ACTIVE'),
    -- Client 4 (accounts 10-12)
    (4, 'RO49BANK0000000004EUR', 1950.00, 'EUR', 'ACTIVE'),
    (4, 'RO49BANK0000000004USD', 1650.00, 'USD', 'ACTIVE'),
    (4, 'RO49BANK0000000004GBP', 980.00, 'GBP', 'ACTIVE'),
    -- Client 5 (accounts 13-15)
    (5, 'RO49BANK0000000005RON', 8300.00, 'RON', 'ACTIVE'),
    (5, 'RO49BANK0000000005EUR', 2400.00, 'EUR', 'ACTIVE'),
    (5, 'RO49BANK0000000005USD', 1250.00, 'USD', 'ACTIVE'),
    -- Client 6 (accounts 16-17)
    (6, 'RO49BANK0000000006RON', 4100.00, 'RON', 'ACTIVE'),
    (6, 'RO49BANK0000000006EUR', 1300.00, 'EUR', 'ACTIVE'),
    -- Client 7 (accounts 18-19)
    (7, 'RO49BANK0000000007RON', 5200.00, 'RON', 'ACTIVE'),
    (7, 'RO49BANK0000000007USD', 1600.00, 'USD', 'ACTIVE'),
    -- Client 8 (accounts 20-21)
    (8, 'RO49BANK0000000008EUR', 2100.00, 'EUR', 'ACTIVE'),
    (8, 'RO49BANK0000000008GBP', 900.00, 'GBP', 'ACTIVE'),
    -- Client 9 (accounts 22-23)
    (9, 'RO49BANK0000000009RON', 4500.00, 'RON', 'ACTIVE'),
    (9, 'RO49BANK0000000009EUR', 1500.00, 'EUR', 'ACTIVE'),
    -- Client 10 (accounts 24-25)
    (10, 'RO49BANK0000000010EUR', 2700.00, 'EUR', 'ACTIVE'),
    (10, 'RO49BANK0000000010USD', 1200.00, 'USD', 'ACTIVE'),
    -- Client 11 (accounts 26-27)
    (11, 'RO49BANK0000000011GBP', 800.00, 'GBP', 'ACTIVE'),
    (11, 'RO49BANK0000000011RON', 5400.00, 'RON', 'ACTIVE'),
    -- Client 12 (accounts 28-29)
    (12, 'RO49BANK0000000012RON', 3600.00, 'RON', 'ACTIVE'),
    (12, 'RO49BANK0000000012EUR', 1900.00, 'EUR', 'ACTIVE'),
    -- Client 13 (accounts 30-31)
    (13, 'RO49BANK0000000013USD', 1750.00, 'USD', 'ACTIVE'),
    (13, 'RO49BANK0000000013EUR', 2100.00, 'EUR', 'ACTIVE'),
    -- Client 14 (accounts 32-33)
    (14, 'RO49BANK0000000014EUR', 2300.00, 'EUR', 'ACTIVE'),
    (14, 'RO49BANK0000000014GBP', 1100.00, 'GBP', 'ACTIVE'),
    -- Client 15 (accounts 34-35)
    (15, 'RO49BANK0000000015RON', 6200.00, 'RON', 'ACTIVE'),
    (15, 'RO49BANK0000000015USD', 1350.00, 'USD', 'ACTIVE'),
    -- Client 16 (accounts 36-37)
    (16, 'RO49BANK0000000016EUR', 2050.00, 'EUR', 'ACTIVE'),
    (16, 'RO49BANK0000000016RON', 4700.00, 'RON', 'ACTIVE'),
    -- Client 17 (accounts 38-39)
    (17, 'RO49BANK0000000017GBP', 1250.00, 'GBP', 'ACTIVE'),
    (17, 'RO49BANK0000000017EUR', 1850.00, 'EUR', 'ACTIVE'),
    -- Client 18 (accounts 40-41)
    (18, 'RO49BANK0000000018USD', 1500.00, 'USD', 'ACTIVE'),
    (18, 'RO49BANK0000000018RON', 3900.00, 'RON', 'ACTIVE'),
    -- Client 19 (accounts 42-43)
    (19, 'RO49BANK0000000019EUR', 2600.00, 'EUR', 'ACTIVE'),
    (19, 'RO49BANK0000000019USD', 1550.00, 'USD', 'ACTIVE'),
    -- Client 20 (accounts 44-45)
    (20, 'RO49BANK0000000020RON', 5100.00, 'RON', 'ACTIVE'),
    (20, 'RO49BANK0000000020GBP', 950.00, 'GBP', 'ACTIVE'),
    -- Client 21 (account 46)
    (21, 'RO49BANK0000000021EUR', 1800.00, 'EUR', 'ACTIVE'),
    -- Client 22 (account 47)
    (22, 'RO49BANK0000000022USD', 1400.00, 'USD', 'ACTIVE'),
    -- Client 23 (account 48)
    (23, 'RO49BANK0000000023RON', 3300.00, 'RON', 'ACTIVE'),
    -- Client 24 (account 49)
    (24, 'RO49BANK0000000024GBP', 870.00, 'GBP', 'ACTIVE'),
    -- Client 25 (account 50)
    (25, 'RO49BANK0000000025EUR', 2500.00, 'EUR', 'ACTIVE');

-- 4) Transactions (50 total) with merchants and categories for AI processing
INSERT INTO "TRANSACTION" (account_id, transaction_type, category, amount, original_amount, original_currency, sign, merchant, details, transaction_date) VALUES
    -- High spenders with varied categories
    (1, 'DEPOSIT', 'INCOME', 500.00, 500.00, 'EUR', 'CREDIT', NULL, 'Initial deposit', NOW() - INTERVAL '30 days'),
    (2, 'WITHDRAWAL', 'GROCERIES', 200.00, 200.00, 'USD', 'DEBIT', 'Walmart', 'Groceries shopping', NOW() - INTERVAL '28 days'),
    (3, 'DEPOSIT', 'INCOME', 750.00, 750.00, 'RON', 'CREDIT', NULL, 'Salary', NOW() - INTERVAL '26 days'),
    (4, 'TRANSFER_INTERNAL', 'OTHERS', 300.00, 300.00, 'EUR', 'DEBIT', NULL, 'Transfer rent', NOW() - INTERVAL '25 days'),
    (5, 'DEPOSIT', 'INCOME', 600.00, 600.00, 'GBP', 'CREDIT', NULL, 'Bonus', NOW() - INTERVAL '24 days'),
    (6, 'WITHDRAWAL', 'SUBSCRIPTIONS', 350.00, 350.00, 'RON', 'DEBIT', 'Netflix', 'Monthly subscription', NOW() - INTERVAL '23 days'),
    (7, 'DEPOSIT', 'INCOME', 420.00, 420.00, 'USD', 'CREDIT', NULL, 'Freelance payment', NOW() - INTERVAL '22 days'),
    (8, 'TRANSFER_INTERNAL', 'OTHERS', 250.00, 250.00, 'EUR', 'DEBIT', NULL, 'Transfer to savings', NOW() - INTERVAL '21 days'),
    (9, 'DEPOSIT', 'INCOME', 900.00, 900.00, 'RON', 'CREDIT', NULL, 'Salary', NOW() - INTERVAL '20 days'),
    (10, 'WITHDRAWAL', 'FOOD', 150.00, 150.00, 'EUR', 'DEBIT', 'McDonald''s', 'Fast food', NOW() - INTERVAL '19 days'),
    (11, 'DEPOSIT', 'INCOME', 700.00, 700.00, 'GBP', 'CREDIT', NULL, 'Dividend', NOW() - INTERVAL '18 days'),
    (12, 'TRANSFER_INTERNAL', 'OTHERS', 400.00, 400.00, 'RON', 'DEBIT', NULL, 'Transfer to EUR account', NOW() - INTERVAL '17 days'),
    (13, 'DEPOSIT', 'INCOME', 650.00, 650.00, 'RON', 'CREDIT', NULL, 'Project bonus', NOW() - INTERVAL '16 days'),
    (14, 'WITHDRAWAL', 'TRANSPORT', 220.00, 220.00, 'EUR', 'DEBIT', 'Shell', 'Fuel', NOW() - INTERVAL '15 days'),
    (15, 'TRANSFER_INTERNAL', 'OTHERS', 500.00, 500.00, 'USD', 'DEBIT', NULL, 'Transfer to USD account', NOW() - INTERVAL '14 days'),
    (16, 'WITHDRAWAL', 'SHOPPING', 480.00, 480.00, 'RON', 'DEBIT', 'Amazon', 'Online shopping', NOW() - INTERVAL '13 days'),
    (17, 'WITHDRAWAL', 'FOOD', 130.00, 130.00, 'EUR', 'DEBIT', 'Starbucks', 'Coffee', NOW() - INTERVAL '12 days'),
    (18, 'DEPOSIT', 'INCOME', 520.00, 520.00, 'RON', 'CREDIT', NULL, 'Salary', NOW() - INTERVAL '11 days'),
    (19, 'TRANSFER_INTERNAL', 'OTHERS', 275.00, 275.00, 'USD', 'DEBIT', NULL, 'Transfer to savings', NOW() - INTERVAL '10 days'),
    (20, 'DEPOSIT', 'INCOME', 610.00, 610.00, 'EUR', 'CREDIT', NULL, 'Gift', NOW() - INTERVAL '9 days'),
    (21, 'WITHDRAWAL', 'SUBSCRIPTIONS', 190.00, 190.00, 'RON', 'DEBIT', 'HBO Max', 'Streaming service', NOW() - INTERVAL '8 days'),
    (22, 'DEPOSIT', 'INCOME', 540.00, 540.00, 'RON', 'CREDIT', NULL, 'Salary', NOW() - INTERVAL '7 days'),
    (23, 'TRANSFER_INTERNAL', 'OTHERS', 260.00, 260.00, 'EUR', 'DEBIT', NULL, 'Transfer to EUR account', NOW() - INTERVAL '6 days'),
    (24, 'DEPOSIT', 'INCOME', 730.00, 730.00, 'EUR', 'CREDIT', NULL, 'Bonus', NOW() - INTERVAL '5 days'),
    (25, 'WITHDRAWAL', 'SHOPPING', 210.00, 210.00, 'USD', 'DEBIT', 'Zara', 'Clothing', NOW() - INTERVAL '4 days'),
    (26, 'DEPOSIT', 'INCOME', 455.00, 455.00, 'GBP', 'CREDIT', NULL, 'Freelance', NOW() - INTERVAL '3 days'),
    (27, 'TRANSFER_INTERNAL', 'OTHERS', 300.00, 300.00, 'RON', 'DEBIT', NULL, 'Transfer to RON account', NOW() - INTERVAL '2 days'),
    (28, 'DEPOSIT', 'INCOME', 380.00, 380.00, 'RON', 'CREDIT', NULL, 'Salary advance', NOW() - INTERVAL '1 day'),
    (29, 'WITHDRAWAL', 'ENTERTAINMENT', 175.00, 175.00, 'EUR', 'DEBIT', 'Cinema', 'Movie tickets', NOW()),
    (30, 'DEPOSIT', 'INCOME', 640.00, 640.00, 'USD', 'CREDIT', NULL, 'Commission', NOW()),
    (31, 'WITHDRAWAL', 'HEALTH', 205.00, 205.00, 'EUR', 'DEBIT', 'Pharmacy', 'Medicines', NOW() - INTERVAL '2 days'),
    (32, 'DEPOSIT', 'INCOME', 590.00, 590.00, 'EUR', 'CREDIT', NULL, 'Dividend', NOW() - INTERVAL '3 days'),
    (33, 'TRANSFER_INTERNAL', 'OTHERS', 310.00, 310.00, 'GBP', 'DEBIT', NULL, 'Transfer to GBP account', NOW() - INTERVAL '4 days'),
    (34, 'DEPOSIT', 'INCOME', 720.00, 720.00, 'RON', 'CREDIT', NULL, 'Salary', NOW() - INTERVAL '5 days'),
    (35, 'WITHDRAWAL', 'SHOPPING', 260.00, 260.00, 'USD', 'DEBIT', 'Best Buy', 'Electronics', NOW() - INTERVAL '6 days'),
    (36, 'DEPOSIT', 'INCOME', 410.00, 410.00, 'EUR', 'CREDIT', NULL, 'Bonus', NOW() - INTERVAL '7 days'),
    (37, 'WITHDRAWAL', 'SUBSCRIPTIONS', 185.00, 185.00, 'RON', 'DEBIT', 'Spotify', 'Music subscription', NOW() - INTERVAL '8 days'),
    (38, 'DEPOSIT', 'INCOME', 530.00, 530.00, 'GBP', 'CREDIT', NULL, 'Project', NOW() - INTERVAL '9 days'),
    (39, 'TRANSFER_INTERNAL', 'OTHERS', 295.00, 295.00, 'EUR', 'DEBIT', NULL, 'Transfer to EUR account', NOW() - INTERVAL '10 days'),
    (40, 'DEPOSIT', 'INCOME', 605.00, 605.00, 'USD', 'CREDIT', NULL, 'Salary', NOW() - INTERVAL '11 days'),
    (41, 'WITHDRAWAL', 'TRANSPORT', 215.00, 215.00, 'RON', 'DEBIT', 'Uber', 'Taxi service', NOW() - INTERVAL '12 days'),
    (42, 'DEPOSIT', 'INCOME', 675.00, 675.00, 'EUR', 'CREDIT', NULL, 'Bonus', NOW() - INTERVAL '13 days'),
    (43, 'TRANSFER_INTERNAL', 'OTHERS', 330.00, 330.00, 'USD', 'DEBIT', NULL, 'Transfer to USD account', NOW() - INTERVAL '14 days'),
    (44, 'WITHDRAWAL', 'GROCERIES', 495.00, 495.00, 'RON', 'DEBIT', 'Carrefour', 'Supermarket', NOW() - INTERVAL '15 days'),
    (45, 'WITHDRAWAL', 'ENTERTAINMENT', 225.00, 225.00, 'GBP', 'DEBIT', 'Disney+', 'Streaming', NOW() - INTERVAL '16 days'),
    (46, 'DEPOSIT', 'INCOME', 560.00, 560.00, 'EUR', 'CREDIT', NULL, 'Salary', NOW() - INTERVAL '17 days'),
    (47, 'WITHDRAWAL', 'SUBSCRIPTIONS', 240.00, 240.00, 'USD', 'DEBIT', 'Apple Music', 'Music service', NOW() - INTERVAL '18 days'),
    (48, 'DEPOSIT', 'INCOME', 585.00, 585.00, 'RON', 'CREDIT', NULL, 'Gift', NOW() - INTERVAL '19 days'),
    (49, 'TRANSFER_INTERNAL', 'OTHERS', 345.00, 345.00, 'GBP', 'DEBIT', NULL, 'Transfer to GBP account', NOW() - INTERVAL '20 days'),
    (50, 'DEPOSIT', 'INCOME', 700.00, 700.00, 'EUR', 'CREDIT', NULL, 'Salary', NOW() - INTERVAL '21 days');
