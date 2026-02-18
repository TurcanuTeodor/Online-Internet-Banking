# Database Guide

This document explains the database schema, migrations, and how data is organized.

## Database Overview

**Database:** PostgreSQL 16  
**Name:** `banking`  
**Migration Tool:** Flyway (runs automatically on startup)

The database uses:
- **ENUM types** for fixed values (roles, account status)
- **Lookup tables** for reference data (currencies, transaction types)
- **Foreign keys** to maintain relationships
- **Indexes** for fast queries
- **Views** for read-only dashboards

---

## Database Migrations

Migrations are SQL files that create/modify the database schema. They run automatically when the backend starts.

**Location:** `backend/src/main/resources/db/migration/`

### Migration Files (in order)

1. **V1__Create_basic_enums.sql** - Create ENUM types
2. **V2__Create_client_table.sql** - Create clients table
3. **V3__Create_contact_info_table.sql** - Client contact info
4. **V4__Create_user_table.sql** - User accounts for login
5. **V5__Create_account_table.sql** - Bank accounts
6. **V6__Create_transaction_table.sql** - Money transactions
7. **V7__Create_category_rules_table.sql** - Transaction categorization
8. **V8__Create_fraud_detection_tables.sql** - Fraud monitoring
9. **V9__Create_views.sql** - Read-only views for dashboards
10. **V10__Seed_sample_data.sql** - Sample clients/accounts
11. **V11__Add_sample_users.sql** - Pre-created test users
12. **V12__Create_indexes_and_final_setup.sql** - Performance indexes
13. **V13__Create_refresh_tokens_table.sql** - Refresh token storage

**Naming convention:** `V{number}__{description}.sql`

**How Flyway works:**
1. Checks which migrations have run (stored in `flyway_schema_history` table)
2. Runs new migrations in order
3. Records completion
4. Never reruns completed migrations

---

## Core Tables

### users
Stores user login credentials and settings.

```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username_or_email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ROLE_ENUM NOT NULL,  -- ENUM: USER or ADMIN
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    two_factor_secret VARCHAR(255),
    client_id INTEGER REFERENCES clients(id)
);
```

**Key points:**
- `role` uses PostgreSQL ENUM (not a string!)
- `password_hash` is BCrypt-hashed, never plain text
- `two_factor_secret` is null until 2FA is enabled
- One user per client (1:1 relationship)

### clients
Stores personal information about bank clients.

```sql
CREATE TABLE clients (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    cnp VARCHAR(13) UNIQUE NOT NULL,  -- SSN equivalent
    birth_date DATE,
    client_type_id INTEGER REFERENCES client_type(id),
    sex_type_id INTEGER REFERENCES sex_type(id),
    active BOOLEAN DEFAULT TRUE
);
```

**Key points:**
- Separate from users (one client can potentially have multiple accounts)
- CNP is unique national identifier
- Uses lookup tables for client_type and sex_type

### accounts
Bank accounts that hold money.

```sql
CREATE TABLE accounts (
    id SERIAL PRIMARY KEY,
    client_id INTEGER REFERENCES clients(id),
    iban VARCHAR(34) UNIQUE NOT NULL,
    balance DECIMAL(15, 2) DEFAULT 0.00,
    currency_type_id INTEGER REFERENCES currency_type(id),
    status ACCOUNT_STATUS_ENUM NOT NULL,  -- ACTIVE, CLOSED, SUSPENDED
    opened_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Key points:**
- One client can have multiple accounts
- Balance is stored with 2 decimal precision
- IBAN is unique identifier for each account
- Status uses ENUM for type safety

### transactions
Records of money transfers.

```sql
CREATE TABLE transactions (
    id SERIAL PRIMARY KEY,
    account_id INTEGER REFERENCES accounts(id),
    amount DECIMAL(15, 2) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    transaction_type_id INTEGER REFERENCES transaction_type(id),
    recipient_iban VARCHAR(34),
    description VARCHAR(255),
    original_currency_type_id INTEGER REFERENCES currency_type(id)
);
```

**Key points:**
- Every transaction belongs to one account
- Types: DEPOSIT, WITHDRAWAL, TRANSFER, PAYMENT
- Recipient IBAN can be null (for deposits/withdrawals)
- Stores original currency for exchange tracking

### refresh_tokens
Stores refresh tokens for authentication.

```sql
CREATE TABLE refresh_tokens (
    id SERIAL PRIMARY KEY,
    token VARCHAR(2048) UNIQUE NOT NULL,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expiry_date TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP
);
```

**Key points:**
- Created on login
- 7-day lifetime (configurable)
- Can be revoked (logout)
- Deleted if user is deleted (CASCADE)

---

## Lookup Tables

Lookup tables store reference data (like dropdown options).

### currency_type
```sql
CREATE TABLE currency_type (
    id SERIAL PRIMARY KEY,
    code VARCHAR(3) UNIQUE NOT NULL,  -- EUR, USD, RON, GBP
    name VARCHAR(50)
);
```

### transaction_type
```sql
CREATE TABLE transaction_type (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL  -- DEPOSIT, WITHDRAWAL, TRANSFER, PAYMENT
);
```

### sex_type
```sql
CREATE TABLE sex_type (
    id SERIAL PRIMARY KEY,
    code CHAR(1) UNIQUE NOT NULL,  -- M, F
    description VARCHAR(20)
);
```

### client_type
```sql
CREATE TABLE client_type (
    id SERIAL PRIMARY KEY,
    code VARCHAR(2) UNIQUE NOT NULL,  -- PF (person), PJ (company)
    description VARCHAR(50)
);
```

**Why lookup tables?**
- Can add new values without code changes
- Can store additional info (descriptions, icons, etc.)
- Can be internationalized
- Better for data that might change

---

## ENUM Types

PostgreSQL ENUMs provide type safety at the database level.

### ROLE_ENUM
```sql
CREATE TYPE ROLE_ENUM AS ENUM ('USER', 'ADMIN');
```

Used in `users` table. Only these two values allowed.

### ACCOUNT_STATUS_ENUM
```sql
CREATE TYPE ACCOUNT_STATUS_ENUM AS ENUM ('ACTIVE', 'CLOSED', 'SUSPENDED');
```

Used in `accounts` table.

**ENUM vs Lookup Table:**
- **Use ENUM for:** Fixed values that never change (roles, status)
- **Use Lookup Table for:** Business data that might evolve (currencies, types)

---

## Views (Read-Only)

Views are like "saved queries" - they don't store data, just provide a convenient way to read it.

### VIEW_CLIENT
Shows all clients with their type descriptions.

```sql
CREATE VIEW view_client AS
SELECT 
    c.id AS client_id,
    c.first_name AS client_first_name,
    c.last_name AS client_last_name,
    c.cnp,
    c.birth_date,
    ct.description AS client_type,
    st.description AS sex,
    c.active AS client_active
FROM clients c
LEFT JOIN client_type ct ON c.client_type_id = ct.id
LEFT JOIN sex_type st ON c.sex_type_id = st.id;
```

### VIEW_ACCOUNT
Shows all accounts with client and currency info.

```sql
CREATE VIEW view_account AS
SELECT 
    a.id AS account_id,
    a.iban AS account_iban,
    a.balance AS account_balance,
    a.status AS account_status,
    c.first_name || ' ' || c.last_name AS client_name,
    cur.code AS currency_code,
    a.opened_date AS account_opened_date
FROM accounts a
LEFT JOIN clients c ON a.client_id = c.id
LEFT JOIN currency_type cur ON a.currency_type_id = cur.id;
```

### VIEW_TRANSACTION
Shows all transactions with readable type names.

```sql
CREATE VIEW view_transaction AS
SELECT 
    t.id AS transaction_id,
    t.amount AS transaction_amount,
    t.timestamp AS transaction_timestamp,
    tt.name AS transaction_type_name,
    t.recipient_iban,
    t.description,
    a.iban AS account_iban
FROM transactions t
LEFT JOIN transaction_type tt ON t.transaction_type_id = tt.id
LEFT JOIN accounts a ON t.account_id = a.id;
```

**Why use views?**
- Simplify complex joins
- Provide consistent data formatting
- Used by admin dashboard (read-only)
- Don't duplicate data

---

## Database Relationships

```
clients
   ↓ (1:1)
users
   
clients
   ↓ (1:N)
accounts
   ↓ (1:N)
transactions

users
   ↓ (1:N)
refresh_tokens
```

**Relationship types:**
- **1:1** - One client has one user account
- **1:N** - One client can have many accounts
- **1:N** - One account can have many transactions
- **1:N** - One user can have many refresh tokens

---

## Indexes

Indexes make queries faster by creating a "lookup table" for specific columns.

**Created in V12:**
```sql
-- Fast user lookup
CREATE INDEX idx_users_username ON users(username_or_email);

-- Fast account lookup
CREATE INDEX idx_accounts_iban ON accounts(iban);
CREATE INDEX idx_accounts_client ON accounts(client_id);

-- Fast transaction queries
CREATE INDEX idx_transactions_account ON transactions(account_id);
CREATE INDEX idx_transactions_timestamp ON transactions(timestamp);

-- Fast refresh token lookup
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
```

**When to use indexes:**
- Columns used in WHERE clauses
- Columns used in JOINs
- Columns used for sorting (ORDER BY)

**Don't overuse indexes:**
- They slow down INSERT/UPDATE
- They take up disk space

---

## Sample Data

**V10** seeds 25 sample clients and 50 sample accounts:
- Clients 1-25 with realistic Romanian names
- Accounts in EUR, USD, RON, GBP
- Various balances

**V11** creates pre-configured users:
- `admin@cashtactics.com` / `password123` (ADMIN)
- `user@cashtactics.com` / `password123` (USER)

---

## Common Queries

### Find all accounts for a client
```sql
SELECT * FROM accounts WHERE client_id = 1;
```

### Find all transactions for an account
```sql
SELECT * FROM transactions 
WHERE account_id = 1 
ORDER BY timestamp DESC;
```

### Check refresh token status
```sql
SELECT 
    token,
    created_at,
    expiry_date,
    CASE 
        WHEN revoked_at IS NOT NULL THEN 'REVOKED'
        WHEN expiry_date < NOW() THEN 'EXPIRED'
        ELSE 'ACTIVE'
    END as status
FROM refresh_tokens
WHERE user_id = 1;
```

### Get account balance in all currencies
```sql
SELECT 
    ct.code AS currency,
    SUM(a.balance) AS total_balance
FROM accounts a
JOIN currency_type ct ON a.currency_type_id = ct.id
WHERE client_id = 1
GROUP BY ct.code;
```

### Admin view: All clients with account count
```sql
SELECT 
    c.id,
    c.first_name || ' ' || c.last_name AS name,
    COUNT(a.id) AS account_count,
    COALESCE(SUM(a.balance), 0) AS total_balance
FROM clients c
LEFT JOIN accounts a ON c.id = a.client_id AND a.status = 'ACTIVE'::ACCOUNT_STATUS_ENUM
GROUP BY c.id, c.first_name, c.last_name
ORDER BY c.id;
```

---

## Database Management

### Reset database (fresh start)
```sql
DROP DATABASE IF EXISTS banking;
CREATE DATABASE banking;
-- Then restart backend, Flyway will recreate everything
```

### Check migration history
```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

### Clean up expired refresh tokens
```sql
DELETE FROM refresh_tokens 
WHERE expiry_date < NOW() 
   OR revoked_at IS NOT NULL;
```

### Manually create a test user
```sql
-- First, ensure client exists (clients are created by V10)
INSERT INTO users (username_or_email, password_hash, role, client_id)
VALUES (
    'test@example.com',
    '$2a$10$...hashed_password_here...',  -- Use BCrypt
    'USER'::ROLE_ENUM,
    3  -- Must match existing client
);
```

---

## Schema Design Principles

### Normalization
- No duplicate data
- Each table has a single purpose
- Relationships through foreign keys

### Data Integrity
- Foreign keys prevent orphaned records
- ENUMs prevent invalid values
- NOT NULL prevents missing required data
- UNIQUE prevents duplicates

### Performance
- Indexes on frequently queried columns
- Views for complex queries
- Appropriate data types (VARCHAR vs TEXT)

### Security
- Passwords are hashed
- Sensitive data (2FA secrets) stored separately
- Cascade deletes prevent orphaned data

---

## Understanding Java ↔ Database Mapping

### Entities (JPA)
Java classes annotated with `@Entity` map to database tables:

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "username_or_email", unique = true)
    private String usernameOrEmail;
    
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ROLE_ENUM")
    private Role role;  // Java enum → PostgreSQL ENUM
}
```

### Repositories
Interfaces that provide database operations:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameOrEmail(String usernameOrEmail);
    // Spring generates SQL: SELECT * FROM users WHERE username_or_email = ?
}
```

---

## Troubleshooting

### Migration failed
- Check `flyway_schema_history` for errors
- Fix the migration file
- May need to manually repair Flyway or reset database

### Duplicate key error
- Unique constraint violated
- Check for existing record before inserting

### Foreign key constraint error
- Trying to reference non-existent record
- Ensure referenced record exists first

### ENUM value error
- Trying to use value not in ENUM definition
- Check Java enum matches database ENUM

---

This database schema provides a solid foundation for a banking system with proper relationships, data integrity, and performance optimization.
