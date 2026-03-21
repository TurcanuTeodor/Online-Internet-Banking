# Stripe card top-up (thesis flow)

## 1. Stripe Dashboard (Test mode)

1. Create / open a [Stripe account](https://dashboard.stripe.com) and stay in **Test mode**.
2. **Developers → API keys**: copy **Secret key** and **Publishable key**.
3. **Developers → Webhooks → Add endpoint**  
   - For local dev with Stripe CLI (recommended): see section 3 below — Stripe CLI gives you a **webhook signing secret** (`whsec_...`).
4. Install [Stripe CLI](https://stripe.com/docs/stripe-cli).

## 2. Backend `.env.properties` (per service)

Add to **payment-service**, **account-service**, and **transaction-service** (same value everywhere):

```properties
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...   # from Stripe CLI listen or Dashboard webhook
INTERNAL_API_SECRET=choose-a-long-random-string
```

`INTERNAL_API_SECRET` is used only for server-to-server calls (payment webhook → account credit → transaction deposit). It must be **identical** on all three services.

## 3. Webhook through the API Gateway

Forward events to your local gateway (HTTPS):

```bash
stripe listen --forward-to https://localhost:8443/api/payments/webhook
```

Use the CLI’s **`whsec_...`** as `STRIPE_WEBHOOK_SECRET` in **payment-service** `.env.properties`.

Start services in order: **PostgreSQL → microservices → api-gateway (8443)** → then run Stripe CLI.

## 4. Frontend

```bash
cd frontend
npm install
```

Copy `frontend/.env.example` to `frontend/.env.local` and set:

```properties
VITE_STRIPE_PUBLISHABLE_KEY=pk_test_...
```

Restart `npm run dev` after changing env vars.

## 5. User flow

1. Log in to the dashboard.
2. On an **ACTIVE** **EUR** or **RON** account, click **Top up**.
3. Enter amount (≥ 0.50), then pay with the **card form** (test card: `4242 4242 4242 4242`, any future expiry, any CVC).
4. After `payment_intent.succeeded`, the webhook credits the account and records a **DEPOSIT** in transaction-service.

## 6. API reference

| Method | Path | Notes |
|--------|------|--------|
| `POST` | `/api/payments/top-up/intent` | Body: `{ "accountId": number, "amount": number }`. Returns `clientSecret`, `paymentId`, `currencyCode`, etc. |
| `POST` | `/api/payments/webhook` | Called by Stripe only; no JWT. |

The gateway exposes **`/api/payments/webhook` without JWT** so Stripe CLI / Stripe servers can reach it.
