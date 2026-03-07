-- V4__Seed_sample_payments.sql
-- Sample payment data for development/testing

INSERT INTO "PAYMENT" (client_id, account_id, amount, currency, status, stripe_payment_intent_id, description, created_at) VALUES
    (1, 1, 150.00, 'EUR', 'COMPLETED', 'pi_test_001_completed', 'Electricity bill payment', NOW() - INTERVAL '20 days'),
    (1, 1, 75.50, 'EUR', 'COMPLETED', 'pi_test_002_completed', 'Internet subscription', NOW() - INTERVAL '18 days'),
    (2, 3, 200.00, 'RON', 'COMPLETED', 'pi_test_003_completed', 'Gas bill payment', NOW() - INTERVAL '15 days'),
    (2, 4, 320.00, 'USD', 'COMPLETED', 'pi_test_004_completed', 'Software license', NOW() - INTERVAL '12 days'),
    (3, 6, 89.99, 'EUR', 'COMPLETED', 'pi_test_005_completed', 'Gym membership', NOW() - INTERVAL '10 days'),
    (3, 6, 45.00, 'EUR', 'FAILED', 'pi_test_006_failed', 'Online purchase attempt', NOW() - INTERVAL '8 days'),
    (4, 8, 500.00, 'RON', 'COMPLETED', 'pi_test_007_completed', 'Insurance premium', NOW() - INTERVAL '7 days'),
    (5, 13, 1200.00, 'RON', 'COMPLETED', 'pi_test_008_completed', 'Rent payment', NOW() - INTERVAL '5 days'),
    (5, 14, 250.00, 'EUR', 'REFUNDED', 'pi_test_009_refunded', 'Cancelled order refund', NOW() - INTERVAL '3 days'),
    (1, 2, 99.00, 'USD', 'PENDING', 'pi_test_010_pending', 'Pending subscription renewal', NOW());

INSERT INTO "PAYMENT_METHOD" (client_id, stripe_payment_method_id, card_brand, card_last4, expiry_month, expiry_year, is_default, created_at) VALUES
    (1, 'pm_test_visa_4242', 'visa', '4242', 12, 2027, true, NOW() - INTERVAL '30 days'),
    (1, 'pm_test_mc_5555', 'mastercard', '5555', 6, 2028, false, NOW() - INTERVAL '25 days'),
    (2, 'pm_test_visa_1234', 'visa', '1234', 3, 2027, true, NOW() - INTERVAL '20 days'),
    (3, 'pm_test_amex_0005', 'amex', '0005', 9, 2028, true, NOW() - INTERVAL '15 days'),
    (4, 'pm_test_visa_9876', 'visa', '9876', 1, 2027, true, NOW() - INTERVAL '10 days'),
    (5, 'pm_test_mc_1111', 'mastercard', '1111', 8, 2028, true, NOW() - INTERVAL '5 days');
