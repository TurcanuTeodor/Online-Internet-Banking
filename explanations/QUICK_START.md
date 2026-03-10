# CashTactics - Quick Start Guide

## Pre-created Admin & User Accounts

After starting all services, you'll have these accounts:

### Admin Account
- **Email:** `admin@cashtactics.com`
- **Password:** `password`
- **Access:** Admin Dashboard at `/admin` (view all clients, accounts, transactions)

### User Account
- **Email:** `user@cashtactics.com`
- **Password:** `password`
- **Access:** Regular Dashboard at `/dashboard`

## Starting the Application

### 1. Start PostgreSQL
Ensure PostgreSQL is running on port 5432 with database `banking`.

### 2. Start Microservices (each in its own terminal)

```bash
# Terminal 1: Auth Service (port 8081)
cd services/auth-service
.\mvnw.cmd spring-boot:run

# Terminal 2: Client Service (port 8082)
cd services/client-service
.\mvnw.cmd spring-boot:run

# Terminal 3: Account Service (port 8083)
cd services/account-service
.\mvnw.cmd spring-boot:run

# Terminal 4: Transaction Service (port 8084)
cd services/transaction-service
.\mvnw.cmd spring-boot:run

# Terminal 5: Payment Service (port 8085)
cd services/payment-service
.\mvnw.cmd spring-boot:run

# Terminal 6: API Gateway (port 8443 - HTTPS)
cd services/api-gateway
.\mvnw.cmd spring-boot:run
```

### 3. Start Frontend (Port 5173)
```bash
cd frontend
npm run dev
```

### 4. Open in Browser
Navigate to: **http://localhost:5173**

All API calls go through the **API Gateway** at `https://localhost:8443` (configured in `frontend/vite.config.js`).

## Service Ports Summary

| Service | Port | Protocol |
|---|---|---|
| auth-service | 8081 | HTTP |
| client-service | 8082 | HTTP |
| account-service | 8083 | HTTP |
| transaction-service | 8084 | HTTP |
| payment-service | 8085 | HTTP |
| api-gateway | 8443 | HTTPS |
| frontend | 5173 | HTTP |
| PostgreSQL | 5432 | TCP |

## Features

### Regular User Dashboard
- View account information (multi-currency: EUR, USD, RON, GBP)
- Transaction history
- Transfer money between accounts

### Admin Dashboard (`/admin`)
- **Clients Tab** — See all clients with details
- **Accounts Tab** — View all accounts, freeze/unfreeze
- **Transactions Tab** — See all transactions across all accounts

## Database Info

- **Database:** `banking` (PostgreSQL)
- **Port:** 5432
- **User:** postgres
- **Schemas:** auth, clients, accounts, transactions, payments

Sample data includes:
- 25 clients
- 50 accounts (multiple currencies: EUR, USD, RON, GBP)
- 50 transactions

## API Endpoints (via Gateway)

Base URL: `https://localhost:8443/api`

### Auth (public)
- `POST /auth/login` — Login
- `POST /auth/register` — Register
- `POST /auth/2fa/setup` — Setup 2FA
- `POST /auth/2fa/confirm` — Confirm 2FA setup
- `POST /auth/2fa/verify` — Verify 2FA code
- `POST /auth/refresh-token` — Refresh access token
- `POST /auth/logout` — Logout

### Clients (JWT required)
- `GET /clients/view` — All clients
- `GET /clients/search?name=...` — Search by name
- `POST /clients` — Create client
- `PUT /clients/{id}/contact` — Update contact info

### Accounts (JWT required)
- `GET /accounts/view` — All accounts
- `GET /accounts/by-client/{clientId}` — Accounts by client
- `GET /accounts/{iban}/balance` — Balance by IBAN
- `POST /accounts/open` — Open account
- `POST /accounts/{id}/close` — Close account
- `POST /accounts/transfer` — Transfer money
- `POST /accounts/{id}/freeze` — Freeze/unfreeze

### Transactions (JWT required)
- `GET /transactions/view-all` — All transactions
- `GET /transactions/by-account/{id}` — By account
- `GET /transactions/between?from=...&to=...` — Between dates
- `GET /transactions/by-type/{code}` — By type
- `GET /transactions/daily-totals` — Daily totals
- `GET /transactions/flagged` — Flagged transactions
- `POST /transactions` — Create transaction

### Payments (JWT required)
- `POST /payments/create-intent` — Create Stripe payment intent
- `POST /payments/confirm/{id}` — Confirm payment
- `GET /payments/history` — Payment history
- `POST /payment-methods` — Add payment method
- `GET /payment-methods` — List payment methods

## Role-Based Access

The app automatically redirects based on role after login:
- **ADMIN** → `/admin` (Admin Dashboard)
- **USER** → `/dashboard` (User Dashboard)
