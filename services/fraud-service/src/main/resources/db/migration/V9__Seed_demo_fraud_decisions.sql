-- V9__Seed_demo_fraud_decisions.sql
-- Demo seed data: pre-built fraud decisions for UI walkthrough.
-- Safe to run on fresh stack (no explicit IDs, BIGSERIAL auto-generates).
-- Covers clientIds 1-5 (seeded users). ACCOUNT_ID refs seeded accounts 1-5.

INSERT INTO "FRAUD_DECISION"
("ACCOUNT_ID", "CLIENT_ID", "STATUS", "DECIDED_BY_TIER", "RISK_SCORE",
 "USER_RESOLUTION", "CREATED_AT", "UPDATED_AT", "AMOUNT", "CURRENCY_CODE", "RULE_HITS")
VALUES
(1, 1, 'BLOCK',          'TIER3_ML',          95.2, 'PENDING',        now() - interval '2 hours',  now() - interval '2 hours',  4500.0,  'EUR', 'ML_ANOMALY, HIGH_VELOCITY'),
(2, 2, 'FLAG',           'TIER1_RULES',        85.0, 'PENDING',        now() - interval '1 day',    now() - interval '1 day',    3200.0,  'EUR', 'SUSPICIOUS_IP, LARGE_AMOUNT'),
(3, 3, 'STEP_UP_REQUIRED','TIER2_BEHAVIORAL',  65.5, 'PENDING',        now() - interval '3 days',   now() - interval '3 days',   1500.0,  'EUR', 'NEW_DEVICE'),
(1, 1, 'MANUAL_REVIEW',  'TIER1_RULES',        50.0, 'PENDING',        now() - interval '5 days',   now() - interval '5 days',   800.0,   'EUR', 'UNUSUAL_TIME'),
(2, 2, 'ALLOW',          'TIER3_ML',           10.0, 'LEGITIMATE',     now() - interval '7 days',   now() - interval '7 days',   200.0,   'EUR', 'NONE'),
(4, 4, 'BLOCK',          'TIER3_ML',           98.1, 'FRAUD_REPORTED', now() - interval '8 days',   now() - interval '2 days',   5000.0,  'EUR', 'ML_ANOMALY, KNOWN_FRAUDSTER'),
(5, 5, 'FLAG',           'TIER2_BEHAVIORAL',   78.4, 'PENDING',        now() - interval '10 days',  now() - interval '10 days',  2750.0,  'EUR', 'VELOCITY_SPIKE'),
(3, 3, 'STEP_UP_REQUIRED','TIER2_BEHAVIORAL',  62.0, 'LEGITIMATE',     now() - interval '12 days',  now() - interval '11 days',  1200.0,  'EUR', 'NEW_DEVICE'),
(1, 1, 'BLOCK',          'TIER1_RULES',       100.0, 'PENDING',        now() - interval '14 days',  now() - interval '14 days', 10000.0,  'EUR', 'SANCTIONS_MATCH'),
(2, 2, 'MANUAL_REVIEW',  'TIER2_BEHAVIORAL',   55.0, 'PENDING',        now() - interval '15 days',  now() - interval '15 days',  950.0,   'EUR', 'GEO_VELOCITY'),
(4, 4, 'FLAG',           'TIER3_ML',           82.5, 'PENDING',        now() - interval '18 days',  now() - interval '18 days',  3800.0,  'EUR', 'ML_ANOMALY'),
(5, 5, 'STEP_UP_REQUIRED','TIER1_RULES',       68.0, 'PENDING',        now() - interval '20 days',  now() - interval '20 days',  1800.0,  'EUR', 'LARGE_AMOUNT'),
(3, 3, 'BLOCK',          'TIER3_ML',           91.0, 'FRAUD_REPORTED', now() - interval '22 days',  now() - interval '21 days',  4200.0,  'EUR', 'ML_ANOMALY, ACCOUNT_TAKEOVER'),
(1, 1, 'FLAG',           'TIER2_BEHAVIORAL',   75.0, 'PENDING',        now() - interval '25 days',  now() - interval '25 days',  2500.0,  'EUR', 'UNUSUAL_RECIPIENT'),
(2, 2, 'MANUAL_REVIEW',  'TIER1_RULES',        52.0, 'PENDING',        now() - interval '28 days',  now() - interval '28 days',  850.0,   'EUR', 'UNUSUAL_TIME');
