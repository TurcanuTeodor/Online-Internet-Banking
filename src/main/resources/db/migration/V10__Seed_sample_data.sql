-- V10__Seed_sample_data.sql
-- Sample dataset: 25 clients, 50 accounts (15 clients with 2 accounts, 5 clients with 3 accounts), 50 transactions

-- 1) Clients
INSERT INTO "CLIENT" (first_name, last_name, client_type_id, sex_type_id, active) VALUES
    ('John', 'Doe', 1, 1, true),
    ('Jane', 'Smith', 1, 2, true),
    ('Alex', 'Johnson', 1, 1, true),
    ('Maria', 'Popescu', 1, 2, true),
    ('Andrei', 'Ionescu', 1, 1, true),
    ('Laura', 'Dumitrescu', 1, 2, true),
    ('Vlad', 'Georgescu', 1, 1, true),
    ('Irina', 'Stan', 1, 2, true),
    ('Cosmin', 'Enache', 1, 1, true),
    ('Ana', 'Neagu', 1, 2, true),
    ('Robert', 'Marin', 1, 1, true),
    ('Cristina', 'Tudor', 1, 2, true),
    ('Bogdan', 'Petrescu', 1, 1, true),
    ('Diana', 'Serban', 1, 2, true),
    ('Mihai', 'Radu', 1, 1, true),
    ('Elena', 'Matei', 1, 2, true),
    ('George', 'Dragan', 1, 1, true),
    ('Raluca', 'Voicu', 1, 2, true),
    ('Stefan', 'Nistor', 1, 1, true),
    ('Oana', 'Pavel', 1, 2, true),
    ('Paul', 'Sandu', 2, 1, true),
    ('Teodora', 'Ilie', 2, 2, true),
    ('Iulian', 'Barbu', 2, 1, true),
    ('Monica', 'Nedelcu', 2, 2, true),
    ('Daniel', 'Balan', 2, 1, true);

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
INSERT INTO "ACCOUNT" (client_id, iban, balance, currency_type_id, status) VALUES
    -- Client 1 (accounts 1-3)
    (1, 'RO49BANK0000000001EUR', 2500.00, 1, 'ACTIVE'),
    (1, 'RO49BANK0000000001USD', 1800.00, 2, 'ACTIVE'),
    (1, 'RO49BANK0000000001RON', 9500.00, 3, 'ACTIVE'),
    -- Client 2 (accounts 4-6)
    (2, 'RO49BANK0000000002EUR', 3200.00, 1, 'ACTIVE'),
    (2, 'RO49BANK0000000002GBP', 2100.00, 4, 'ACTIVE'),
    (2, 'RO49BANK0000000002RON', 7200.00, 3, 'ACTIVE'),
    -- Client 3 (accounts 7-9)
    (3, 'RO49BANK0000000003USD', 1400.00, 2, 'ACTIVE'),
    (3, 'RO49BANK0000000003EUR', 2750.00, 1, 'ACTIVE'),
    (3, 'RO49BANK0000000003RON', 6100.00, 3, 'ACTIVE'),
    -- Client 4 (accounts 10-12)
    (4, 'RO49BANK0000000004EUR', 1950.00, 1, 'ACTIVE'),
    (4, 'RO49BANK0000000004USD', 1650.00, 2, 'ACTIVE'),
    (4, 'RO49BANK0000000004GBP', 980.00, 4, 'ACTIVE'),
    -- Client 5 (accounts 13-15)
    (5, 'RO49BANK0000000005RON', 8300.00, 3, 'ACTIVE'),
    (5, 'RO49BANK0000000005EUR', 2400.00, 1, 'ACTIVE'),
    (5, 'RO49BANK0000000005USD', 1250.00, 2, 'ACTIVE'),
    -- Client 6 (accounts 16-17)
    (6, 'RO49BANK0000000006RON', 4100.00, 3, 'ACTIVE'),
    (6, 'RO49BANK0000000006EUR', 1300.00, 1, 'ACTIVE'),
    -- Client 7 (accounts 18-19)
    (7, 'RO49BANK0000000007RON', 5200.00, 3, 'ACTIVE'),
    (7, 'RO49BANK0000000007USD', 1600.00, 2, 'ACTIVE'),
    -- Client 8 (accounts 20-21)
    (8, 'RO49BANK0000000008EUR', 2100.00, 1, 'ACTIVE'),
    (8, 'RO49BANK0000000008GBP', 900.00, 4, 'ACTIVE'),
    -- Client 9 (accounts 22-23)
    (9, 'RO49BANK0000000009RON', 4500.00, 3, 'ACTIVE'),
    (9, 'RO49BANK0000000009EUR', 1500.00, 1, 'ACTIVE'),
    -- Client 10 (accounts 24-25)
    (10, 'RO49BANK0000000010EUR', 2700.00, 1, 'ACTIVE'),
    (10, 'RO49BANK0000000010USD', 1200.00, 2, 'ACTIVE'),
    -- Client 11 (accounts 26-27)
    (11, 'RO49BANK0000000011GBP', 800.00, 4, 'ACTIVE'),
    (11, 'RO49BANK0000000011RON', 5400.00, 3, 'ACTIVE'),
    -- Client 12 (accounts 28-29)
    (12, 'RO49BANK0000000012RON', 3600.00, 3, 'ACTIVE'),
    (12, 'RO49BANK0000000012EUR', 1900.00, 1, 'ACTIVE'),
    -- Client 13 (accounts 30-31)
    (13, 'RO49BANK0000000013USD', 1750.00, 2, 'ACTIVE'),
    (13, 'RO49BANK0000000013EUR', 2100.00, 1, 'ACTIVE'),
    -- Client 14 (accounts 32-33)
    (14, 'RO49BANK0000000014EUR', 2300.00, 1, 'ACTIVE'),
    (14, 'RO49BANK0000000014GBP', 1100.00, 4, 'ACTIVE'),
    -- Client 15 (accounts 34-35)
    (15, 'RO49BANK0000000015RON', 6200.00, 3, 'ACTIVE'),
    (15, 'RO49BANK0000000015USD', 1350.00, 2, 'ACTIVE'),
    -- Client 16 (accounts 36-37)
    (16, 'RO49BANK0000000016EUR', 2050.00, 1, 'ACTIVE'),
    (16, 'RO49BANK0000000016RON', 4700.00, 3, 'ACTIVE'),
    -- Client 17 (accounts 38-39)
    (17, 'RO49BANK0000000017GBP', 1250.00, 4, 'ACTIVE'),
    (17, 'RO49BANK0000000017EUR', 1850.00, 1, 'ACTIVE'),
    -- Client 18 (accounts 40-41)
    (18, 'RO49BANK0000000018USD', 1500.00, 2, 'ACTIVE'),
    (18, 'RO49BANK0000000018RON', 3900.00, 3, 'ACTIVE'),
    -- Client 19 (accounts 42-43)
    (19, 'RO49BANK0000000019EUR', 2600.00, 1, 'ACTIVE'),
    (19, 'RO49BANK0000000019USD', 1550.00, 2, 'ACTIVE'),
    -- Client 20 (accounts 44-45)
    (20, 'RO49BANK0000000020RON', 5100.00, 3, 'ACTIVE'),
    (20, 'RO49BANK0000000020GBP', 950.00, 4, 'ACTIVE'),
    -- Client 21 (account 46)
    (21, 'RO49BANK0000000021EUR', 1800.00, 1, 'ACTIVE'),
    -- Client 22 (account 47)
    (22, 'RO49BANK0000000022USD', 1400.00, 2, 'ACTIVE'),
    -- Client 23 (account 48)
    (23, 'RO49BANK0000000023RON', 3300.00, 3, 'ACTIVE'),
    -- Client 24 (account 49)
    (24, 'RO49BANK0000000024GBP', 870.00, 4, 'ACTIVE'),
    -- Client 25 (account 50)
    (25, 'RO49BANK0000000025EUR', 2500.00, 1, 'ACTIVE');

-- 4) Transactions (50 total)
-- transaction_type_id: 1=DEPOSIT, 2=WITHDRAW, 3=TRANSFER (from V2 seed order)
INSERT INTO "TRANSACTION" (account_id, transaction_type_id, amount, original_amount, original_currency_type_id, sign, details, transaction_date) VALUES
    (1, 1, 500.00, 500.00, 1, 'CREDIT', 'Initial deposit', NOW() - INTERVAL '30 days'),
    (2, 2, 200.00, 200.00, 2, 'DEBIT', 'ATM withdrawal', NOW() - INTERVAL '28 days'),
    (3, 1, 750.00, 750.00, 3, 'CREDIT', 'Salary', NOW() - INTERVAL '26 days'),
    (4, 3, 300.00, 300.00, 1, 'DEBIT', 'Transfer rent', NOW() - INTERVAL '25 days'),
    (5, 1, 600.00, 600.00, 4, 'CREDIT', 'Bonus', NOW() - INTERVAL '24 days'),
    (6, 2, 350.00, 350.00, 3, 'DEBIT', 'Utility bill', NOW() - INTERVAL '23 days'),
    (7, 1, 420.00, 420.00, 2, 'CREDIT', 'Freelance payment', NOW() - INTERVAL '22 days'),
    (8, 3, 250.00, 250.00, 1, 'DEBIT', 'Transfer to savings', NOW() - INTERVAL '21 days'),
    (9, 1, 900.00, 900.00, 3, 'CREDIT', 'Salary', NOW() - INTERVAL '20 days'),
    (10, 2, 150.00, 150.00, 1, 'DEBIT', 'Groceries', NOW() - INTERVAL '19 days'),
    (11, 1, 700.00, 700.00, 4, 'CREDIT', 'Dividend', NOW() - INTERVAL '18 days'),
    (12, 3, 400.00, 400.00, 3, 'DEBIT', 'Transfer to EUR account', NOW() - INTERVAL '17 days'),
    (13, 1, 650.00, 650.00, 3, 'CREDIT', 'Project bonus', NOW() - INTERVAL '16 days'),
    (14, 2, 220.00, 220.00, 1, 'DEBIT', 'Fuel', NOW() - INTERVAL '15 days'),
    (15, 3, 500.00, 500.00, 2, 'DEBIT', 'Transfer to USD account', NOW() - INTERVAL '14 days'),
    (16, 1, 480.00, 480.00, 3, 'CREDIT', 'Refund', NOW() - INTERVAL '13 days'),
    (17, 2, 130.00, 130.00, 1, 'DEBIT', 'Restaurant', NOW() - INTERVAL '12 days'),
    (18, 1, 520.00, 520.00, 3, 'CREDIT', 'Salary', NOW() - INTERVAL '11 days'),
    (19, 3, 275.00, 275.00, 2, 'DEBIT', 'Transfer to savings', NOW() - INTERVAL '10 days'),
    (20, 1, 610.00, 610.00, 1, 'CREDIT', 'Gift', NOW() - INTERVAL '9 days'),
    (21, 2, 190.00, 190.00, 3, 'DEBIT', 'Utilities', NOW() - INTERVAL '8 days'),
    (22, 1, 540.00, 540.00, 3, 'CREDIT', 'Salary', NOW() - INTERVAL '7 days'),
    (23, 3, 260.00, 260.00, 1, 'DEBIT', 'Transfer to EUR account', NOW() - INTERVAL '6 days'),
    (24, 1, 730.00, 730.00, 1, 'CREDIT', 'Bonus', NOW() - INTERVAL '5 days'),
    (25, 2, 210.00, 210.00, 2, 'DEBIT', 'Shopping', NOW() - INTERVAL '4 days'),
    (26, 1, 455.00, 455.00, 4, 'CREDIT', 'Freelance', NOW() - INTERVAL '3 days'),
    (27, 3, 300.00, 300.00, 3, 'DEBIT', 'Transfer to RON account', NOW() - INTERVAL '2 days'),
    (28, 1, 380.00, 380.00, 3, 'CREDIT', 'Salary advance', NOW() - INTERVAL '1 day'),
    (29, 2, 175.00, 175.00, 1, 'DEBIT', 'Gym', NOW()),
    (30, 1, 640.00, 640.00, 2, 'CREDIT', 'Commission', NOW()),
    (31, 2, 205.00, 205.00, 1, 'DEBIT', 'Pharmacy', NOW() - INTERVAL '2 days'),
    (32, 1, 590.00, 590.00, 1, 'CREDIT', 'Dividend', NOW() - INTERVAL '3 days'),
    (33, 3, 310.00, 310.00, 4, 'DEBIT', 'Transfer to GBP account', NOW() - INTERVAL '4 days'),
    (34, 1, 720.00, 720.00, 3, 'CREDIT', 'Salary', NOW() - INTERVAL '5 days'),
    (35, 2, 260.00, 260.00, 2, 'DEBIT', 'Electronics', NOW() - INTERVAL '6 days'),
    (36, 1, 410.00, 410.00, 1, 'CREDIT', 'Bonus', NOW() - INTERVAL '7 days'),
    (37, 2, 185.00, 185.00, 3, 'DEBIT', 'Bills', NOW() - INTERVAL '8 days'),
    (38, 1, 530.00, 530.00, 4, 'CREDIT', 'Project', NOW() - INTERVAL '9 days'),
    (39, 3, 295.00, 295.00, 1, 'DEBIT', 'Transfer to EUR account', NOW() - INTERVAL '10 days'),
    (40, 1, 605.00, 605.00, 2, 'CREDIT', 'Salary', NOW() - INTERVAL '11 days'),
    (41, 2, 215.00, 215.00, 3, 'DEBIT', 'Transport', NOW() - INTERVAL '12 days'),
    (42, 1, 675.00, 675.00, 1, 'CREDIT', 'Bonus', NOW() - INTERVAL '13 days'),
    (43, 3, 330.00, 330.00, 2, 'DEBIT', 'Transfer to USD account', NOW() - INTERVAL '14 days'),
    (44, 1, 495.00, 495.00, 3, 'CREDIT', 'Freelance', NOW() - INTERVAL '15 days'),
    (45, 2, 225.00, 225.00, 4, 'DEBIT', 'Subscriptions', NOW() - INTERVAL '16 days'),
    (46, 1, 560.00, 560.00, 1, 'CREDIT', 'Salary', NOW() - INTERVAL '17 days'),
    (47, 2, 240.00, 240.00, 2, 'DEBIT', 'Bills', NOW() - INTERVAL '18 days'),
    (48, 1, 585.00, 585.00, 3, 'CREDIT', 'Gift', NOW() - INTERVAL '19 days'),
    (49, 3, 345.00, 345.00, 4, 'DEBIT', 'Transfer to GBP account', NOW() - INTERVAL '20 days'),
    (50, 1, 700.00, 700.00, 1, 'CREDIT', 'Salary', NOW() - INTERVAL '21 days');
