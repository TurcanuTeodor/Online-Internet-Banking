# Thesis Chapter Checklist and Mapping — Online-Internet-Banking

This document maps the university thesis template headings (SablonLicenta2026.docx) to the project workspace and lists what to include, expand on, and not forget when writing each chapter. Use this as a working checklist while authoring the thesis in English.

---

## 1. Introduction
- Purpose: concise presentation of the system goals and motivation (why an online banking platform, why fraud detection tiers, why microservices).
- Scope: clarify what's in/out (features implemented, services included: auth, client, account, transaction, payment, fraud, API gateway, frontend).
- Contributions: list the novel or primary contributions of your implementation (e.g., tiered fraud pipeline with pseudonymization, schema-per-service DB zoning, Spring Boot microservices + React SPA, secure top-up via Stripe integration).
- Where to extract details from: [docs/README.md](docs/README.md), [README.md](README.md), `frontend/`, `services/` modules.
- Do not forget: short overview figure (system context), high-level metrics if available (requests/sec, latency targets), one-paragraph limitations and ethical/privacy note.

## 2. Problem Statement and Related Work
- Define the problem: online banking risks (fraud), privacy/PII concerns, need for modular scalable architecture.
- Related work: brief survey of approaches to fraud detection (rule-based, behavioral scoring, ML/LLM-assisted reasoning), cite standard papers and APIs used (Stripe, ECB, LLM providers).
- Where to extract details from: `docs/ARCHITECTURE.md`, `docs/SECURITY_KEYS_AND_ALGORITHMS.md`, `docs/TESTING_GUIDE.md` for evaluation methods.
- Do not forget: explicit research questions or hypotheses, assumptions, and constraints (e.g., single-instance PostgreSQL with schema-per-service, no cross-schema JOINs).

## 3. Technologies and Methods
- List and justify the main technologies: Java + Spring Boot (microservices), React + Vite frontend, PostgreSQL (schema-per-service), Redis (caching), Docker Compose for deployment, Stripe for payments, LLM providers for Tier 3 reasoning.
- Design patterns, security methods and cryptography: password hashing (BCrypt), AES-256-GCM for PII, pseudonymization (SHA-256 IBAN hashing), JWT auth, HMAC/REST signatures.
- Where to extract details from: `services/*/pom.xml`, `frontend/package.json`, `docs/SECURITY_KEYS_AND_ALGORITHMS.md`, `deploy/docker-compose*.yml`.
- Do not forget: reasons for choices (trade-offs), alternative technologies considered, licensing or external API constraints (Stripe terms, LLM usage).

## 4. System Architecture
- Provide architecture overview and rationale: context diagram, container diagram, data zoning model, inter-service communication map, API gateway lifecycle.
- Diagrams to include and update: files in `docs/architecture/` (chapter4 and chapter5 diagrams) and the PlantUML sources under `docs/architecture/` and any exported images in `out/`.
  - Ensure diagrams reflect current state: services, ports, schema names, and new endpoints you added.
- Explain zones: centralized (auth), shared (payments/accounts/transactions), private (clients/fraud), and their access restrictions.
- Where to extract details from: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), PlantUML files in `docs/architecture/` and images in `out/`.
- Do not forget: document assumptions (single PostgreSQL instance, no FK cross-schema), API security headers (`X-Internal-Api-Secret` usage), and scaling considerations.

## 5. Implementation
- Break down by component/service with short subsections per microservice:
  - `auth-service`: JWT flow, step-up TOTP, password re-encryption on change.
  - `client-service`: client profile, onboarding flows, GDPR operations.
  - `account-service`: account lifecycle, balances, currency conversion.
  - `transaction-service`: tx records, debit/credit flows, idempotency and signing.
  - `payment-service`: Stripe integration, webhooks, top-up sequence.
  - `fraud-service`: tiered fraud pipeline (Tier 1 rules, Tier 2 behavioral scoring, Tier 3 LLM reasoning), pseudonymization steps, persistence of decisions.
  - API Gateway: filters (CORS, rate-limit, JWT validation), routing, circuit breaker/fallback.
- For each service include:
  - key classes or modules (point to package paths), important endpoints (HTTP routes), main DB schemas/tables, and config (application.yml / properties).
  - testing approach and important tests (unit/integration), how to run them.
- Where to extract details from: `services/*/src/main/java/ro/app/*`, `services/*/src/main/resources`, `docs/IMPLEMENTATION_GUIDE.md`, `docs/TESTING_GUIDE.md`.
- Do not forget: provide code snippets for critical flows (transfer flow, fraud evaluation request, transaction persistence), and mention any divergences from the original design in the template diagrams.

## 6. Evaluation / Testing / Results
- Describe testing strategy: unit tests, integration tests, manual scenarios (transfer, top-up, fraud cases), performance checks if any.
- Relevant files: `docs/TESTING_GUIDE.md`, any test folders under `services/*/src/test`, and manual test scripts or notes in `frontend/` or `docs/QUICK_START.md`.
- Metrics: include measured outcomes where available (false positives/negatives for fraud rules, response times, throughput). If not measured, describe a plan and expected evaluation approach.
- Do not forget: show sample requests/responses for key flows, explain how pseudonymization preserves privacy while allowing correlation, and include limitations of your evaluation.

## 7. Conclusions
- Summarize achievements, lessons learned, limitations, and suggestions for future work (improved ML models, distributed databases, production-grade observability, CI/CD pipeline).
- Where to reference implementations: point back to each service section and architecture justification.

## Bibliography
- Provide citation formatting guidance (APA/IEEE as required by your university).
- Include references for external APIs, libraries, and academic papers used.

## Appendices
- Appendix A — List of figures: include all diagrams (link to files in `docs/architecture/` and exported images in `out/`).
- Appendix B — List of tables: any important DB schema or config tables.
- Appendix C — Acronyms: list JWT, SPA, API, LLM, PII, TOTP, ECB, etc.
- Appendix D — Code snippets and important configs: `docker-compose.yml`, relevant `application.yml` fragments, DB schema SQL snippets.

---

## Recommended next steps (authoring workflow)
1. Update the diagrams in `docs/architecture/` and re-export PNG/SVG to `out/` to match your current code.
2. Walk each service folder and add short descriptions and key-file links into the `Implementation` chapter.
3. Extract or add example API payloads and cURL snippets for primary flows (transfer, fraud evaluate, top-up).
4. Draft evaluation scenarios and run basic measurements or record representative logs.
5. Assemble the Markdown into the university template (SablonLicenta) or paste into its corresponding sections.

If you want, I can now:
- scan the repo for likely file-to-chapter mappings (endpoints, classes) and produce a more detailed per-section bullet list with exact file links, or
- start a first-pass English Markdown draft of the full chapters using the content already present.
