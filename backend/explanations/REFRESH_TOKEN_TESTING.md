# Refresh Token Testing Guide

## 1. Basic Login Flow

### Test 1: Login without 2FA
```bash
curl -X POST https://localhost:8443/api/auth/login \
  -H "Content-Type: application/json" \
  -d {
    "usernameOrEmail": "test_user@example.com",
    "password": "password123"
  }
```

**Expected Response**:
```json
{
  "twoFactorRequired": false,
  "token": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "clientId": 1,
  "role": "USER"
}
```

**Frontend Storage**:
- `localStorage.jwt_token`: Access token (short-lived, 15 min)
- `localStorage.refresh_token`: Refresh token (long-lived, 7 days)

---

## 2. Token Refresh Flow

### Test 2: Manually refresh token
```bash
curl -X POST https://localhost:8443/api/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d {
    "refreshToken": "<refresh_token_from_login>"
  }
```

**Expected Response**:
```json
{
  "token": "eyJhbGc...",          # New access token
  "refreshToken": "eyJhbGc..."    # New refresh token (rotated)
}
```

---

## 3. Auto-Refresh Testing (Frontend)

### Test 3: Simulate token expiration

1. Login normally (get access token with 15 min expiration)
2. Open Browser DevTools → Application → Local Storage
3. Verify `jwt_token` and `refresh_token` are stored
4. Open Network tab (to watch refresh call)

### Test 4: Force token expiration
```javascript
// In browser console
localStorage.setItem('jwt_token', 'invalid.token.here');
```

5. Make any API call (e.g., GET `/api/accounts`)
6. Network tab should show:
   - First request: 401 Unauthorized
   - Second request: POST `/api/auth/refresh-token` (auto-refresh)
   - Third request: GET `/api/accounts` (retry with new token)
7. Check Local Storage → `jwt_token` should have new value

---

## 4. Logout Testing

### Test 5: Logout flow
```bash
curl -X POST https://localhost:8443/api/auth/logout \
  -H "Content-Type: application/json" \
  -d {
    "refreshToken": "<refresh_token>"
  }
```

**Expected**:
- 200 OK response
- Refresh token marked as revoked in database

**Frontend**:
```javascript
// authService.logout() should:
// 1. Call POST /api/auth/logout
// 2. Clear localStorage.jwt_token
// 3. Clear localStorage.refresh_token
// 4. Redirect to /login
```

---

## 5. Multi-Tab Testing

### Test 6: Token invalidation across tabs
1. Login in Tab A
2. Open same app in Tab B (should use same localStorage)
3. In Tab A, click Logout
4. In Tab B, wait 5 seconds
5. Make any API call in Tab B
6. Expected: Should get 401 → auto-refresh fails (token revoked) → redirect to login

---

## 6. 2FA + Refresh Token

### Test 7: Login with 2FA enabled
```bash
curl -X POST https://localhost:8443/api/auth/login \
  -H "Content-Type: application/json" \
  -d {
    "usernameOrEmail": "user_with_2fa@example.com",
    "password": "password123"
  }
```

**Expected Response**:
```json
{
  "twoFactorRequired": true,
  "token": "eyJhbGc...",          # Temp token (5 min, purpose-bound)
  "refreshToken": null,            # No refresh token yet!
  "clientId": 1,
  "role": "USER"
}
```

### Test 8: Verify 2FA and get refresh token
```bash
curl -X POST https://localhost:8443/api/auth/2fa/verify \
  -H "Content-Type: application/json" \
  -d {
    "tempToken": "<from_login_response>",
    "code": "123456"  # TOTP code from authenticator
  }
```

**Expected Response** (similar to Test 1):
```json
{
  "twoFactorRequired": false,
  "token": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "clientId": 1,
  "role": "USER"
}
```

---

## 7. Error Cases

### Test 9: Invalid refresh token
```bash
curl -X POST https://localhost:8443/api/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d {
    "refreshToken": "invalid.token.jwt"
  }
```

**Expected Response** (401):
```json
{
  "error": "Invalid refresh token"
}
```

### Test 10: Expired refresh token
```bash
# Manually set expiry to past date in database
UPDATE refresh_tokens SET expiry_date = NOW() - INTERVAL '1 day' WHERE id = 1;
```

Then try to refresh → Should get 401.

### Test 11: Revoked refresh token
```bash
# Manually revoke in database
UPDATE refresh_tokens SET revoked_at = NOW() WHERE id = 1;
```

Then try to refresh → Should get 401.

---

## 8. Database Verification

### Check active refresh tokens
```sql
SELECT 
  rt.id,
  u.username_or_email,
  rt.created_at,
  rt.expiry_date,
  rt.revoked_at,
  CASE 
    WHEN rt.revoked_at IS NULL AND rt.expiry_date > NOW() THEN 'VALID'
    WHEN rt.revoked_at IS NOT NULL THEN 'REVOKED'
    ELSE 'EXPIRED'
  END AS status
FROM refresh_tokens rt
JOIN "user" u ON rt.user_id = u.id
ORDER BY rt.created_at DESC;
```

---

## 9. Performance Testing

### Test 12: Concurrent requests (queue handling)
```javascript
// In browser console
Promise.all([
  fetch('/api/accounts', { headers: { 'Authorization': 'Bearer ...' } }),
  fetch('/api/users', { headers: { 'Authorization': 'Bearer ...' } }),
  fetch('/api/transactions', { headers: { 'Authorization': 'Bearer ...' } })
])
```

With expired access token:
- Should trigger ONE refresh-token call
- All 3 requests should be queued
- After refresh, all 3 should retry with new token
- Check Network tab to verify only ONE /auth/refresh-token request

---

## 10. Security Testing

### Test 13: Token tampering
```javascript
localStorage.setItem('jwt_token', localStorage.getItem('jwt_token').slice(0, -5) + 'xxxxx');
// Try to make API call → Should get 401 (invalid signature)
```

### Test 14: Token in cookie (future enhancement)
Currently tokens are in localStorage. Future: might use HttpOnly cookies to prevent XSS.

---

## Checklist for Manual Testing

- [ ] Login returns both tokens
- [ ] Tokens stored in localStorage correctly
- [ ] Refresh token endpoint works manually
- [ ] Auto-refresh on 401 works (watch Network tab)
- [ ] Token rotation works (old refresh revoked, new issued)
- [ ] Logout revokes refresh token
- [ ] Expired token cannot be refreshed
- [ ] 2FA login flow includes refresh token after verify
- [ ] Multi-tab: logout in one tab logs out all tabs
- [ ] Concurrent requests queue correctly
- [ ] Invalid tokens return 401
- [ ] Database records refresh tokens correctly

---

## Frontend Integration Checklist

- [ ] authService.js has `refreshAccessToken()` function
- [ ] authService.js has updated `logout()` function
- [ ] apiClient.js interceptor detects 401
- [ ] apiClient.js queues requests during refresh
- [ ] apiClient.js uses separate axios instance for refresh call
- [ ] Refresh token stored in localStorage on login
- [ ] Refresh token cleared on logout
- [ ] UI shows loading state during auto-refresh (optional)

---

## Postman Collection

Create requests for:
1. POST /auth/login
2. POST /auth/refresh-token
3. POST /auth/logout
4. POST /auth/2fa/verify

Set variables:
- `{{base_url}}`: https://localhost:8443
- `{{access_token}}`: js:`pm.response.json().token`
- `{{refresh_token}}`: js:`pm.response.json().refreshToken`

Use in Authorization header:
```
Bearer {{access_token}}
```
