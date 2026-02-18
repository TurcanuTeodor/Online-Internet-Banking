# Online Internet Banking System

A full-stack online banking application built as a college project, demonstrating modern web development practices with Spring Boot backend and React frontend.

## 🚀 Quick Start

**Pre-created Test Accounts:**
- **Admin:** `admin@cashtactics.com` / `password123`
- **User:** `user@cashtactics.com` / `password123`

**Start Backend:**
```bash
cd backend
./mvnw spring-boot:run
# Runs on https://localhost:8443
```

**Start Frontend:**
```bash
cd frontend
npm install
npm run dev
# Runs on http://localhost:5173
```

**Access:** Open http://localhost:5173 and login with the accounts above.

**📚 Detailed Documentation:** See [backend/explanations/](backend/explanations/) for comprehensive guides on architecture, implementation, testing, and database schema.

---

## Table of Contents

- Project Overview
- Technology Stack
- Architecture & Components
- Security (JWT + Refresh Tokens + 2FA)
- Database & Migrations (Flyway)
- Running & Configuration
- API Endpoints & Examples
- Frontend
- Validation & Error Handling

---

## Project Overview

This is a **college project** demonstrating a secure online banking system with:

- **Backend:** REST API with Spring Boot, JWT authentication, 2FA support
- **Frontend:** React with modern hooks, Axios for API calls, TailwindCSS
- **Database:** PostgreSQL with Flyway migrations, all reference data as ENUMs
- **Security:** JWT with refresh tokens (auto-renewal), optional 2FA, HTTPS
- **Features:** Account management, money transfers, transaction history, fraud detection, AI categorization, admin dashboard

## Technology Stack

**Backend:**
- Java 17, Spring Boot 3
- Spring Security, Spring Data JPA
- PostgreSQL 16 with Flyway migrations
- JWT authentication with TOTP (2FA)
- Caffeine caching

**Frontend:**
- React 18 with Vite
- Axios (HTTP client with auto-refresh)
- TailwindCSS
- React Router

## Architecture & CRefresh Tokens + 2FA)

**JWT Authentication:**
- **Access Token:** Short-lived (15 minutes), used for API requests
- **Refresh Token:** Long-lived (7 days), stored in database, used to renew access tokens
- **Auto-Refresh:** Frontend automatically renews expired access tokens using refresh token
- **Token Rotation:** Old refresh tokens are revoked when new ones are issued
- Login returns both tokens; access tokens cannot be revoked but expire quickly

**Two-Factor Authentication (2FA):**
- Optional TOTP-based 2FA (Google Authenticator compatible)
- Flow: setup (generate secret + QR code) → confirm → verify with code
- When 2FA is enabled, login returns temp token → user enters 6-digit code → gets final tokens
- Tokens include `2fa=ok` claim when 2FA verification is complete

**Additional Security:**
- Passwords hashed with BCrypt
- User roles stored as PostgreSQL ENUM (`ROLE_ENUM`)
- HTTPS on port 8443 (self-signed certificate for development)
- Refresh tokens stored in database for revocation capability

## Database & Migrations (Flyway)

Flyway versioned migrations live in `src/main/resources/db/migration`:

- **V1:** Create PostgreSQL ENUMs
  - `ROLE_ENUM` (USER, ADMIN)
  - `ACCOUNT_STATUS_ENUM` (ACTIVE, CLOSED, SUSPENDED)  
  - `TRANSACTION_TYPE_ENUM` (DEPOSIT, WITHDRAWAL, TRANSFER_INTERNAL, TRANSFER_EXTERNAL)
  - `TRANSACTION_CATEGORY_ENUM` (FOOD, GROCERIES, TRANSPORT, SHOPPING, ENTERTAINMENT, etc.)
  - `CLIENT_TYPE_ENUM` (PF, PJ)
  - `SEX_TYPE_ENUM` (M, F, O)
  - `CURRENCY_ENUM` (EUR, USD, RON, GBP)

- **V2:** `CLIENT` table (first_name, last_name, client_type, sex_type, risk_level, active)
- **V3:** `CONTACT_INFO` table (email, phone, address, city, postal_code)
- **V4:** `USER` table (username_or_email, password_hash, role, 2FA fields)
- **V5:** `ACCOUNT` table (iban, balance, currency_code, status)
- **V6:** `TRANSACTION` table (amount, type, category, merchant, risk_score, flagged)
- **V7:** `CATEGORY_RULE` table (keyword-to-category mappings for AI categorization)
- **V8:** Fraud detection tables (`FRAUD_SCORE`, `FRAUD_ALERT`)
- **V9:** Read-only views (`VIEW_CLIENT`, `VIEW_ACCOUNT`, `VIEW_TRANSACTION`, `VIEW_FRAUD_DASHBOARD`)
- **V10:** Seed sample data (25 clients, 50 accounts, 50 transactions)
- **V11:** Add sample users with BCrypt hashed passwords
- **V12:** Create additional indexes for performance optimization
- **V13:** `REFRESH_TOKENS` table for refresh token storage

**Entity Highlights:**
- All reference data uses PostgreSQL ENUMs (not lookup tables)
- `User.role`, `Account.status`, `Transaction.transaction_type`, etc. are mapped using `@Enumerated(EnumType.STRING)` + `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`
- Java enums in `model.enums` match PostgreSQL ENUMs exactly
- View entities (`ViewClient`, `ViewAccount`, `ViewTransaction`, `ViewFraudDashboard`) map to SQL views
- Tables are quoted uppercase (e.g., `"ACCOUNT"`); `globally_quoted_identifiers=true` in config

**Fraud Detection Features:**
- Transaction risk scoring with component analysis (amount, time, category, frequency, device)
- Automatic flagging of suspicious transactions
- Fraud alerts with review workflow (PENDING, REVIEWED, CONFIRMED, FALSE_POSITIVE)
- AI-powered transaction categorization using keyword rules

**Caching (Caffeine):**
- **Exchange Rates:** Currency conversion rates fetched from ECB (European Central Bank) are cached for 10 minutes to reduce external API calls
- **Account Balances:** Account balance queries are cached with key `iban` to optimize frequent balance checks
- **Accounts by Client:** Client account lists are cached with key `clientId` to improve performance
- Cache configuration: `maximumSize=500, expireAfterAccess=10m` (500 entries max, 10-minute TTL)
- See `@Cacheable` annotations in `ExchangeRateService` and `AccountService`

## Running & Configuration

Environment: Java 17+ (runs fine on newer JDKs), Maven, PostgreSQL.

Configure in `src/main/resources/application.properties` (values are loaded from `.env.properties`):

```properties
spring.config.import=optional:file:.env.properties

# DataSource
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

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
server.ssl.enabled-protocols=TLSv1.3, TLSv1.2
server.port=${SERVER_PORT:8443}

# HikariCP Connection Pool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=30000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.connection-timeout=30000

# Caffeine Cache (for exchange rates)
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=500,expireAfterAccess=10m

# Exchange Rates (ECB)
app.fx.ecb-url=https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml

# JWT Configuration
app.jwt.secret=${JWT_SECRET}
app.jwt.expiration-minutes=${JWT_EXPIRATION_MINUTES}
app.jwt.temp-expiration-minutes=${JWT_TEMP_EXPIRATION_MINUTES}
app.jwt.issuer=${JWT_ISSUER}
app.jwt.refresh-token-days=${JWT_REFRESH_TOKEN_DAYS}

# 2FA Configuration
app.2fa.app-name=${TOTP_APP_NAME}
```

**Start the backend:**
```bash
./mvnw spring-boot:run
```

Flyway migrations run automatically on startup. The server listens on `https://localhost:8443` and `http://localhost:8080`.

## API Endpoints & Examples

Auth (2FA-capable)
- `POST /api/auth/register` → create user tied to an existing client
- `POST /api/auth/login` → returns final JWT or temp token if 2FA is enabled
- `POST /api/auth/2fa/setup` → get secret + QR
- `POST /api/auth/2fa/confirm` → enable 2FA
- `POST /api/auth/2fa/verify` → exchange temp token + TOTP for final JWT

**Clients:**
- `POST /api/clients` → create client
- `PUT /api/clients/{id}/contact` → update contact info
- `GET /api/clients/view` → list all clients (admin only, from VIEW_CLIENT)

**Accounts:**
- `POST /api/accounts/open` → open new account (default status ACTIVE)
- `POST /api/accounts/{iban}/deposit` → deposit money
- `POST /api/accounts/{iban}/withdraw` → withdraw money
- `POST /api/accounts/transfer` → transfer between accounts
- `GET /api/accounts/by-client/{clientId}` → get client's accounts
- `GET /api/accounts/{iban}/balance` → get account balance

**Transactions:**
- `GET /api/transactions/view-all` → all transactions (admin only)
- `GET /api/transactions/by-iban/{iban}` → transactions for an account
- `GET /api/transactions/by-client/{clientId}` → client's transactions
- `GET /api/transactions/by-type/{type}` → filter by transaction type
- `GET /api/transactions/between?from=YYYY-MM-DD&to=YYYY-MM-DD` → date range
- `GET /api/transactions/daily-totals` → aggregated daily totals

**Example: register → login → 2FA**

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
  "tempToken": "temp_token_from_login",
  "code": "123456"
}
```

**Example: transfer money**

```json
POST /api/accounts/transfer
{
  "fromIban": "RO49BANK0000000001EUR",
  "toIban": "RO49BANK0000000002EUR",
  "amount": 150.00
}
```

**Transaction Types:**
- `DEPOSIT` - Money added to account
- `WITHDRAWAL` - Money removed from account
- `TRANSFER_INTERNAL` - Between accounts in the system
- `TRANSFER_EXTERNAL` - To external bank accounts

**Transaction Categories:**
AI-powered categorization: FOOD, GROCERIES, TRANSPORT, SHOPPING, ENTERTAINMENT, HEALTH, TRAVEL, SUBSCRIPTIONS, INCOME, OTHERS

## Frontend

**Location:** `frontend/`

**Tech Stack:** React 18, Vite, TailwindCSS, Axios, React Router

**Key Features:**
- **Auto-Refresh:** `apiClient.js` automatically renews expired access tokens
- **Protected Routes:** Role-based routing (admin vs user dashboards)
- **2FA Support:** QR code display and code verification
- **Responsive Design:** TailwindCSS utilities

**Pages:**
- `/login` - User login
- `/register` - New user registration
- `/dashboard` - User dashboard (accounts, transactions)
- `/admin` - Admin dashboard (all clients, all transactions)
- `/2fa-verify` - Two-factor authentication code entry

**Services:**
- `authService.js` - Login, logout, register, 2FA, token refresh
- `apiClient.js` - Axios instance with request/response interceptors
- `accountService.js` - Account operations
- `transactionService.js` - Transaction operations

**Starting the Frontend:**
```bash
cd frontend
npm install
npm run dev
```
Access at: http://localhost:5173

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

## Troubleshooting

**Quoted identifiers:**
- Tables are created quoted/uppercase (e.g., `"ACCOUNT"`)
- `globally_quoted_identifiers=true` in config ensures Hibernate matches them

**PostgreSQL enums:**
- `User.role`, `Account.status`, etc. are true database ENUMs (not varchar)
- Use `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` for proper Hibernate binding

**Flyway migrations:**
- Flyway runs only new migrations
- To change schema, create a new migration (e.g., V14) with `ALTER TABLE` statements
- Never modify existing migration files after they've run

**Drop database with active sessions:**
```sql
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = 'banking' AND pid <> pg_backend_pid();

DROP DATABASE banking;
```

## Project Structure

```
Online-Internet-Banking/
├── backend/                    # Spring Boot backend
│   ├── src/main/java/         # Java source code
│   ├── src/main/resources/    # Application properties, migrations
│   ├── explanations/          # 📚 Detailed documentation
│   │   ├── README.md          # Documentation index
│   │   ├── QUICK_START.md     # Getting started guide
│   │   ├── ARCHITECTURE.md    # System architecture
│   │   ├── IMPLEMENTATION_GUIDE.md  # Implementation details
│   │   ├── TESTING_GUIDE.md   # Testing instructions
│   │   └── DATABASE.md        # Database schema
│   └── pom.xml                # Maven dependencies
├── frontend/                   # React frontend
│   ├── src/                   # React components and pages
│   ├── services/              # API client services
│   └── package.json           # npm dependencies
└── README.md                   # This file
```

## Detailed Documentation

For in-depth information, see the documentation in [backend/explanations/](backend/explanations/):

- **[QUICK_START.md](backend/explanations/QUICK_START.md)** - How to run the application
- **[ARCHITECTURE.md](backend/explanations/ARCHITECTURE.md)** - System design and architecture
- **[IMPLEMENTATION_GUIDE.md](backend/explanations/IMPLEMENTATION_GUIDE.md)** - Code implementation details
- **[TESTING_GUIDE.md](backend/explanations/TESTING_GUIDE.md)** - Testing guide with examples
- **[DATABASE.md](backend/explanations/DATABASE.md)** - Database schema and migrations

## Complete Setup Checklist

**Prerequisites:**
- Java 17+
- Node.js 16+
- PostgreSQL 16
- Maven

**Backend Setup:**
1. Create PostgreSQL database: `CREATE DATABASE banking;`
2. Create `.env.properties` in project root:
   ```properties
   DB_URL=jdbc:postgresql://localhost:5432/banking
   DB_USERNAME=postgres
   DB_PASSWORD=your_password
   SSL_KEYSTORE_PASSWORD=changeit
   SERVER_PORT=8443
   ```
3. Start backend: `cd backend && ./mvnw spring-boot:run`
4. Flyway migrations run automatically on startup

**Frontend Setup:**
1. Install dependencies: `cd frontend && npm install`
2. Start dev server: `npm run dev`
3. Access at http://localhost:5174

**Testing:**
- Login with `user@cashtactics.com` / `password123`
- Or login as admin: `admin@cashtactics.com` / `password123`
- Import Postman collection from `backend/src/main/resources/postman/postman_collection.json`

The backend API is at `https://localhost:8443/api/**` and requires JWT for most requests (except auth endpoints).

---

**Note:** This is a college project demonstrating full-stack development, security best practices, and modern web technologies. It includes features like JWT refresh tokens, two-factor authentication, role-based access control and a responsive React frontend. Fraud detection and AI-powered transaction categorization functionalities are work in progress.
