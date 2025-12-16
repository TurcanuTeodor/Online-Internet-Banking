# Online Internet Banking — Backend (Spring Boot)

## Table of Contents

- Project Overview
- Architecture & Components
- Security (JWT + 2FA + HTTPS)
- Database & Migrations (Flyway)
- Running & Configuration
- API Endpoints & Examples (incl. Auth)
- Validation & Error Handling
- Caching
- Troubleshooting (DB reset, quoting, enums)

---

## Project Overview

- REST backend with layered architecture (controller → service → repository → DB)
- Persistent data in PostgreSQL using JPA/Hibernate
- Authentication with JWT and optional 2FA (Google Authenticator compatible)
- Flyway manages schema and seed data (V1…V10)
- HTTPS enabled on port 8443 (self-signed keystore for dev)

## Architecture & Components

- Controllers: expose REST endpoints for auth, clients, accounts, transactions
- Services: business rules (open/close accounts, deposit/withdraw/transfer, validations)
- Repositories: Spring Data JPA for DB access
- Models (Entities): map to tables and views
- DTOs + Mappers: clean payloads between API and entities
- Exception handling: consistent JSON errors via `GlobalExceptionHandler`

Important classes (examples):
- `AccountService`: IBAN generation, balance updates, transaction logging
- `AuthService`: register, login, 2FA setup/confirm/verify
- `JwtAuthenticationFilter`: validates JWT and enforces 2FA claim for protected routes
- `SecurityConfig`: Spring Security configuration

## Security (JWT + 2FA + HTTPS)

- Login returns either a final JWT (no 2FA) or a short temp token (when 2FA is enabled)
- 2FA flow: setup (generate secret + QR) → confirm → verify; then tokens include `2fa=ok`
- `JwtAuthenticationFilter` checks signature, expiration, issuer, and requires `2fa=ok` for access if user has 2FA enabled
- Passwords are hashed; user roles use a PostgreSQL ENUM (`ROLE_ENUM`)
- HTTPS on 8443 with a PKCS12 keystore (for local dev, self‑signed)

## Database & Migrations (Flyway)

Flyway versioned migrations live in `src/main/resources/db/migration`:

- V1: PostgreSQL enums → `ROLE_ENUM`, `ACCOUNT_STATUS_ENUM`
- V2: `CURRENCY_TYPE`, `TRANSACTION_TYPE` (columns: `id`, `code`, `name`, timestamps)
  - Seed codes are short: `EUR`, `USD`, `RON`, `GBP` and `DEP` (Deposit), `RET` (Withdrawal), `TRF` (Transfer)
- V3: `SEX_TYPE`, `CLIENT_TYPE` (both have `code` + `name`; seeds: `M/F/O`, `PF/PJ`)
- V4: `CLIENT` (first_name, last_name, `client_type_id`, `sex_type_id`, `active`, audit)
- V5: `CONTACT_INFO` (email, phone, contact_person, website, address, city, postal_code)
- V6: `USER` (Postgres enum column `role ROLE_ENUM`)
- V7: `ACCOUNT` (status `ACCOUNT_STATUS_ENUM`, `currency_type_id` FK)
- V8: `TRANSACTION` (`transaction_type_id`, `original_currency_type_id`, amount/original_amount)
- V9: read‑only views → `VIEW_CLIENT`, `VIEW_ACCOUNT`, `VIEW_TRANSACTION`
- V10: seed sample data (25 clients, 50 accounts, 50 transactions)

Entity highlights:
- `User.role` and `Account.status` are mapped to Postgres enums using `@Enumerated(EnumType.STRING)` + `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`
- Lookup entities (`CurrencyType`, `TransactionType`, `ClientType`, `SexType`) have `code` and `name`
- View entities (`ViewClient`, `ViewAccount`, `ViewTransaction`) map to SQL views with aliased columns

Table naming: tables and views are quoted uppercase (e.g., `"ACCOUNT"`). In `application.properties` I set `spring.jpa.properties.hibernate.globally_quoted_identifiers=true` so Hibernate matches the quoted names.

## Running & Configuration

Environment: Java 17+ (runs fine on newer JDKs), Maven, PostgreSQL.

Configure in `src/main/resources/application.properties` (values are loaded from `.env.properties`):

```properties
spring.config.import=optional:file:.env.properties

# DataSource
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
spring.jpa.hibernate.naming.implicit-strategy=org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl
spring.jpa.properties.hibernate.globally_quoted_identifiers=true

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true

# HTTPS
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=tomcat
server.port=${SERVER_PORT:8443}
```

Run with Maven wrapper (Flyway runs automatically on startup):

```bash
./mvnw spring-boot:run
```

The server listens on HTTPS `https://localhost:8443`.

## API Endpoints & Examples

Auth (2FA-capable)
- `POST /api/auth/register` → create user tied to an existing client
- `POST /api/auth/login` → returns final JWT or temp token if 2FA is enabled
- `POST /api/auth/2fa/setup` → get secret + QR
- `POST /api/auth/2fa/confirm` → enable 2FA
- `POST /api/auth/2fa/verify` → exchange temp token + TOTP for final JWT

Clients
- `POST /api/clients` → create
- `PUT /api/clients/{id}/contact` → update contact info
- `GET /api/clients/view` → list from view (read‑only)

Accounts
- `POST /api/accounts/open` → open account (default status ACTIVE)
- `POST /api/accounts/{iban}/deposit`
- `POST /api/accounts/{iban}/withdraw`
- `POST /api/accounts/transfer`
- `GET /api/accounts/view`

Transactions
- `GET /api/transactions/view`
- `GET /api/transactions/account/{iban}`
- `GET /api/transactions/filter?...`

Example: register → login → 2FA

```json
// Register
POST /api/auth/register
{
  "clientId": 1,
  "usernameOrEmail": "john.doe@example.com",
  "password": "SecurePass123!"
}

// Login (returns temp token if 2FA is enabled)
POST /api/auth/login
{
  "usernameOrEmail": "john.doe@example.com",
  "password": "SecurePass123!"
}

// 2FA verify (use code from authenticator app)
POST /api/auth/2fa/verify
{
  "code": "123456"
}
```

Example: transfer

```json
POST /api/accounts/transfer
{
  "fromIban": "RO49BANK0000000001EUR",
  "toIban": "RO49BANK0000000002EUR",
  "amount": 150.00
}
```

Transaction types use short codes: `DEP`, `RET`, `TRF`.

## Validation & Error Handling

- DTOs use Bean Validation annotations (e.g., `@NotNull`, `@Email`, `@Size`)
- A global exception handler returns consistent JSON:

```json
{
  "timestamp": "2025-12-16T12:30:15Z",
  "status": 404,
  "error": "Resource Not Found",
  "message": "Client not found",
  "path": "/api/clients/123"
}
```

Auth errors use a dedicated exception type and respond with 401.

## Caching

- Caffeine cache for quick reads (e.g., account balances, accounts by client)
- Simple TTL configuration in `application.properties`

## Troubleshooting

Quoted identifiers
- Tables are created quoted/uppercase (e.g., `"ACCOUNT"`). I enabled `globally_quoted_identifiers` so Hibernate matches them.

PostgreSQL enums
- `User.role` and `Account.status` are true database enums (not varchar). I used `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` so Hibernate binds correctly.

Flyway reruns
- Flyway runs only new migrations. If I change an old migration, it won’t re‑apply unless I reset the DB or create a new version (e.g., V11) with `ALTER TABLE` statements.

Drop database with active sessions (psql):

```sql
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = 'banking' AND pid <> pg_backend_pid();

DROP DATABASE banking;
```

Seed data
- V10 inserts 25 clients, 50 accounts (some with multiple currencies), and 50 transactions. Rerun by resetting the DB or adding a new seed migration.

---

That’s it. This README summarizes what I built, how I wired security and database migrations, and how to run/test the project. If you want me to add diagrams or more examples, I can extend it.
