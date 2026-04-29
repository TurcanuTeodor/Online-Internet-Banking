# Online Banking System - Documentation

This is a college project demonstrating a full-stack online banking application with microservices architecture and modern security features.

## Documentation Files

### Quick Start
- **[QUICK_START.md](QUICK_START.md)** - Get the app running in minutes
  - Pre-created test accounts
  - How to start all services
  - Basic features overview

### Technical Guides
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - System design, microservices, API Gateway
- **[IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)** - Implementation details for key features
- **[TESTING_GUIDE.md](TESTING_GUIDE.md)** - How to test the application
- **[DATABASE.md](DATABASE.md)** - Database schema, migrations, schema-per-service
- **[REDIS_CACHING.md](REDIS_CACHING.md)** - Redis cache, Pub/Sub, Redisson locks, gateway blacklist and rate limiting
- **[SECURITY_KEYS_AND_ALGORITHMS.md](SECURITY_KEYS_AND_ALGORITHMS.md)** - Keys, tokens, encryption/hashing algorithms
- **[STRIPE_TOP_UP_SEQUENCE.md](STRIPE_TOP_UP_SEQUENCE.md)** - Thesis-oriented sequence diagram: Stripe card top-up (Browser → Gateway → services)

## Project Features

### Architecture
- **Microservices** — 5 business services + API Gateway
- **API Gateway** — Spring Cloud Gateway with JWT validation, rate limiting, circuit breaker
- **Redis** — L2 cache, Pub/Sub invalidation, distributed locks, token blacklist and rate limiting
- **Schema-per-service** — Each service owns its database schema
- **Inter-service communication** — REST with JWT forwarding

### Authentication & Security
- User registration and login
- JWT-based authentication with refresh tokens
- Two-factor authentication (2FA) with TOTP
- Role-based access control (Admin/User)
- SSL/TLS supported (gateway may run HTTP in Docker dev override)

### Banking Features
- Multiple account types and currencies (EUR, USD, RON, GBP)
- Money transfers between accounts with exchange rates
- Transaction history and categorization
- Account balance tracking
- Stripe payment integration

### Admin Features
- View all clients and their accounts
- View all transactions across the system
- Freeze/unfreeze accounts
- Admin dashboard with tabs

## Technology Stack

### Backend (Microservices)
- **Java 21** with **Spring Boot 3.3**
- **Spring Cloud Gateway** (API Gateway, reactive/Netty)
- **PostgreSQL 16** with Flyway migrations
- **Spring Security** + **JWT** (jjwt 0.12.5)
- **TOTP** for 2FA
- **Resilience4j** for circuit breaker + rate limiting
- **Stripe SDK** for payment processing

### Frontend
- **React 18** with **Vite**
- **TailwindCSS** for styling
- **Axios** with auto-refresh interceptor
- **React Router** for navigation

## Getting Started

1. Read [QUICK_START.md](QUICK_START.md) to run the application
2. Check [ARCHITECTURE.md](ARCHITECTURE.md) to understand the microservices design
3. See [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md) for code details
4. Use [TESTING_GUIDE.md](TESTING_GUIDE.md) to test features

## Pre-created Test Accounts

**Admin:**
- Email: `admin@cashtactics.com`
- Password: `password`

**User:**
- Email: `user@cashtactics.com`
- Password: `password`

## Project Structure

```
Online-Internet-Banking/
├── services/                   # Microservices
│   ├── api-gateway/            # API Gateway (port 8443)
│   ├── auth-service/           # Authentication (port 8081)
│   ├── client-service/         # Client management (port 8082)
│   ├── account-service/        # Account management (port 8083)
│   ├── transaction-service/    # Transaction history (port 8084)
│   └── payment-service/        # Stripe payments (port 8085)
├── frontend/                   # React frontend
│   ├── src/                    # React components & pages
│   └── services/               # API client services
├── explanations/               # This documentation
└── README.md                   # Main project readme
```
