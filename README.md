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

### Start everything with Docker Compose (Thesis / Demo mode)
From project root:

```bash
docker compose -f docker-compose.yml up -d --build
```

This starts:
- `postgres` (5432)
- `auth-service` (8081)
- `client-service` (8082)
- `account-service` (8083)
- `transaction-service` (8084)
- `payment-service` (8085)
- `api-gateway` (8443)
- `frontend` (3000)

### App URLs
- Frontend: `http://localhost:3000`
- API Gateway: `https://localhost:8443`

Stop stack:

```bash
docker compose -f docker-compose.yml down
```

### Development mode (hot reload)
Use this when actively coding frontend/backend and you want live reload.

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build
```

Development URLs:
- Frontend (Vite): `http://localhost:5173`
- API Gateway: `https://localhost:8443`

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
├── explanations/
├── docker-compose.yml
├── docker-compose.override.yml
├── docker-compose.dev.yml
└── postman_collection.json
```

## Notes About Gateway Protocol

- In both thesis/demo and dev setup, gateway runs on `https://localhost:8443`.
- The gateway certificate is self-signed in local dev, so tools may require trusting the cert or skipping strict verification.

## Documentation

Detailed docs are in `explanations/`:
- `explanations/QUICK_START.md`
- `explanations/ARCHITECTURE.md`
- `explanations/IMPLEMENTATION_GUIDE.md`
- `explanations/TESTING_GUIDE.md`
- `explanations/DATABASE.md`

## Postman

Use `postman_collection.json` from project root and set:
- `baseUrl = https://localhost:8443`

## Thesis Demo Checklist (5 Minutes)

Use this sequence to demonstrate the implemented privacy and fraud flows clearly.

### 1. Start stack

```bash
docker compose -f docker-compose.yml up -d --build
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
