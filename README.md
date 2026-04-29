# Online Internet Banking

Full-stack microservices banking project (Java/Spring Boot + React/Vite).

## Quick Start (Current Project Setup)

### Secrets Setup (Required First Step)
1. Copy [`.env.example`](.env.example) to `.env` in project root and set your local values.
2. Copy [`services/fraud-service/.env.example.properties`](services/fraud-service/.env.example.properties) to `services/fraud-service/.env.properties` and set your local values.
3. Keep all real secrets only in local `.env*` files (already git-ignored).

### Test accounts
- Admin: `admin@cashtactics.com` / `password`
- User: `user@cashtactics.com` / `password`

### Start everything with Docker Compose

Dev:

```bash
docker compose --env-file .env -p online-internet-banking-dev -f deploy/docker-compose.yml -f deploy/docker-compose.override.yml up -d --build
```

Prod:

```bash
docker compose --env-file .env -p online-internet-banking-prod -f deploy/docker-compose.yml -f deploy/docker-compose.prod.yml up -d --build
```

### App URLs
- Dev Frontend: `http://localhost:5173`
- Dev API Gateway: `http://localhost:8080`
- Prod Frontend: `http://localhost:3000`
- Prod API Gateway: `https://localhost:8443`

### Redis in the project
- `account-service` uses Caffeine (L1) + Redis (L2) for cache.
- Redis Pub/Sub keeps local caches in sync across instances.
- `api-gateway` uses Redis for token blacklist and rate limiting.

Stop dev stack:

```bash
docker compose --env-file .env -p online-internet-banking-dev -f deploy/docker-compose.yml -f deploy/docker-compose.override.yml down
```

Stop prod stack:

```bash
docker compose --env-file .env -p online-internet-banking-prod -f deploy/docker-compose.yml -f deploy/docker-compose.prod.yml down
```

### Development mode (hot reload)
Use the dev command above when actively coding frontend/backend and you want live reload.

## Project Structure

```text
Online-Internet-Banking/
├── services/
│   ├── api-gateway/
│   ├── auth-service/
│   ├── client-service/
│   ├── account-service/
│   ├── transaction-service/
│   └── payment-service/
├── frontend/
├── deploy/ (docker compose overlays)
├── docs/ (project documentation and UML)
├── data/
└── postman collection: docs/postman/postman_collection.json
```

## Notes About Gateway Protocol

- Dev gateway runs on `http://localhost:8080`.
- Prod gateway runs on `https://localhost:8443`.
- The gateway certificate is self-signed in local dev, so tools may require trusting the cert or skipping strict verification.

## Documentation

Detailed docs are in `docs/`:
- `docs/QUICK_START.md`
- `docs/ARCHITECTURE.md`
- `docs/IMPLEMENTATION_GUIDE.md`
- `docs/TESTING_GUIDE.md`
- `docs/DATABASE.md`
- `docs/REDIS_CACHING.md`

## Postman

Use `docs/postman/postman_collection.json` and set:
- `baseUrl = https://localhost:8443`

## Thesis Demo Checklist (5 Minutes)

Use this sequence to demonstrate the implemented privacy and fraud flows clearly.

### 1. Start stack

```bash
docker compose --env-file .env -p online-internet-banking-prod -f deploy/docker-compose.yml -f deploy/docker-compose.prod.yml up -d --build
```

Open:
- Frontend: `http://localhost:3000`
- Gateway: `https://localhost:8443`

### 2. Admin privacy control (masked by default)

1. Login as admin (`admin@cashtactics.com` / `password`).
2. Go to Accounts or Transactions in Admin Dashboard.
3. Confirm financial values are masked.
4. Click Reveal data.
5. In modal, select reason category (and details for `OTHER`).
6. Confirm reveal.
7. Verify values become visible only after audited confirmation.

### 3. Admin audit report (reasonCode filter)

1. Open Admin Dashboard -> Audit Logs.
2. Filter by a specific reason code (e.g. `FRAUD_REVIEW`).
3. Verify table shows actor, scope, target, reason code, details, timestamp.

### 4. User fraud/security flow

1. Login as user (`user@cashtactics.com` / `password`).
2. Open Dashboard -> Security.
3. Verify unresolved alerts are shown and banner appears when pending alerts exist.
4. Resolve one alert as `LEGITIMATE` and one as `FRAUD_REPORTED` (if available).
5. Verify status and resolution notes update in Security Center.

### 5. Optional technical validation

```bash
docker compose logs -f account-service fraud-service api-gateway frontend
```
