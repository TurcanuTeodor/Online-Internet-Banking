# Refresh Token Implementation - Summary

## Ce am implementat

### Backend (Java/Spring)

#### 1. Entity - RefreshToken
- Locație: `ro.app.banking.model.entity.RefreshToken`
- Stochează refresh tokens în baza de date
- Proprietary:
  - `token`: JWT token value
  - `user`: relație cu User entity
  - `createdAt`, `expiryDate`: gestionare expirare (7 zile default)
  - `revokedAt`: pentru logout / token revocation
- Metode helper:
  - `isExpired()`: verifică dacă token a expirat
  - `isRevoked()`: verifică dacă a fost revocat
  - `isValid()`: ambele condiții

#### 2. Repository - RefreshTokenRepository
- Locație: `ro.app.banking.repository.RefreshTokenRepository`
- Operații CRUD pe refresh tokens
- Queries custom:
  - `findByToken()`: lookup by token value
  - `findActiveTokensByUser()`: tokens valide ale unui user
  - `deleteByUser()`: logout complet

#### 3. Service - RefreshTokenService
- Locație: `ro.app.banking.security.jwt.RefreshTokenService`
- Responsabil pentru:
  - `createRefreshToken()`: crează şi salvează token nou
  - `verifyRefreshToken()`: validare JWT + database check
  - `revokeRefreshToken()`: logout single token
  - `revokeAllUserTokens()`: logout din toate deviceurile
  - `deleteExpiredTokens()`: cleanup periodic

#### 4. JwtService - extensii
- Adaugat `generateRefreshToken()` method
- Refresh tokens sunt JWT simple cu claim `type: "refresh"`
- TTL: 7 zile (configurabil via `app.jwt.refresh-token-days`)

#### 5. AuthService - modificări
- Login/2FA verify acum returnează refresh token
- `refreshToken()`: genereaza nou access token din refresh token + rotation
- `logout()`: revocă refresh token
- Token rotation: viechi refresh token e revocat, noul e generat

#### 6. AuthController - noi endpoints
- `POST /api/auth/refresh-token`
  - Input: `{ refreshToken: "..." }`
  - Output: `{ token: "...", refreshToken: "..." }`
- `POST /api/auth/logout`
  - Input: `{ refreshToken: "..." }`
  - Revocă token-ul

#### 7. LoginResponse - update
- Acum include `refreshToken` field
- Pentru 2FA requests: refresh token = null (se generează după verify)

#### 8. Migration - V13__Create_refresh_tokens_table.sql
- Tabela `refresh_tokens` cu:
  - Foreign key pe users (ON DELETE CASCADE)
  - Index pe token + user_id + status
  - Für performance optimization

---

### Frontend (React/JavaScript)

#### 1. authService.js - noi funcții
- `refreshAccessToken()`: 
  - Apelează backend `/auth/refresh-token`
  - Auto-logout dacă refresh fails
  
- `logout()` async:
  - Apelează `/auth/logout` cu refresh token
  - Curăță localStorage chiar dacă API fails
  - Redirect la login

#### 2. apiClient.js - Auto-Refresh Interceptor
**Request Interceptor**:
- Attach `Authorization: Bearer <jwt_token>` din localStorage

**Response Interceptor** (sophisticate):
- Detectează 401 Unauthorized
- Șterge retry logic loops cu `_retry` flag
- Queue management: dacă deja making refresh request, queue alte requests
- Execută refresh pe alt axios instance (fără interceptors) = prevent infinite loop
- Retry original request cu noul token
- Logout automat dacă refresh fails

---

## Flow-uri

### Login Flow
```
User Login Request
    ↓
POST /api/auth/login (username, password)
    ↓
Backend validates credentials
    ↓
Generate:
  - JWT access token (15 min default)
  - Refresh token (7 days default) → salvat în DB
    ↓
LoginResponse: { token, refreshToken, clientId, role, twoFactorRequired }
    ↓
Frontend localStorage:
  - jwt_token: token
  - refresh_token: refreshToken
```

### Auto-Refresh Flow
```
API Call → Server returns 401 (token expired)
    ↓
Interceptor detected 401
    ↓
Lock new requests (isRefreshing = true)
Queue failed requests
    ↓
POST /api/auth/refresh-token (refreshToken)
    ↓
Backend verifies refresh token:
  - JWT signature check
  - Database check (not revoked, not expired)
  - Generates new access token
  - Optionally: revokes old refresh + generates new
    ↓
Return: { token, refreshToken }
    ↓
Frontend updates localStorage
Unlock queue (isRefreshing = false)
Retry all queued requests
Retry original request
```

### Logout Flow
```
User clicks logout
    ↓
authService.logout()
    ↓
POST /api/auth/logout (refreshToken)
    ↓
Backend revokes token:
  - Sets revokedAt timestamp
  - Token cannot be reused
    ↓
Frontend clears localStorage:
  - jwt_token
  - refresh_token
    ↓
Redirect to /login
```

---

## Security Features

1. **Token Separation**:
   - Access token: short-lived (15 min), full claims
   - Refresh token: long-lived (7 days), minimalist claims

2. **Database Tracking**:
   - Every refresh token stored in DB
   - Can revoke individual tokens
   - Can logout all sessions per user

3. **Token Rotation**:
   - Old refresh token revoked on refresh
   - New refresh token issued
   - Mitigates token theft risk

4. **Expired Token Cleanup**:
   - `deleteExpiredTokens()` method
   - Schedule periodic cleanup (job?)

5. **Request Queueing**:
   - Multiple simultaneous requests don't trigger multiple refreshes
   - All requests use same new token

6. **No Exposed Tokens**:
   - Refresh tokens sent in request bodies (not headers)
   - Access tokens cleared on logout

---

## Configuration

```properties
# JWT Access Token
app.jwt.expiration-minutes=15

# Temp Token (2FA)
app.jwt.temp-expiration-minutes=5

# Refresh Token
app.jwt.refresh-token-days=7

# JWT Config
app.jwt.secret=<your-secret>
app.jwt.issuer=<your-issuer>
```

---

## Database Cleanup (TODO)

Recomandare: Setup scheduled task pentru cleanup refresh tokens expirat:

```java
@Service
public class TokenCleanupTask {
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    public void cleanupExpiredTokens() {
        refreshTokenService.deleteExpiredTokens();
    }
}
```

---

## Testing Recommendations

1. **Manual Testing**:
   - Login → token stored
   - Wait token expiration
   - Make API call → auto-refresh happens
   - Check network tab for refresh-token call

2. **Edge Cases**:
   - Multiple tabs: both should use same refresh token
   - Logout in one tab: other tabs should fail on next request
   - Invalid refresh token: should force logout
   - Expired refresh token: should force logout

3. **Load Testing**:
   - Concurrent requests → queueing works
   - Multiple refreshes → no race conditions

---

## Future Enhancements

1. **Device Management**: Track refresh tokens per device/browser
2. **Refresh Token Expiration Alerts**: Warn user before expiration
3. **Token Binding**: Tie tokens to IP/device fingerprint
4. **Revocation List (CRL)**: Central list of revoked tokens
5. **Sliding Window**: Extend expiry on each refresh
