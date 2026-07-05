-- V6__Set_destination_accounts.sql
-- Add some destination_account_id values to the initially seeded internal transfers

UPDATE "TRANSACTION" SET destination_account_id = 2 WHERE id = 4;
UPDATE "TRANSACTION" SET destination_account_id = 3 WHERE id = 8;
UPDATE "TRANSACTION" SET destination_account_id = 1 WHERE id = 12;
UPDATE "TRANSACTION" SET destination_account_id = 2 WHERE id = 15;
UPDATE "TRANSACTION" SET destination_account_id = 4 WHERE id = 19;
UPDATE "TRANSACTION" SET destination_account_id = 1 WHERE id = 23;
UPDATE "TRANSACTION" SET destination_account_id = 5 WHERE id = 27;
UPDATE "TRANSACTION" SET destination_account_id = 2 WHERE id = 33;
UPDATE "TRANSACTION" SET destination_account_id = 1 WHERE id = 39;
UPDATE "TRANSACTION" SET destination_account_id = 3 WHERE id = 43;
UPDATE "TRANSACTION" SET destination_account_id = 1 WHERE id = 49;
