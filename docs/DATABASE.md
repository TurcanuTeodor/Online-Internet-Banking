# Database Guide

This document explains the database schema, migrations, and how data is organized.

## Database Overview

**Database:** PostgreSQL 17  
**Name:** `banking`  
**Migration Tool:** Flyway (runs automatically per service on startup)

The database uses:
- **Schema-per-service isolation** — each microservice has its own schema
- **ENUM types** for fixed values (roles, account status)
- **Lookup tables** for reference data (currencies, transaction types)
- **Foreign keys** to maintain relationships within each schema
- **Indexes** for fast queries
- **Views** for read-only dashboards

---

## Schema Organization

Each microservice manages its own schema via Flyway migrations:

| Service | Schema | Tables |
|---|---|---|
| auth-service | `auth` | users, refresh_tokens |
| client-service | `clients` | clients, contact_info, client_type, sex_type |
| account-service | `accounts` | accounts, currency_type |
| transaction-service | `transactions` | transactions, transaction_type |
| payment-service | `payments` | payments, payment_methods |

Migration files location: `services/{service-name}/src/main/resources/db/migration/`

---

## Database Migrations

Migrations are SQL files that create/modify the database schema. They run automatically when each service starts.

**Naming convention:** `V{number}__{description}.sql`

**How Flyway works:**
1. Each service sets `spring.flyway.schemas={schema_name}` in its config
2. Checks which migrations have run (stored in `flyway_schema_history` table per schema)
3. Runs new migrations in order
4. Records completion
5. Never reruns completed migrations

---

## Core Tables

### users (auth schema)
Stores user login credentials and settings.

```sql
CREATE TABLE auth.users (
    id SERIAL PRIMARY KEY,
    username_or_email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ROLE_ENUM NOT NULL,  -- ENUM: USER or ADMIN
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    two_factor_secret VARCHAR(255),
    client_id INTEGER
);
```

**Key points:**
- `role` uses PostgreSQL ENUM (not a string!)
- `password_hash` is BCrypt-hashed, never plain text
- `two_factor_secret` is null until 2FA is enabled
- `client_id` references the client in client-service (logical, not FK across schemas)

### refresh_tokens (auth schema)
Stores refresh tokens for authentication.

```sql
CREATE TABLE auth.refresh_tokens (
    id SERIAL PRIMARY KEY,
    token VARCHAR(2048) UNIQUE NOT NULL,
    user_id INTEGER NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expiry_date TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL
);
```

### clients (clients schema)
Stores personal information about bank clients.

```sql
CREATE TABLE clients.clients (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    cnp VARCHAR(13) UNIQUE NOT NULL,
    birth_date DATE,
    client_type_id INTEGER REFERENCES clients.client_type(id),
    sex_type_id INTEGER REFERENCES clients.sex_type(id),
    active BOOLEAN DEFAULT TRUE
);
```

### accounts (accounts schema)
Bank accounts that hold money.

```sql
CREATE TABLE accounts.accounts (
    id SERIAL PRIMARY KEY,
    client_id INTEGER NOT NULL,
    iban VARCHAR(34) UNIQUE NOT NULL,
    balance DECIMAL(15, 2) DEFAULT 0.00,
    currency_type_id INTEGER REFERENCES accounts.currency_type(id),
    status ACCOUNT_STATUS_ENUM NOT NULL,
    opened_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### transactions (transactions schema)
Records of money transfers.

```sql
CREATE TABLE transactions.transactions (
    id SERIAL PRIMARY KEY,
    account_id INTEGER NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    transaction_type_id INTEGER REFERENCES transactions.transaction_type(id),
    recipient_iban VARCHAR(34),
    description VARCHAR(255),
    original_currency_type_id INTEGER
);
```

---

## Lookup Tables

### currency_type (accounts schema)
```sql
CREATE TABLE accounts.currency_type (
    id SERIAL PRIMARY KEY,
    code VARCHAR(3) UNIQUE NOT NULL,  -- EUR, USD, RON, GBP
    name VARCHAR(50)
);
```

### transaction_type (transactions schema)
```sql
CREATE TABLE transactions.transaction_type (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL  -- DEPOSIT, WITHDRAWAL, TRANSFER, PAYMENT
);
```

### sex_type, client_type (clients schema)
Reference data for client personal information.

---

## ENUM Types

PostgreSQL ENUMs provide type safety at the database level.

- **ROLE_ENUM:** `'USER'`, `'ADMIN'`
- **ACCOUNT_STATUS_ENUM:** `'ACTIVE'`, `'CLOSED'`, `'SUSPENDED'`

---

## Indexes

Indexes make queries faster:

```sql
-- Fast user lookup
CREATE INDEX idx_users_username ON auth.users(username_or_email);
CREATE INDEX idx_refresh_tokens_token ON auth.refresh_tokens(token);

-- Fast account lookup
CREATE INDEX idx_accounts_iban ON accounts.accounts(iban);
CREATE INDEX idx_accounts_client ON accounts.accounts(client_id);

-- Fast transaction queries
CREATE INDEX idx_transactions_account ON transactions.transactions(account_id);
CREATE INDEX idx_transactions_timestamp ON transactions.transactions(timestamp);
```

---

## Database Relationships

```
clients.clients
   ↓ (1:1, logical)
auth.users (client_id references clients.clients.id)
   
clients.clients
   ↓ (1:N, logical)
accounts.accounts (client_id references clients.clients.id)
   ↓ (1:N, logical)
transactions.transactions (account_id references accounts.accounts.id)

auth.users
   ↓ (1:N)
auth.refresh_tokens
```

**Note:** Cross-schema references are logical (same IDs, no FK constraints across schemas). Within each schema, FK constraints are enforced.

---

## Sample Data

Each service seeds its own sample data via Flyway migrations:
- **25 sample clients** with realistic Romanian names
- **50 sample accounts** in EUR, USD, RON, GBP
- **50 sample transactions**
- **2 pre-configured users:**
  - `admin@cashtactics.com` / `password` (ADMIN, clientId=1)
  - `user@cashtactics.com` / `password` (USER, clientId=2)

---

## Database Management

### Reset database (fresh start)
```sql
DROP DATABASE IF EXISTS banking;
CREATE DATABASE banking;
-- Then restart all services, each Flyway will recreate its schema
```

### Check migration history per service
```sql
SELECT * FROM auth.flyway_schema_history ORDER BY installed_rank;
SELECT * FROM clients.flyway_schema_history ORDER BY installed_rank;
-- etc.
```

---

## Schema Design Principles

### Schema-per-Service
- Each microservice owns its data
- No cross-schema JOINs
- Services communicate via REST APIs

### Data Integrity
- FK constraints within each schema
- ENUMs prevent invalid values
- NOT NULL prevents missing required data
- UNIQUE prevents duplicates

### Performance
- Indexes on frequently queried columns
- Appropriate data types (VARCHAR vs TEXT)
- PostgreSQL with proper indexing handles the query load

---

This database schema provides a solid foundation for a banking system with proper relationships, data integrity, and schema isolation per microservice.
