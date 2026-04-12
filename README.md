# Online Internet Banking

Full-stack microservices banking project (Java/Spring Boot + React/Vite).

## Quick Start (Current Project Setup)

### Test accounts
- Admin: `admin@cashtactics.com` / `password`
- User: `user@cashtactics.com` / `password`

### Start everything with Docker Compose
From project root:

```bash
docker compose up --build
```

This starts:
- `postgres` (5432)
- `auth-service` (8081)
- `client-service` (8082)
- `account-service` (8083)
- `transaction-service` (8084)
- `payment-service` (8085)
- `api-gateway` (8443)
- `frontend` (5173)

### App URLs
- Frontend: `http://localhost:5173`
- API Gateway: `https://localhost:8443`

Stop stack:

```bash
docker compose down
```

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
└── postman_collection.json
```

## Notes About Gateway Protocol

- In the current Docker dev setup (`docker-compose.override.yml`), gateway runs on `https://localhost:8443`.
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
