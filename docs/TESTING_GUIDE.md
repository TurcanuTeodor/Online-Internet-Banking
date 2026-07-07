# Testing Guide

This guide explains how to test the online banking system's features through the API Gateway.

**All requests go through the API Gateway** at `https://localhost:8443` (self-signed cert, use `-k` with curl).

## Quick Test Checklist

- [ ] Login with pre-created accounts
- [ ] Register a new user
- [ ] Test 2FA setup and verification
- [ ] Test refresh token auto-refresh
- [ ] Test logout revokes tokens
- [ ] View accounts and transactions
- [ ] Transfer money between accounts
- [ ] Test admin dashboard
- [ ] Test gateway JWT enforcement (401 without token)
- [ ] Test circuit breaker (503 fallback)

---

## 1. Testing Authentication

### Test Login (No 2FA)

**Using the UI:**
1. Go to http://localhost:5173/login
2. Enter: `user@cashtactics.com` / `password`
3. Should redirect to dashboard

**Using curl (through gateway):**
```bash
curl -k -X POST https://localhost:8443/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail": "user@cashtactics.com", "password": "password"}'
```

**Expected response:**
```json
{
  "token": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "clientId": 2,
  "role": "USER",
  "twoFactorRequired": false
}
```

### Test Registration

```bash
curl -k -X POST https://localhost:8443/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"clientId": 5, "usernameOrEmail": "newuser@test.com", "password": "password123"}'
```

---

## 2. Testing Refresh Tokens

### Manual token refresh

```bash
# Login first to get tokens
RESPONSE=$(curl -sk -X POST https://localhost:8443/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail": "user@cashtactics.com", "password": "password"}')

REFRESH_TOKEN=$(echo $RESPONSE | jq -r '.refreshToken')

# Refresh:
curl -k -X POST https://localhost:8443/api/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}"
```

### Auto-refresh in browser

1. Login to the application
2. Open DevTools → Application → Local Storage
3. Edit `jwt_token` to `invalid.token.here`
4. Navigate somewhere — auto-refresh should kick in
5. In Network tab: 401 → refresh call → retry (success)

### Token rotation

After refreshing, the old refresh token is revoked:
1. Refresh with token A → get token B (success)
2. Refresh with token A again → 401 (revoked)

---

## 3. Testing Logout

```bash
curl -k -X POST https://localhost:8443/api/auth/logout \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}"

# Try to use same refresh token → should fail
curl -k -X POST https://localhost:8443/api/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}"
# Expected: 401 Unauthorized
```

---

## 4. Testing 2FA

### Setup 2FA
```bash
curl -k -X POST https://localhost:8443/api/auth/2fa/setup \
  -H "Authorization: Bearer $TOKEN"
```
Response contains QR code (base64) — scan with Google Authenticator.

### Confirm 2FA
```bash
curl -k -X POST https://localhost:8443/api/auth/2fa/confirm \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"code": "123456"}'
```

### Login with 2FA
1. Login → get `twoFactorRequired: true` + temp token
2. Verify:
```bash
curl -k -X POST https://localhost:8443/api/auth/2fa/verify \
  -H "Content-Type: application/json" \
  -d '{"tempToken": "...", "code": "123456"}'
```

---

## 5. Testing Banking Features (through Gateway)

### View all accounts (admin)
```bash
curl -k https://localhost:8443/api/accounts/view \
  -H "Authorization: Bearer $TOKEN"
```

### Accounts by client
```bash
curl -k https://localhost:8443/api/accounts/by-client/1 \
  -H "Authorization: Bearer $TOKEN"
```

### Transfer money
```bash
curl -k -X POST https://localhost:8443/api/accounts/transfer \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fromIban": "RO49BANK0000000001EUR",
    "toIban": "RO49BANK0000000002EUR",
    "amount": 100.00
  }'
```

This also creates debit/credit records in transaction-service (inter-service call with JWT forwarding).

### View transactions
```bash
curl -k https://localhost:8443/api/transactions/view-all \
  -H "Authorization: Bearer $TOKEN"
```

---

## 6. Testing API Gateway Features

### JWT Enforcement (expect 401)
```bash
# No token → 401
curl -k https://localhost:8443/api/accounts/view
# Expected: 401 Unauthorized

# Invalid token → 401
curl -k https://localhost:8443/api/accounts/view \
  -H "Authorization: Bearer invalid.token.here"
# Expected: 401 Unauthorized
```

### Circuit Breaker (expect 503)
1. Stop a service (e.g., kill account-service)
2. Call its endpoint through gateway:
```bash
curl -k https://localhost:8443/api/accounts/view \
  -H "Authorization: Bearer $TOKEN"
# Expected: 503 with fallback message
```

### Rate Limiting
Send more than 50 requests/second — should get HTTP 429.

### Public routes (no JWT needed)
```bash
# Auth endpoints are public
curl -k -X POST https://localhost:8443/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail": "admin@cashtactics.com", "password": "password"}'
# Expected: 200 OK with tokens
```

---

## 7. Testing Admin Features

### Login as admin
```bash
curl -k -X POST https://localhost:8443/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail": "admin@cashtactics.com", "password": "password"}'
```

### View all clients
```bash
curl -k https://localhost:8443/api/clients/view \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### View all transactions
```bash
curl -k https://localhost:8443/api/transactions/view-all \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

## 8. Error Cases

### Wrong password
```bash
curl -k -X POST https://localhost:8443/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail": "user@cashtactics.com", "password": "wrong"}'
# Expected: 401
```

### Missing auth on protected route
```bash
curl -k https://localhost:8443/api/accounts/view
# Expected: 401
```

### Service down
```bash
# Stop transaction-service, then:
curl -k https://localhost:8443/api/transactions/view-all \
  -H "Authorization: Bearer $TOKEN"
# Expected: 503 with fallback message
```

---

## 9. Database Verification

```sql
-- Check refresh tokens
SELECT id, SUBSTRING(token, 1, 20) || '...' as token_preview,
       user_id, created_at, expiry_date,
       CASE WHEN revoked_at IS NOT NULL THEN 'REVOKED'
            WHEN expiry_date < NOW() THEN 'EXPIRED'
            ELSE 'ACTIVE' END as status
FROM auth.refresh_tokens ORDER BY created_at DESC;

-- Check account balances after transfer
SELECT iban, balance FROM accounts.accounts WHERE iban IN (
    'RO49BANK0000000001EUR', 'RO49BANK0000000002EUR'
);

-- Check transaction records created by inter-service call
SELECT * FROM transactions.transactions ORDER BY id DESC LIMIT 5;
```

---

## Common Issues & Solutions

| Issue | Solution |
|---|---|
| CSRF error on gateway | Ensure `SecurityConfig.java` exists with `.csrf().disable()` |
| 401 on inter-service calls | Check JWT forwarding in account-service (Authorization header) |
| 503 fallback | Target service is down — start it |
| Self-signed cert warning | Use `-k` with curl or `secure: false` in Vite proxy |
| Token refresh loop | Ensure `/auth/*` endpoints skip the refresh interceptor |
| 2FA code not working | Check phone time is synced, code changes every 30 sec |

---

Happy testing!
