# Payment Service — Ghid de Testare

## Cerințe prealabile

1. **PostgreSQL** pornit (localhost:5432, database `banking`)
2. **Stripe CLI** instalat (`stripe login` făcut cel puțin o dată)
3. **auth-service** pornit (port 8081) — necesar pentru JWT token
4. **payment-service** pornit (port 8085)

---

## Pasul 0 — Pornire servicii

Deschide **3 terminale** separate:

### Terminal 1 — auth-service
```bash
cd auth-service
.\mvnw.cmd spring-boot:run
```
Așteaptă mesajul: `Started AuthServiceApplication ... (port 8081)`

### Terminal 2 — payment-service
```bash
cd payment-service
.\mvnw.cmd spring-boot:run
```
Așteaptă mesajul: `Started PaymentServiceApplication ... (port 8085)`

### Terminal 3 — Stripe webhook tunnel
```bash
stripe listen --forward-to localhost:8085/api/payments/webhook
```
Va afișa: `Ready! Your webhook signing secret is whsec_...`

> **Important:** Webhook secret-ul din `stripe listen` trebuie să fie **același** cu cel din `.env.properties` (`STRIPE_WEBHOOK_SECRET`). Dacă diferă, copiază-l din terminal în `.env.properties` și repornește payment-service.

---

## Pasul 1 — Obține JWT Token (Login)

### Cu PowerShell (curl.exe)
```powershell
$body = '{"usernameOrEmail":"admin@cashtactics.com","password":"password"}'
curl.exe -s -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d $body
```

### Cu Postman
- **Method:** POST
- **URL:** `http://localhost:8081/api/auth/login`
- **Body → raw → JSON:**
```json
{
  "usernameOrEmail": "admin@cashtactics.com",
  "password": "password"
}
```

### Răspuns
```json
{
  "twoFactorRequired": false,
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "clientId": 1,
  "role": "ADMIN"
}
```

**Copiază valoarea `token`** — o folosești la toate request-urile următoare.

### Configurare Postman (o singură dată)
1. Creează o variabilă de collection/environment: `TOKEN`
2. Lipește token-ul ca valoare
3. La fiecare request, în tab-ul **Authorization**:
   - Type: **Bearer Token**
   - Token: `{{TOKEN}}`

### Configurare PowerShell (o singură dată per sesiune)
```powershell
$TOKEN = "eyJhbGciOiJIUzI1NiJ9..."   # lipește token-ul aici
```

> **Token-ul expiră în 15 minute.** Dacă primești 401/403, refă login-ul.

---

## Useri de test disponibili

| Email | Parola | Role | ClientId |
|-------|--------|------|----------|
| admin@cashtactics.com | password | ADMIN | 1 |
| user@cashtactics.com | password | USER | 2 |
| john.doe@example.com | password | USER | 3 |
| jane.smith@example.com | password | USER | 4 |

Niciunul nu are 2FA activat, deci login-ul returnează direct JWT-ul.

---

## Pasul 2 — Creează un PaymentMethod de test pe Stripe

### Cu Stripe CLI
```bash
# Card care mereu REUȘEȘTE (Visa 4242)
stripe payment_methods create --type=card -d "card[token]=tok_visa"

# Card care mereu EȘUEAZĂ (declined)
stripe payment_methods create --type=card -d "card[token]=tok_chargeDeclined"

# Card cu fonduri insuficiente
stripe payment_methods create --type=card -d "card[token]=tok_chargeDeclinedInsufficientFunds"

# Mastercard de test
stripe payment_methods create --type=card -d "card[token]=tok_mastercard"
```

Din răspuns, copiază `"id": "pm_..."` — acesta este ID-ul PaymentMethod.

### Tokeni de test Stripe utili

| Token | Card | Comportament |
|-------|------|-------------|
| `tok_visa` | Visa 4242 | Mereu reușește |
| `tok_mastercard` | Mastercard 5555 | Mereu reușește |
| `tok_chargeDeclined` | Visa 0002 | Mereu refuzat |
| `tok_chargeDeclinedInsufficientFunds` | Visa 9995 | Fonduri insuficiente |
| `tok_chargeDeclinedExpiredCard` | Visa 0069 | Card expirat |
| `tok_chargeDeclinedProcessingError` | Visa 0119 | Eroare procesare |

---

## Pasul 3 — Attach Payment Method (salvează cardul în DB)

### Cu PowerShell
```powershell
$headers = @{"Content-Type"="application/json"; "Authorization"="Bearer $TOKEN"}
$body = '{"clientId":1,"stripePaymentMethodId":"pm_XXXXX"}'
Invoke-RestMethod -Method POST -Uri "http://localhost:8085/api/payment-methods" -Headers $headers -Body $body | ConvertTo-Json
```

### Cu Postman
- **Method:** POST
- **URL:** `http://localhost:8085/api/payment-methods`
- **Authorization:** Bearer Token → `{{TOKEN}}`
- **Body → raw → JSON:**
```json
{
  "clientId": 1,
  "stripePaymentMethodId": "pm_XXXXX"
}
```

### Răspuns (succes)
```json
{
  "id": 7,
  "clientId": 1,
  "stripePaymentMethodId": "pm_XXXXX",
  "cardBrand": "visa",
  "cardLast4": "4242",
  "expiryMonth": 3,
  "expiryYear": 2027,
  "isDefault": false,
  "createdAt": "2026-03-05T19:46:50"
}
```

---

## Pasul 4 — Listează Payment Methods

### Cu PowerShell
```powershell
$headers = @{"Authorization"="Bearer $TOKEN"}
Invoke-RestMethod -Uri "http://localhost:8085/api/payment-methods/by-client/1" -Headers $headers | ConvertTo-Json -Depth 5
```

### Cu Postman
- **Method:** GET
- **URL:** `http://localhost:8085/api/payment-methods/by-client/1`
- **Authorization:** Bearer Token → `{{TOKEN}}`

---

## Pasul 5 — Creează o plată (flow-ul principal)

### Cu PowerShell
```powershell
$headers = @{"Content-Type"="application/json"; "Authorization"="Bearer $TOKEN"}
$body = @'
{
  "clientId": 1,
  "accountId": 1,
  "amount": 25.99,
  "currencyCode": "eur",
  "paymentMethodId": "pm_XXXXX",
  "description": "Test payment"
}
'@
Invoke-RestMethod -Method POST -Uri "http://localhost:8085/api/payments" -Headers $headers -Body $body | ConvertTo-Json
```

### Cu Postman
- **Method:** POST
- **URL:** `http://localhost:8085/api/payments`
- **Authorization:** Bearer Token → `{{TOKEN}}`
- **Body → raw → JSON:**
```json
{
  "clientId": 1,
  "accountId": 1,
  "amount": 25.99,
  "currencyCode": "eur",
  "paymentMethodId": "pm_XXXXX",
  "description": "Test payment"
}
```

### Răspuns (succes — card 4242)
```json
{
  "id": 11,
  "clientId": 1,
  "accountId": 1,
  "amount": 25.99,
  "currencyCode": "EUR",
  "status": "COMPLETED",
  "stripePaymentIntentId": "pi_3T7fzg...",
  "description": "Test payment",
  "createdAt": "2026-03-05T19:47:29"
}
```

### Răspuns (eroare — card declined)
```
HTTP 402 Payment Required
```
```json
{
  "status": 402,
  "error": "Payment Required",
  "message": "Payment failed: Your card was declined.; code: card_declined",
  "path": "/api/payments",
  "timestamp": "2026-03-05T19:48:42"
}
```

> **Ce se întâmplă în spate:**
> 1. Payment-service creează un Stripe PaymentIntent (suma în cenți)
> 2. Stripe procesează plata cu cardul atașat
> 3. Dacă reușește → status `COMPLETED`, dacă nu → `FAILED` + eroare 402
> 4. Stripe trimite webhook events → Terminal 3 le prinde → `WebhookController` actualizează statusul

---

## Pasul 6 — Verifică o plată (GET by ID)

### Cu PowerShell
```powershell
$headers = @{"Authorization"="Bearer $TOKEN"}
Invoke-RestMethod -Uri "http://localhost:8085/api/payments/11" -Headers $headers | ConvertTo-Json
```

### Cu Postman
- **Method:** GET
- **URL:** `http://localhost:8085/api/payments/11`
- **Authorization:** Bearer Token → `{{TOKEN}}`

---

## Pasul 7 — Listează plățile unui client

### Cu PowerShell
```powershell
$headers = @{"Authorization"="Bearer $TOKEN"}
Invoke-RestMethod -Uri "http://localhost:8085/api/payments/by-client/1" -Headers $headers | ConvertTo-Json -Depth 5
```

### Cu Postman
- **Method:** GET
- **URL:** `http://localhost:8085/api/payments/by-client/1`
- **Authorization:** Bearer Token → `{{TOKEN}}`

---

## Pasul 8 — Refund (rambursare)

### Cu PowerShell
```powershell
$headers = @{"Authorization"="Bearer $TOKEN"}
Invoke-RestMethod -Method POST -Uri "http://localhost:8085/api/payments/11/refund" -Headers $headers | ConvertTo-Json
```

### Cu Postman
- **Method:** POST
- **URL:** `http://localhost:8085/api/payments/11/refund`
- **Authorization:** Bearer Token → `{{TOKEN}}`
- **Body:** gol (nu trimite nimic)

### Răspuns
```json
{
  "id": 11,
  "status": "REFUNDED",
  "..."
}
```

> **Notă:** Doar plățile cu status `COMPLETED` pot fi rambursate. Altfel primești eroare.

---

## Pasul 9 — Set Default / Delete Payment Method

### Set Default
```
PUT http://localhost:8085/api/payment-methods/{id}/set-default
Authorization: Bearer {{TOKEN}}
```

### Delete
```
DELETE http://localhost:8085/api/payment-methods/{id}
Authorization: Bearer {{TOKEN}}
```

---

## Referință rapidă — Toate Endpoint-urile

### Payments (`/api/payments`)

| Method | URL | Descriere | Body |
|--------|-----|-----------|------|
| POST | `/api/payments` | Creează plată Stripe | CreatePaymentRequest |
| GET | `/api/payments/{id}` | Detalii plată | — |
| POST | `/api/payments/{id}/refund` | Rambursare | — |
| GET | `/api/payments/by-client/{clientId}` | Plățile unui client | — |
| POST | `/api/payments/webhook` | Stripe webhook (nu apela manual!) | — |

### Payment Methods (`/api/payment-methods`)

| Method | URL | Descriere | Body |
|--------|-----|-----------|------|
| POST | `/api/payment-methods` | Atașează card | AttachPaymentMethodRequest |
| GET | `/api/payment-methods/by-client/{clientId}` | Cardurile unui client | — |
| PUT | `/api/payment-methods/{id}/set-default` | Setează card default | — |
| DELETE | `/api/payment-methods/{id}` | Șterge card | — |

---

## Troubleshooting

| Problemă | Cauză | Soluție |
|----------|-------|---------|
| 401 Unauthorized | Token expirat sau lipsă | Refă login (Pasul 1) |
| 403 Forbidden | Token invalid sau 2FA claim | Verifică token-ul, loghează-te iar |
| 402 Payment Required | Stripe a refuzat plata | Normal pentru carduri declined; verifică PaymentMethod ID |
| 404 Not Found | ID inexistent | Verifică ID-ul plății/cardului |
| 500 Internal Server Error | Bug în cod | Verifică logurile payment-service |
| Webhook 400 Bad Request | Semnătură invalidă | Webhook secret din `.env.properties` != cel din `stripe listen` |
| Stripe CLI: `command not found` | Stripe CLI neinstalat | Instalează: `winget install Stripe.StripeCLI` |

---

## Flow complet de testare (checklist)

```
□ 1. Pornește auth-service     (terminal 1)
□ 2. Pornește payment-service  (terminal 2)
□ 3. Pornește stripe listen    (terminal 3)
□ 4. Login → copiază JWT token
□ 5. stripe payment_methods create --type=card -d "card[token]=tok_visa"
□ 6. POST /api/payment-methods  → atașează cardul
□ 7. GET  /api/payment-methods/by-client/1 → verifică lista
□ 8. POST /api/payments         → creează plată (verifică status: COMPLETED)
□ 9. GET  /api/payments/{id}    → verifică persistența
□ 10. POST /api/payments/{id}/refund → rambursare (verifică: REFUNDED)
□ 11. Creează card declined + încearcă plată → verifică 402
□ 12. Verifică stripe listen terminal → webhook events primite
```
