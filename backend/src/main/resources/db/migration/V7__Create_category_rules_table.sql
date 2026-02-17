-- V7__Create_category_rules_table.sql
-- Stores keyword-to-category mappings for AI category prediction

CREATE TABLE "CATEGORY_RULE" (
    id BIGSERIAL PRIMARY KEY,
    keyword VARCHAR(255) NOT NULL UNIQUE,
    category TRANSACTION_CATEGORY_ENUM NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert common rules for category prediction
INSERT INTO "CATEGORY_RULE" (keyword, category) VALUES
-- FOOD
('starbucks', 'FOOD'),
('mcdonald', 'FOOD'),
('burger', 'FOOD'),
('pizza', 'FOOD'),
('restaurant', 'FOOD'),
('cafe', 'FOOD'),
('coffee', 'FOOD'),

-- GROCERIES
('walmart', 'GROCERIES'),
('carrefour', 'GROCERIES'),
('tesco', 'GROCERIES'),
('lidl', 'GROCERIES'),
('aldi', 'GROCERIES'),
('supermarket', 'GROCERIES'),
('grocery', 'GROCERIES'),
('market', 'GROCERIES'),

-- TRANSPORT
('uber', 'TRANSPORT'),
('lyft', 'TRANSPORT'),
('taxi', 'TRANSPORT'),
('bus', 'TRANSPORT'),
('train', 'TRANSPORT'),
('airline', 'TRANSPORT'),
('gas station', 'TRANSPORT'),
('fuel', 'TRANSPORT'),
('parking', 'TRANSPORT'),
('metro', 'TRANSPORT'),

-- SHOPPING
('amazon', 'SHOPPING'),
('ebay', 'SHOPPING'),
('aliexpress', 'SHOPPING'),
('zara', 'SHOPPING'),
('h&m', 'SHOPPING'),
('nike', 'SHOPPING'),
('adidas', 'SHOPPING'),
('mall', 'SHOPPING'),
('store', 'SHOPPING'),
('boutique', 'SHOPPING'),

-- ENTERTAINMENT
('netflix', 'SUBSCRIPTIONS'),
('spotify', 'SUBSCRIPTIONS'),
('hbo', 'SUBSCRIPTIONS'),
('disney', 'SUBSCRIPTIONS'),
('cinema', 'ENTERTAINMENT'),
('movie', 'ENTERTAINMENT'),
('theater', 'ENTERTAINMENT'),
('concert', 'ENTERTAINMENT'),
('game', 'ENTERTAINMENT'),
('steam', 'ENTERTAINMENT'),

-- HEALTH
('pharmacy', 'HEALTH'),
('doctor', 'HEALTH'),
('hospital', 'HEALTH'),
('medical', 'HEALTH'),
('dental', 'HEALTH'),
('clinic', 'HEALTH'),

-- TRAVEL
('booking', 'TRAVEL'),
('airbnb', 'TRAVEL'),
('hotels', 'TRAVEL'),
('flight', 'TRAVEL'),
('resort', 'TRAVEL'),
('hostel', 'TRAVEL'),

-- SUBSCRIPTIONS
('monthly', 'SUBSCRIPTIONS'),
('subscription', 'SUBSCRIPTIONS'),
('membership', 'SUBSCRIPTIONS'),
('premium', 'SUBSCRIPTIONS'),

-- INCOME (salary, transfers in)
('salary', 'INCOME'),
('bonus', 'INCOME'),
('refund', 'INCOME'),
('transfer in', 'INCOME');

CREATE INDEX idx_category_rule_keyword ON "CATEGORY_RULE"(keyword);
CREATE INDEX idx_category_rule_category ON "CATEGORY_RULE"(category);
