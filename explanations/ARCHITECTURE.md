# System Architecture

This document explains how the Online Banking System is structured and how its components work together.

## Overall System Design

The system follows a **microservices architecture** with an API Gateway as the single entry point:

```
┌─────────────────────────────────────────────────────────────────┐
│                     FRONTEND (React + Vite)                     │
├─────────────────────────────────────────────────────────────────┤
│  React Pages (Login.jsx, Dashboard.jsx, AdminDashboard, etc.)  │
│                    ↓                                            │
│  Service Layer (authService.js, apiClient.js, etc.)            │
│                    ↓                                            │
│  localStorage (jwt_token, refresh_token)                       │
└─────────────────────────────────────────────────────────────────┘
                       ↕ HTTPS (JSON)
┌─────────────────────────────────────────────────────────────────┐
│               API GATEWAY (Spring Cloud Gateway)                │
│                    Port 8443 (HTTPS/Netty)                      │
├─────────────────────────────────────────────────────────────────┤
│  JWT Pre-validation │ Rate Limiting │ Circuit Breaker │ CORS   │
│  Route forwarding   │ SSL Termination │ Fallback responses     │
└──────┬──────┬──────┬──────┬──────┬──────────────────────────────┘
       │      │      │      │      │
       ▼      ▼      ▼      ▼      ▼
┌────────┐┌────────┐┌────────┐┌────────┐┌────────┐
│  AUTH  ││ CLIENT ││ACCOUNT ││ TRANS. ││PAYMENT │
│ :8081  ││ :8082  ││ :8083  ││ :8084  ││ :8085  │
└───┬────┘└───┬────┘└───┬────┘└───┬────┘└───┬────┘
    │         │         │         │         │
    ▼         ▼         ▼         ▼         ▼
┌─────────────────────────────────────────────────────────────────┐
│                   DATABASE (PostgreSQL)                          │
│         Single instance, schema-per-service isolation           │
├─────────────────────────────────────────────────────────────────┤
│  Schema: auth │ clients │ accounts │ transactions │ payments   │
│  Tables: users, refresh_tokens, clients, accounts, etc.        │
└─────────────────────────────────────────────────────────────────┘
```

---

## Microservices

Each service is an independent Spring Boot application with its own schema in PostgreSQL.

### auth-service (Port 8081)
- **Responsibility:** Registration, login, JWT token management, 2FA, refresh tokens
- **Schema:** `auth` (users, refresh_tokens)
- **Public routes** — no JWT required for login/register
- **Endpoints:** `/api/auth/login`, `/api/auth/register`, `/api/auth/2fa/*`, `/api/auth/refresh-token`, `/api/auth/logout`

### client-service (Port 8082)
- **Responsibility:** Client profile lifecycle, contact info updates with step-up verification, view projection/masking, encryption migration/re-encryption
- **Schema:** `clients` (clients, contact_info)
- **Protected** — requires JWT
- **Endpoints:** `/api/clients/*`
- **Internal structure:** `ClientProfileService`, `ClientContactService`, `ClientViewProjectionService`, `ClientEncryptionLifecycleService`, `AuthStepUpClient`

### account-service (Port 8083)
- **Responsibility:** Account management, transfers, balance, exchange rates
- **Schema:** `accounts` (accounts, currency_type)
- **Protected** — requires JWT
- **Endpoints:** `/api/accounts/*`
- **Inter-service communication:** Calls transaction-service via REST to create transaction records on transfer

### transaction-service (Port 8084)
- **Responsibility:** Transaction history, queries, daily totals, flagged transactions
- **Schema:** `transactions` (transactions, transaction_type)
- **Protected** — requires JWT
- **Endpoints:** `/api/transactions/*`

### payment-service (Port 8085)
- **Responsibility:** Stripe payments, payment methods, webhooks
- **Schema:** `payments` (payments, payment_methods)
- **Protected** — requires JWT
- **Endpoints:** `/api/payments/*`, `/api/payment-methods/*`

### api-gateway (Port 8443)
- **Responsibility:** Central entry point, routing, JWT pre-validation, rate limiting, circuit breaker, CORS, SSL
- **Technology:** Spring Cloud Gateway (reactive, Netty)
- **Route configuration:** `GatewayConfig.java`
- **Filters:** `JwtAuthFilter` (pre-validates JWT), `RateLimitFilter` (50 req/sec via Resilience4j)
- **Fallback:** Returns 503 with message when a service is down (circuit breaker)

---

## Frontend Architecture

### Component Structure

```
Frontend Components
├── Pages (React Components)
│   ├── Login.jsx - User login page
│   ├── Register.jsx - New user registration
│   ├── TwoFactorVerify.jsx - 2FA verification
│   ├── Dashboard.jsx - User dashboard (accounts, transactions)
│   └── AdminDashboard/ - Admin view (clients, accounts, transactions)
│       ├── index.jsx - Main admin layout with tabs
│       ├── ClientsTab.jsx - All clients list
│       ├── AccountsTab.jsx - All accounts list
│       └── TransactionsTab.jsx - All transactions list
│
├── Services (API Communication)
│   ├── authService.js - Authentication (login, logout, 2FA)
│   ├── apiClient.js - HTTP client with auto-refresh
│   ├── accountService.js - Account operations
│   ├── transactionService.js - Transaction operations
│   └── clientService.js - Client data operations
│
└── Storage
    └── localStorage
        ├── jwt_token - Access token (15 min lifespan)
        └── refresh_token - Refresh token (7 day lifespan)
```

### How Auto-Refresh Works

The `apiClient.js` has a **response interceptor** that automatically handles expired tokens:

1. User makes an API call
2. If response is **401 Unauthorized** (token expired):
   - Automatically calls `/api/auth/refresh-token` with refresh token
   - Gets new access token
   - Retries the original request
   - User doesn't notice anything!

---

## Service Layer Architecture (per microservice)

Each microservice follows the same **layered architecture**:

```
Controller Layer → Service Layer → Repository Layer → Entity Layer
```

**1. Controller Layer** - Handles HTTP requests
- Receives JSON requests, validates input, calls service layer, returns JSON responses

**2. Service Layer** - Business logic
- Implements core functionality, handles transactions, coordinates operations

**3. Repository Layer** - Database access
- CRUD operations, custom database queries via Spring Data JPA

**4. Entity Layer** - Data models
- Represents database tables as Java classes, defines relationships

---

## Inter-Service Communication

### account-service → transaction-service
When a transfer is made, account-service:
1. Updates sender/receiver balances locally
2. Calls `POST http://localhost:8084/api/transactions` via RestTemplate
3. Forwards the incoming JWT token for authentication
4. Creates debit record (sender) and credit record (receiver)
5. If transaction-service is down, the transfer still succeeds but logs a warning

---

## API Gateway Details

### Route Configuration

| Route Pattern | Target | JWT Required | Circuit Breaker |
|---|---|---|---|
| `/api/auth/**` | auth-service :8081 | No | Yes (authCB) |
| `/api/clients/**` | client-service :8082 | Yes | Yes (clientCB) |
| `/api/accounts/**` | account-service :8083 | Yes | Yes (accountCB) |
| `/api/transactions/**` | transaction-service :8084 | Yes | Yes (transactionCB) |
| `/api/payments/**` | payment-service :8085 | Yes | Yes (paymentCB) |
| `/api/payment-methods/**` | payment-service :8085 | Yes | Yes (paymentCB) |

### Security Layers
1. **SSL/TLS** — Self-signed certificate (keystore.p12), HTTPS on port 8443
2. **JWT Pre-validation** — Gateway validates token signature before forwarding
3. **Rate Limiting** — 50 requests/second per gateway (Resilience4j)
4. **Circuit Breaker** — Fallback 503 response when service is down
5. **CORS** — Configured for frontend origin (localhost:5173)

---

## Data Flow Diagrams

### 1. Login Flow

```
User enters credentials
    ↓
Frontend → POST https://localhost:8443/api/auth/login
    ↓
Gateway forwards to auth-service :8081 (public route, no JWT check)
    ↓
auth-service validates username/password
    ↓
Generate 2 tokens:
  - Access token (JWT, 15 min) 
  - Refresh token (JWT, 7 days) → saved to database
    ↓
Return: { token, refreshToken, role, clientId }
    ↓
Frontend stores both tokens in localStorage
```

### 2. Protected API Call Flow

```
User navigates to Dashboard
    ↓
Frontend → GET https://localhost:8443/api/accounts/by-client/1
    ↓
Gateway: JwtAuthFilter validates token signature
    ↓ (valid)
Gateway forwards to account-service :8083
    ↓
account-service: SecurityConfig validates JWT again (full validation)
    ↓
Returns account data
```

### 3. Transfer Flow (Inter-Service)

```
User initiates transfer
    ↓
Frontend → POST https://localhost:8443/api/accounts/transfer
    ↓
Gateway validates JWT → forwards to account-service :8083
    ↓
account-service:
  1. Validates sender/receiver accounts
  2. Checks sufficient balance
  3. Updates balances (sender -amount, receiver +amount)
  4. Calls transaction-service :8084 with JWT forwarding:
     - POST /api/transactions (debit record for sender)
     - POST /api/transactions (credit record for receiver)
    ↓
Returns success
```

---

## Security Features

### Token-Based Authentication
- **Access Token**: Short-lived (15 min), stored in localStorage
  - Contains user info (role, clientId)
  - Verified on every API request (gateway + service)
  - Cannot be revoked (but expires quickly)

- **Refresh Token**: Long-lived (7 days), stored in database + localStorage
  - Can be revoked (logout functionality)
  - Used to get new access tokens
  - Tracked in database for security

### Two-Factor Authentication (2FA)
- Uses TOTP (Time-based One-Time Password)
- Compatible with Google Authenticator, Authy, etc.
- User scans QR code during setup
- Required on every login if enabled

### Password Storage
- Passwords are hashed using BCrypt
- Never stored in plain text
- Salt is automatically generated per password

---

## Why Microservices?

### Independent Deployment
- Each service can be updated/scaled independently
- A bug in payment-service doesn't affect auth-service

### Technology Flexibility
- API Gateway uses reactive WebFlux/Netty
- Other services use traditional Spring MVC/Tomcat

### Fault Isolation
- Circuit breaker prevents cascade failures
- If transaction-service goes down, accounts still work

### Schema Isolation
- Each service owns its data (schema-per-service)
- No cross-schema JOINs — explicit REST calls instead

---

This architecture provides a solid foundation for a secure, maintainable, and scalable online banking system.
