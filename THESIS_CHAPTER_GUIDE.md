# Thesis Chapter Guide for Online-Internet-Banking

This file is a thesis-writing companion for the current state of the repository. It is intentionally conservative: it points only to code, docs, and diagrams that are actually present in the workspace and flags places where the documentation is likely stale.

Use this as the base for the English thesis text. For each chapter, the guide tells you:
- what to introduce,
- what to expand on,
- what not to forget,
- which files to cite,
- which parts of the existing docs are likely outdated.

## Ground Rules

- Treat the codebase as the source of truth when docs disagree.
- Prefer the repo docs for narrative, but verify architecture and API details against the service code and gateway config.
- The system currently includes the API Gateway plus six backend services: auth, client, account, transaction, payment, and fraud.
- The live frontend stores tokens in `sessionStorage`, not `localStorage`.
- Gateway routing now includes public client signup, GDPR routes, Stripe webhook handling, and fraud routes.

---

## Chapter 1. Introduction

### What to introduce
- The project is a full-stack online banking platform built as a microservices system.
- The application combines banking operations, authentication, external payment processing, and fraud detection.
- The thesis goal is to show how modular architecture, security controls, and privacy-preserving design can coexist in a financial app.

### What to expand on
- Why online banking is a good case study for distributed systems, security, and privacy.
- Why the project was split into microservices instead of a monolith.
- Why the system includes both operational banking functions and fraud detection.
- Why the app uses a dedicated API Gateway as a single entry point.

### Do not forget
- State the main capabilities: login, 2FA, client management, account management, transfers, Stripe top-ups, transaction history, admin views, Redis-backed protection, and tiered fraud analysis.
- Mention that the system supports role-based behavior for ADMIN and USER.
- Mention that the thesis is about an implemented system, not a theoretical design.

### Best repo evidence
- [README.md](README.md)
- [docs/README.md](docs/README.md)
- [docs/QUICK_START.md](docs/QUICK_START.md)
- [THESIS_OUTLINE.md](THESIS_OUTLINE.md)

### Good thesis sentences to build from
- “This project implements an online internet banking platform using a microservices architecture, with security, privacy, and fraud detection treated as first-class requirements.”
- “The system separates responsibilities across authentication, client data management, account operations, transactions, external payments, and fraud analysis.”

---

## Chapter 2. Problem Statement and Related Work

### What to introduce
- The practical problem: secure digital banking requires authentication, fine-grained access control, reliable transaction processing, and fraud monitoring.
- The privacy problem: banking systems handle sensitive personal and financial data, so internal access and data exposure must be minimized.
- The architecture problem: a single application must coordinate internal services and external providers without collapsing into a monolith.

### What to expand on
- Compare rule-based fraud detection, behavioral scoring, and model-assisted / LLM-assisted reasoning.
- Explain why a tiered fraud pipeline is useful: fast deterministic checks first, then heavier or more ambiguous analysis later.
- Explain why schema-per-service helps with isolation and future maintainability.
- Discuss why external payment providers such as Stripe are often separated from the core ledger.

### Do not forget
- Keep this chapter grounded in the actual project: do not overclaim academic novelty.
- Use the fraud documents to explain the rationale for the tiered pipeline and the refactor to PaySim-based training.
- Mention privacy constraints and internal data access controls.

### Best repo evidence
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- [docs/SECURITY_KEYS_AND_ALGORITHMS.md](docs/SECURITY_KEYS_AND_ALGORITHMS.md)
- [docs/REDIS_CACHING.md](docs/REDIS_CACHING.md)
- [docs/fraud-tier3-paysim-refactor.md](docs/fraud-tier3-paysim-refactor.md)
- [docs/STRIPE_TOP_UP_SEQUENCE.md](docs/STRIPE_TOP_UP_SEQUENCE.md)

### Staleness note
- The architecture docs focus on the banking core and may understate the fraud service in the live codebase.

---

## Chapter 3. Technologies and Methods

### What to introduce
- Backend: Java 21, Spring Boot 3.3, Spring Security, Spring Cloud Gateway, Spring Data/JPA, Flyway, Resilience4j, Stripe SDK, JJWT.
- Frontend: React 18, Vite, TailwindCSS, Axios, React Router.
- Data layer: PostgreSQL with schema-per-service organization.
- Infrastructure: Docker Compose, Redis, Caffeine, Postman collection.

### What to expand on
- Explain why Spring Boot is used for service-level development.
- Explain why Gateway is reactive and why it acts as the system entry point.
- Explain why Redis is used both for performance and for cross-instance consistency.
- Explain why cryptographic choices matter: BCrypt for passwords, HMAC-signed JWT, TOTP for 2FA, TLS for transport.
- Explain the difference between access tokens and refresh tokens.
- Explain why the client data encryption lifecycle is centralized in client-service.

### Do not forget
- Mention the build and deployment approach through Docker Compose overlays.
- Mention the use of environment files for secrets.
- Mention that internal service-to-service calls are authenticated separately from user JWT flow.

### Best repo evidence
- [services/api-gateway/pom.xml](services/api-gateway/pom.xml)
- [services/auth-service/pom.xml](services/auth-service/pom.xml)
- [services/fraud-service/pom.xml](services/fraud-service/pom.xml)
- [services/payment-service/pom.xml](services/payment-service/pom.xml)
- [frontend/package.json](frontend/package.json)
- [deploy/docker-compose.yml](deploy/docker-compose.yml)
- [deploy/docker-compose.override.yml](deploy/docker-compose.override.yml)
- [deploy/docker-compose.prod.yml](deploy/docker-compose.prod.yml)
- [docs/DATABASE.md](docs/DATABASE.md)
- [docs/SECURITY_KEYS_AND_ALGORITHMS.md](docs/SECURITY_KEYS_AND_ALGORITHMS.md)

### Staleness note
- [docs/DATABASE.md](docs/DATABASE.md) still says PostgreSQL 17, while the compose stack uses PostgreSQL 16.
- [docs/SECURITY_KEYS_AND_ALGORITHMS.md](docs/SECURITY_KEYS_AND_ALGORITHMS.md) still mentions `VITE_API_URL`, but the frontend uses `VITE_API_BASE_URL`.

---

## Chapter 4. System Architecture

### What to introduce
- The application is organized around a React frontend, a Spring Cloud Gateway, and six backend services.
- Each service owns a separate PostgreSQL schema.
- The gateway applies security and routing before requests reach the backend.
- The fraud service is a first-class service, not a side utility.

### What to expand on
- Explain the context diagram: actors, external systems, and the main platform boundary.
- Explain the container diagram: frontend, gateway, services, and database.
- Explain the data zoning model: centralized identity/authentication, shared banking data, and private sensitive data.
- Explain inter-service communication and why some calls use forwarded user JWT while others use a shared internal secret.
- Explain the API Gateway request lifecycle: CORS, rate limiting, JWT validation, routing, circuit breaker, fallback.
- Explain transfer flow and why transaction records are created after balance updates.
- Explain the async fraud analysis branch and why it is separated from the synchronous transfer path.

### Do not forget
- Update diagrams if the text and code diverge.
- Include the fraud service and its tiers in the architecture narrative.
- Mention public gateway exceptions: auth routes, client signup, webhook, fraud health.
- Mention that the frontend currently stores tokens in session storage.

### Best repo evidence
- [docs/architecture/chapter4/system-context.puml](docs/architecture/chapter4/system-context.puml)
- [docs/architecture/chapter4/containter.puml](docs/architecture/chapter4/containter.puml)
- [docs/architecture/chapter4/data-zoning.puml](docs/architecture/chapter4/data-zoning.puml)
- [docs/architecture/chapter4/inter-service-communication.puml](docs/architecture/chapter4/inter-service-communication.puml)
- [docs/architecture/chapter4/api-gateway-request-lifecycle.puml](docs/architecture/chapter4/api-gateway-request-lifecycle.puml)
- [docs/architecture/chapter4/transfer-flow.puml](docs/architecture/chapter4/transfer-flow.puml)
- [docs/architecture/chapter5/transfer-flow-async-fraud.puml](docs/architecture/chapter5/transfer-flow-async-fraud.puml)
- [services/api-gateway/src/main/java/ro/app/gateway/config/GatewayConfig.java](services/api-gateway/src/main/java/ro/app/gateway/config/GatewayConfig.java)
- [services/api-gateway/src/main/java/ro/app/gateway/filter/JwtAuthFilter.java](services/api-gateway/src/main/java/ro/app/gateway/filter/JwtAuthFilter.java)
- [services/api-gateway/src/main/java/ro/app/gateway/filter/RateLimitFilter.java](services/api-gateway/src/main/java/ro/app/gateway/filter/RateLimitFilter.java)
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- [docs/STRIPE_TOP_UP_SEQUENCE.md](docs/STRIPE_TOP_UP_SEQUENCE.md)

### Staleness note
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) still describes a smaller service set than the current code.
- The live gateway routing includes `/api/clients/sign-up`, `/api/gdpr/**`, `/api/payments/webhook`, and `/api/fraud/**`.

### Architecture summary you can reuse
- Frontend uses the gateway as the only public backend entry point.
- Gateway validates and routes requests, applies rate limiting, and provides circuit breaker fallbacks.
- Services are isolated by concern and schema.
- Sensitive data is partitioned so administrative and internal workflows do not expose everything by default.

---

## Chapter 5. Implementation

### What to introduce
- Organize this chapter by subsystem or service, not by random file order.
- For each service, explain its responsibility, public endpoints, internal calls, and persistence.

### Service-by-service structure

#### auth-service
- Responsibilities: registration, login, refresh tokens, logout, 2FA setup and verification.
- Cite:
  - [services/auth-service/src/main/java/ro/app/auth/AuthServiceApplication.java](services/auth-service/src/main/java/ro/app/auth/AuthServiceApplication.java)
  - [services/auth-service/src/main/java/ro/app/auth/controller/AuthController.java](services/auth-service/src/main/java/ro/app/auth/controller/AuthController.java)
  - [services/auth-service/src/main/java/ro/app/auth/controller/InternalStepUpAuthController.java](services/auth-service/src/main/java/ro/app/auth/controller/InternalStepUpAuthController.java)
  - [services/auth-service/src/main/java/ro/app/auth/service/AuthService.java](services/auth-service/src/main/java/ro/app/auth/service/AuthService.java)
  - [services/auth-service/src/main/java/ro/app/auth/service/RefreshTokenService.java](services/auth-service/src/main/java/ro/app/auth/service/RefreshTokenService.java)
  - [services/auth-service/src/main/java/ro/app/auth/service/TwoFaService.java](services/auth-service/src/main/java/ro/app/auth/service/TwoFaService.java)
  - [services/auth-service/src/main/java/ro/app/auth/security/jwt/JwtService.java](services/auth-service/src/main/java/ro/app/auth/security/jwt/JwtService.java)
- Expand on: temp token / step-up flow, refresh token rotation, JWT claims, 2FA secret generation.

#### client-service
- Responsibilities: client profile lifecycle, GDPR operations, encryption/re-encryption lifecycle, contact data management.
- Cite:
  - [services/client-service/src/main/java/ro/app/client/ClientServiceApplication.java](services/client-service/src/main/java/ro/app/client/ClientServiceApplication.java)
  - [services/client-service/src/main/java/ro/app/client/controller/ClientController.java](services/client-service/src/main/java/ro/app/client/controller/ClientController.java)
  - [services/client-service/src/main/java/ro/app/client/controller/GdprController.java](services/client-service/src/main/java/ro/app/client/controller/GdprController.java)
  - [services/client-service/src/main/java/ro/app/client/controller/InternalClientController.java](services/client-service/src/main/java/ro/app/client/controller/InternalClientController.java)
  - [services/client-service/src/main/java/ro/app/client/service/ClientProfileService.java](services/client-service/src/main/java/ro/app/client/service/ClientProfileService.java)
  - [services/client-service/src/main/java/ro/app/client/service/ClientEncryptionLifecycleService.java](services/client-service/src/main/java/ro/app/client/service/ClientEncryptionLifecycleService.java)
  - [services/client-service/src/main/java/ro/app/client/service/ClientGdprService.java](services/client-service/src/main/java/ro/app/client/service/ClientGdprService.java)
- Expand on: data masking, reveal-auditing, encryption key rotation, privacy controls.

#### account-service
- Responsibilities: accounts, balances, transfers, exchange rates, balance audit, Stripe top-up application.
- Cite:
  - [services/account-service/src/main/java/ro/app/account/AccountServiceApplication.java](services/account-service/src/main/java/ro/app/account/AccountServiceApplication.java)
  - [services/account-service/src/main/java/ro/app/account/controller/AccountController.java](services/account-service/src/main/java/ro/app/account/controller/AccountController.java)
  - [services/account-service/src/main/java/ro/app/account/controller/AdminAuditController.java](services/account-service/src/main/java/ro/app/account/controller/AdminAuditController.java)
  - [services/account-service/src/main/java/ro/app/account/controller/InternalStripeTopUpController.java](services/account-service/src/main/java/ro/app/account/controller/InternalStripeTopUpController.java)
  - [services/account-service/src/main/java/ro/app/account/service/AccountTransferService.java](services/account-service/src/main/java/ro/app/account/service/AccountTransferService.java)
  - [services/account-service/src/main/java/ro/app/account/service/AccountTopUpService.java](services/account-service/src/main/java/ro/app/account/service/AccountTopUpService.java)
  - [services/account-service/src/main/java/ro/app/account/config/redis/CacheInvalidationPublisher.java](services/account-service/src/main/java/ro/app/account/config/redis/CacheInvalidationPublisher.java)
- Expand on: transaction-safe transfer flow, lock usage, Redis cache invalidation, balance updates, audit reveal flow.

#### transaction-service
- Responsibilities: transaction records, queries, totals, flagged transactions.
- Cite:
  - [services/transaction-service/src/main/java/ro/app/transaction/TransactionServiceApplication.java](services/transaction-service/src/main/java/ro/app/transaction/TransactionServiceApplication.java)
  - [services/transaction-service/src/main/java/ro/app/transaction/controller/TransactionController.java](services/transaction-service/src/main/java/ro/app/transaction/controller/TransactionController.java)
  - [services/transaction-service/src/main/java/ro/app/transaction/service/TransactionOrchestrationService.java](services/transaction-service/src/main/java/ro/app/transaction/service/TransactionOrchestrationService.java)
  - [services/transaction-service/src/main/java/ro/app/transaction/service/TransactionQueryService.java](services/transaction-service/src/main/java/ro/app/transaction/service/TransactionQueryService.java)
- Expand on: debit/credit record creation and how transaction history is queried.

#### payment-service
- Responsibilities: Stripe payment intents, payment methods, webhooks, settlement.
- Cite:
  - [services/payment-service/src/main/java/ro/app/payment/PaymentServiceApplication.java](services/payment-service/src/main/java/ro/app/payment/PaymentServiceApplication.java)
  - [services/payment-service/src/main/java/ro/app/payment/controller/PaymentController.java](services/payment-service/src/main/java/ro/app/payment/controller/PaymentController.java)
  - [services/payment-service/src/main/java/ro/app/payment/controller/PaymentMethodController.java](services/payment-service/src/main/java/ro/app/payment/controller/PaymentMethodController.java)
  - [services/payment-service/src/main/java/ro/app/payment/controller/WebhookController.java](services/payment-service/src/main/java/ro/app/payment/controller/WebhookController.java)
  - [services/payment-service/src/main/java/ro/app/payment/service/payment/creation/PaymentCreationService.java](services/payment-service/src/main/java/ro/app/payment/service/payment/creation/PaymentCreationService.java)
  - [services/payment-service/src/main/java/ro/app/payment/service/payment/settlement/PaymentSettlementService.java](services/payment-service/src/main/java/ro/app/payment/service/payment/settlement/PaymentSettlementService.java)
  - [services/payment-service/src/main/java/ro/app/payment/service/stripe/StripeCustomerService.java](services/payment-service/src/main/java/ro/app/payment/service/stripe/StripeCustomerService.java)
- Expand on: PaymentIntent lifecycle, webhook idempotency, Stripe top-up sequence, separation of card handling from ledger data.

#### fraud-service
- Responsibilities: fraud detection pipeline, alerts, decisions, tiered scoring.
- Cite:
  - [services/fraud-service/src/main/java/ro/app/fraud/FraudServiceApplication.java](services/fraud-service/src/main/java/ro/app/fraud/FraudServiceApplication.java)
  - [services/fraud-service/src/main/java/ro/app/fraud/controller/FraudController.java](services/fraud-service/src/main/java/ro/app/fraud/controller/FraudController.java)
  - [services/fraud-service/src/main/java/ro/app/fraud/controller/InternalFraudController.java](services/fraud-service/src/main/java/ro/app/fraud/controller/InternalFraudController.java)
  - [services/fraud-service/src/main/java/ro/app/fraud/service/FraudService.java](services/fraud-service/src/main/java/ro/app/fraud/service/FraudService.java)
  - [services/fraud-service/src/main/java/ro/app/fraud/tier1/RuleEngine.java](services/fraud-service/src/main/java/ro/app/fraud/tier1/RuleEngine.java)
  - [services/fraud-service/src/main/java/ro/app/fraud/tier2/BehavioralScoringService.java](services/fraud-service/src/main/java/ro/app/fraud/tier2/BehavioralScoringService.java)
  - [services/fraud-service/src/main/java/ro/app/fraud/tier3/Tier3MlService.java](services/fraud-service/src/main/java/ro/app/fraud/tier3/Tier3MlService.java)
  - [services/fraud-service/src/main/java/ro/app/fraud/tier3/ModelTrainerCli.java](services/fraud-service/src/main/java/ro/app/fraud/tier3/ModelTrainerCli.java)
- Expand on: synchronous rule checks, behavioral scoring, offline-trained ML model, pseudonymization, async fraud branch, alert resolution.

#### api-gateway
- Responsibilities: route management, JWT validation, rate limiting, token blacklist, CORS, circuit breakers.
- Cite:
  - [services/api-gateway/src/main/java/ro/app/gateway/ApiGatewayApplication.java](services/api-gateway/src/main/java/ro/app/gateway/ApiGatewayApplication.java)
  - [services/api-gateway/src/main/java/ro/app/gateway/config/GatewayConfig.java](services/api-gateway/src/main/java/ro/app/gateway/config/GatewayConfig.java)
  - [services/api-gateway/src/main/java/ro/app/gateway/filter/JwtAuthFilter.java](services/api-gateway/src/main/java/ro/app/gateway/filter/JwtAuthFilter.java)
  - [services/api-gateway/src/main/java/ro/app/gateway/filter/RateLimitFilter.java](services/api-gateway/src/main/java/ro/app/gateway/filter/RateLimitFilter.java)
  - [services/api-gateway/src/main/java/ro/app/gateway/service/TokenBlacklistService.java](services/api-gateway/src/main/java/ro/app/gateway/service/TokenBlacklistService.java)
  - [services/api-gateway/src/main/java/ro/app/gateway/service/RedisRateLimitService.java](services/api-gateway/src/main/java/ro/app/gateway/service/RedisRateLimitService.java)
- Expand on: public vs protected routes, fallback behavior, internal route ordering, why webhook and health routes bypass JWT.

### Frontend subsection
- Cite:
  - [frontend/src/App.jsx](frontend/src/App.jsx)
  - [frontend/src/pages/Login.jsx](frontend/src/pages/Login.jsx)
  - [frontend/src/pages/Register.jsx](frontend/src/pages/Register.jsx)
  - [frontend/src/pages/TwoFactorVerify.jsx](frontend/src/pages/TwoFactorVerify.jsx)
  - [frontend/src/pages/Dashboard.jsx](frontend/src/pages/Dashboard.jsx)
  - [frontend/src/pages/AdminDashboard/index.jsx](frontend/src/pages/AdminDashboard/index.jsx)
  - [frontend/src/services/apiClient.js](frontend/src/services/apiClient.js)
  - [frontend/src/services/authService.js](frontend/src/services/authService.js)
  - [frontend/src/services/fraudService.js](frontend/src/services/fraudService.js)
  - [frontend/src/services/accountService.js](frontend/src/services/accountService.js)
  - [frontend/src/services/paymentService.js](frontend/src/services/paymentService.js)
  - [frontend/src/services/transactionService.js](frontend/src/services/transactionService.js)
- Expand on: role-based navigation, auto-refresh token flow, dashboard tabs, admin vs user view.

### Do not forget
- Mention the exact gateway route exceptions visible in the code.
- Mention that the app uses `sessionStorage` for JWT and refresh token persistence in the current frontend implementation.
- Mention the fraud service as a separate chapter-worthy implementation topic.

---

## Chapter 6. Testing and Evaluation

### What to introduce
- Explain how the system is tested through the gateway and directly in the UI.
- Show that the project includes scripted API tests, manual flows, integration tests, and service-specific validation.

### What to expand on
- Authentication tests: login, registration, refresh, logout, 2FA.
- Banking flow tests: account view, transfers, transaction history.
- Gateway tests: JWT enforcement, rate limiting, circuit breaker fallback.
- Admin tests: client and transaction visibility, reveal auditing.
- Fraud tests: alert retrieval, resolution, and scoring behavior.
- Payment tests: Stripe top-up and webhook settlement.

### Do not forget
- State the expected response codes and why they matter.
- If you have not measured performance or model quality, say so explicitly and keep evaluation qualitative rather than inventing metrics.
- If you do include metrics later, cite where they were obtained and how.

### Best repo evidence
- [docs/TESTING_GUIDE.md](docs/TESTING_GUIDE.md)
- [services/payment-service/TESTING_GUIDE.md](services/payment-service/TESTING_GUIDE.md)
- [docs/postman/postman_collection.json](docs/postman/postman_collection.json)
- [docs/QUICK_START.md](docs/QUICK_START.md)
- [services/account-service/src/test/java/ro/app/account/integration/AccountControllerIT.java](services/account-service/src/test/java/ro/app/account/integration/AccountControllerIT.java)
- [services/transaction-service/src/test/java/ro/app/transaction/integration/TransactionControllerIT.java](services/transaction-service/src/test/java/ro/app/transaction/integration/TransactionControllerIT.java)
- [services/payment-service/src/test/java/ro/app/payment/integration/PaymentControllerIT.java](services/payment-service/src/test/java/ro/app/payment/integration/PaymentControllerIT.java)
- [services/client-service/src/test/java/ro/app/client/integration/ClientControllerIT.java](services/client-service/src/test/java/ro/app/client/integration/ClientControllerIT.java)
- [services/fraud-service/src/test/java/ro/app/fraud/tier3/PaySimFeatureMapperTest.java](services/fraud-service/src/test/java/ro/app/fraud/tier3/PaySimFeatureMapperTest.java)
- [services/api-gateway/src/test/java/ro/app/gateway/service/RedisRateLimitServiceTest.java](services/api-gateway/src/test/java/ro/app/gateway/service/RedisRateLimitServiceTest.java)

### Thesis-friendly evaluation framing
- “The system was validated through gateway-mediated API scenarios, service integration tests, and manual end-to-end flows covering authentication, account operations, transfers, payments, and fraud handling.”

---

## Chapter 7. Conclusions

### What to introduce
- Summarize the architecture, implementation, and the practical lessons learned.

### What to expand on
- State what worked well: service isolation, gateway security, Redis-assisted protection, privacy controls, tiered fraud logic.
- State what was hard: keeping diagrams, docs, and code aligned; handling secure internal calls; balancing usability and security.
- Mention realistic future work: observability, better fraud model evaluation, distributed deployment, stronger production hardening, CI/CD.

### Do not forget
- Keep conclusions specific to what was actually built.
- Briefly mention limitations rather than pretending the system is production complete.

### Best repo evidence
- [README.md](README.md)
- [docs/IMPLEMENTATION_GUIDE.md](docs/IMPLEMENTATION_GUIDE.md)
- [docs/TESTING_GUIDE.md](docs/TESTING_GUIDE.md)
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

---

## Bibliography

### What to include
- External libraries and frameworks used by the system.
- Vendor documentation for Stripe, Spring, React, PostgreSQL, Redis, Flyway, Resilience4j, JJWT, and any ML library used by fraud-service.
- Academic or technical sources on fraud detection, behavioral analysis, and secure banking systems.

### Best repo evidence
- [services/api-gateway/pom.xml](services/api-gateway/pom.xml)
- [services/auth-service/pom.xml](services/auth-service/pom.xml)
- [services/fraud-service/pom.xml](services/fraud-service/pom.xml)
- [frontend/package.json](frontend/package.json)
- [docs/SECURITY_KEYS_AND_ALGORITHMS.md](docs/SECURITY_KEYS_AND_ALGORITHMS.md)
- [docs/STRIPE_TOP_UP_SEQUENCE.md](docs/STRIPE_TOP_UP_SEQUENCE.md)

### Do not forget
- The repository does not contain a finished bibliography section.
- Build the bibliography directly from the dependencies and the external systems documented in the code and docs.

---

## Appendices

### Appendix 1. Figures
- [docs/architecture/chapter4/system-context.puml](docs/architecture/chapter4/system-context.puml)
- [docs/architecture/chapter4/containter.puml](docs/architecture/chapter4/containter.puml)
- [docs/architecture/chapter4/data-zoning.puml](docs/architecture/chapter4/data-zoning.puml)
- [docs/architecture/chapter4/inter-service-communication.puml](docs/architecture/chapter4/inter-service-communication.puml)
- [docs/architecture/chapter4/api-gateway-request-lifecycle.puml](docs/architecture/chapter4/api-gateway-request-lifecycle.puml)
- [docs/architecture/chapter4/transfer-flow.puml](docs/architecture/chapter4/transfer-flow.puml)
- [docs/architecture/chapter5/transfer-flow-async-fraud.puml](docs/architecture/chapter5/transfer-flow-async-fraud.puml)

### Appendix 2. Tables
- [docs/DATABASE.md](docs/DATABASE.md)
- [docs/REDIS_CACHING.md](docs/REDIS_CACHING.md)
- [docs/SECURITY_KEYS_AND_ALGORITHMS.md](docs/SECURITY_KEYS_AND_ALGORITHMS.md)

### Appendix 3. Acronyms
- JWT
- 2FA
- TOTP
- SPA
- API
- PII
- ECB
- LLM
- CORS
- TLS
- PCI

### Appendix 4. Useful request examples
- [docs/postman/postman_collection.json](docs/postman/postman_collection.json)
- [docs/TESTING_GUIDE.md](docs/TESTING_GUIDE.md)
- [services/payment-service/TESTING_GUIDE.md](services/payment-service/TESTING_GUIDE.md)

---

## Small editing checklist before you write the final thesis

1. Refresh the diagrams so they match the code and gateway routes.
2. Recheck the docs that still mention the older smaller service set.
3. Decide whether your final thesis names the product as Online Internet Banking or CashTactics, then keep that naming consistent.
4. Keep the architecture chapter focused on what is truly implemented, not on intended future features.
5. Use the source files above as citations whenever you describe behavior.

---

## Most important stale-doc warnings

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) is behind the current code on service count and route coverage.
- [docs/README.md](docs/README.md) also still reflects the older smaller service set.
- [docs/DATABASE.md](docs/DATABASE.md) says PostgreSQL 17, but the compose stack uses PostgreSQL 16.
- [docs/SECURITY_KEYS_AND_ALGORITHMS.md](docs/SECURITY_KEYS_AND_ALGORITHMS.md) still uses the older frontend env var name.
