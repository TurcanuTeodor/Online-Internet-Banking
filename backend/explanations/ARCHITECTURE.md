# Architecture Documentation - Refresh Token System

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     FRONTEND (React/JavaScript)                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                   Application Layer                       │   │
│  │  - Login.jsx, Dashboard.jsx, TwoFactorVerify.jsx         │   │
│  │  - Makes API calls through apiClient                     │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              ↓                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Service Layer (services/)                    │   │
│  ├──────────────────────────────────────────────────────────┤   │
│  │  authService.js (Authentication)                         │   │
│  │  ├─ register()                                           │   │
│  │  ├─ login()              [MODIFIED: stores refreshToken]│   │
│  │  ├─ logout()             [MODIFIED: async + revoke]     │   │
│  │  ├─ setup2FA()                                          │   │
│  │  ├─ confirm2FA()                                        │   │
│  │  ├─ verify2FA()          [MODIFIED: stores refreshToken]│   │
│  │  └─ refreshAccessToken() [NEW]                          │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              HTTP Client Layer                            │   │
│  │                                                           │   │
│  │  apiClient.js (axios instance)                           │   │
│  │  ├─ Request Interceptor                                 │   │
│  │  │  └─ Attach: Authorization: Bearer <jwt_token>        │   │
│  │  │                                                       │   │
│  │  └─ Response Interceptor [ENHANCED: auto-refresh]       │   │
│  │     ├─ Detect 401 Unauthorized                          │   │
│  │     ├─ Lock new requests (isRefreshing = true)          │   │
│  │     ├─ Queue failed requests                            │   │
│  │     ├─ POST /api/auth/refresh-token                     │   │
│  │     ├─ Update localStorage tokens                       │   │
│  │     ├─ Unlock (isRefreshing = false)                    │   │
│  │     └─ Retry queued + original request                  │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Storage Layer                                │   │
│  │                                                           │   │
│  │  localStorage:                                           │   │
│  │  ├─ jwt_token: Access token (15 min TTL)               │   │
│  │  └─ refresh_token: Refresh token (7 day TTL)           │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
                              ↕ HTTP/HTTPS
                        (application/json)
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                  BACKEND (Spring Boot / Java)                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │             REST Controller Layer                         │   │
│  │                                                           │   │
│  │  AuthController                                          │   │
│  │  ├─ POST /auth/login               [EXISTING]           │   │
│  │  ├─ POST /auth/refresh-token       [NEW]                │   │
│  │  ├─ POST /auth/logout              [NEW]                │   │
│  │  ├─ POST /auth/2fa/setup           [EXISTING]           │   │
│  │  ├─ POST /auth/2fa/confirm         [EXISTING]           │   │
│  │  └─ POST /auth/2fa/verify          [MODIFIED]           │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              ↓                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │           Service/Business Logic Layer                    │   │
│  │                                                           │   │
│  │  AuthService                                             │   │
│  │  ├─ register()                     [EXISTING]           │   │
│  │  ├─ login()                        [MODIFIED]           │   │
│  │  ├─ verify2fa()                    [MODIFIED]           │   │
│  │  ├─ refreshToken()                 [NEW]                │   │
│  │  ├─ logout()                       [NEW]                │   │
│  │  └─ (delegates to RefreshTokenService)                  │   │
│  │                                                           │   │
│  │  RefreshTokenService               [NEW SERVICE]         │   │
│  │  ├─ createRefreshToken(user)                            │   │
│  │  ├─ verifyRefreshToken(token)                           │   │
│  │  ├─ revokeRefreshToken(token)                           │   │
│  │  ├─ revokeAllUserTokens(user)                           │   │
│  │  └─ deleteExpiredTokens()                               │   │
│  │                                                           │   │
│  │  JwtService                                              │   │
│  │  ├─ generateToken()                [EXISTING]           │   │
│  │  ├─ generateTempToken()            [EXISTING]           │   │
│  │  ├─ generateRefreshToken()         [NEW]                │   │
│  │  ├─ parseClaims()                  [EXISTING]           │   │
│  │  └─ isValid()                      [EXISTING]           │   │
│  │                                                           │   │
│  │  TotpService                                             │   │
│  │  ├─ generateSecret()               [EXISTING]           │   │
│  │  ├─ buildOtpAuthUrl()              [EXISTING]           │   │
│  │  └─ verifyCode()                   [EXISTING]           │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              ↓                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │           Repository/Data Access Layer                    │   │
│  │                                                           │   │
│  │  RefreshTokenRepository            [NEW REPOSITORY]      │   │
│  │  ├─ findByToken(token)                                  │   │
│  │  ├─ findByUser(user)                                    │   │
│  │  ├─ findActiveTokensByUser(user)                        │   │
│  │  └─ deleteByUser(user)                                  │   │
│  │                                                           │   │
│  │  UserRepository                                          │   │
│  │  └─ (existing methods)                                  │   │
│  │                                                           │   │
│  │  ClientRepository                                        │   │
│  │  └─ (existing methods)                                  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              ↓                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Entity/Domain Model Layer                    │   │
│  │                                                           │   │
│  │  RefreshToken                      [NEW ENTITY]          │   │
│  │  ├─ id: Long (PK)                                       │   │
│  │  ├─ token: String (unique, JWT)                         │   │
│  │  ├─ user: User (FK)                                     │   │
│  │  ├─ createdAt: LocalDateTime                            │   │
│  │  ├─ expiryDate: LocalDateTime                           │   │
│  │  ├─ revokedAt: LocalDateTime                            │   │
│  │  └─ Methods: isExpired(), isRevoked(), isValid()        │   │
│  │                                                           │   │
│  │  User                                                     │   │
│  │  ├─ id: Long (PK)                                       │   │
│  │  ├─ client: Client (OneToOne)                           │   │
│  │  ├─ usernameOrEmail: String (unique)                    │   │
│  │  ├─ passwordHash: String                                │   │
│  │  ├─ role: Role (ADMIN, USER)                            │   │
│  │  ├─ twoFactorEnabled: Boolean                           │   │
│  │  ├─ twoFactorSecret: String                             │   │
│  │  └─ (1:N with RefreshToken via FK)                      │   │
│  │                                                           │   │
│  │  Client                                                  │   │
│  │  └─ (existing structure)                                │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              ↓                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │            Data Transfer Object (DTO) Layer              │   │
│  │                                                           │   │
│  │  LoginResponse                                           │   │
│  │  ├─ token: String                  [EXISTING]           │   │
│  │  ├─ refreshToken: String           [NEW]                │   │
│  │  ├─ twoFactorRequired: Boolean      [EXISTING]           │   │
│  │  ├─ clientId: Long                 [EXISTING]           │   │
│  │  └─ role: String                   [EXISTING]           │   │
│  │                                                           │   │
│  │  RefreshTokenRequest                [NEW DTO]            │   │
│  │  └─ refreshToken: String                                │   │
│  │                                                           │   │
│  │  RefreshTokenResponse               [NEW DTO]            │   │
│  │  ├─ token: String                                       │   │
│  │  └─ refreshToken: String (optional)                     │   │
│  │                                                           │   │
│  │  TwoFaVerifyRequest                                      │   │
│  │  ├─ tempToken: String                                   │   │
│  │  └─ code: String                                        │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
                              ↕ JDBC
                     (PostgreSQL Driver)
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                   DATABASE (PostgreSQL)                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  refresh_tokens table                                           │
│  ├─ id (SERIAL PRIMARY KEY)                                     │
│  ├─ token (VARCHAR(2048) UNIQUE)                                │
│  ├─ user_id (INTEGER FK → users.id)                             │
│  ├─ created_at (TIMESTAMP DEFAULT NOW)                          │
│  ├─ expiry_date (TIMESTAMP)                                     │
│  ├─ revoked_at (TIMESTAMP NULL)                                 │
│  │                                                               │
│  └─ Indexes:                                                    │
│     ├─ idx_refresh_tokens_token        (Fast lookup by token)   │
│     ├─ idx_refresh_tokens_user_id      (Find user's tokens)     │
│     └─ idx_refresh_tokens_valid        (Active tokens query)    │
│                                                                   │
│  Relationships:                                                  │
│  └─ FK user_id → users(id) ON DELETE CASCADE                    │
│                                                                   │
│  Retention:                                                      │
│  ├─ Active: revoked_at IS NULL AND expiry_date > NOW            │
│  ├─ Expired: expiry_date < NOW (can be deleted)                 │
│  └─ Revoked: revoked_at IS NOT NULL (can be deleted)            │
│                                                                   │
├─────────────────────────────────────────────────────────────────┤
│  Other related tables:                                           │
│  ├─ users (existing)                                            │
│  ├─ clients (existing)                                          │
│  └─ Other tables (unaffected)                                   │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Data Flow Diagrams

### 1. Login Flow with Token Creation

```
Client Request (Browser)
  │
  ├─ POST /api/auth/login
  │   └─ Body: { usernameOrEmail, password }
  │
Frontend
  │
  └─ authService.login()
      │
      └─ HTTP POST to backend
         │
         Backend (AuthController)
         │
         └─ POST /api/auth/login handler
            │
            └─ AuthService.login()
               │
               ├─ UserRepository.findByUsernameOrEmail()
               │  └─ DB Query: SELECT * FROM users WHERE username_or_email = ?
               │
               ├─ PasswordEncoder.matches()
               │  └─ Verify password
               │
               ├─ Generate Access Token
               │  └─ JwtService.generateToken()
               │     └─ Create JWT with claims (role, clientId, 2fa status)
               │
               ├─ Generate Refresh Token
               │  └─ RefreshTokenService.createRefreshToken()
               │     │
               │     ├─ JwtService.generateRefreshToken()
               │     │  └─ Create JWT with type: "refresh"
               │     │
               │     └─ RefreshTokenRepository.save()
               │        └─ DB Insert: INSERT INTO refresh_tokens (token, user_id, ...)
               │
               └─ Return LoginResponse
                  └─ { token, refreshToken, clientId, role }
         │
         └─ HTTP Response (200 OK)
            └─ JSON: { token: "...", refreshToken: "...", ... }
  │
  └─ Frontend stores tokens
      ├─ localStorage.jwt_token = response.token
      └─ localStorage.refresh_token = response.refreshToken
```

### 2. API Request with Auto-Refresh On 401

```
Client Request (Browser)
  │
  ├─ API Call (e.g., GET /api/accounts)
  │
Frontend (apiClient)
  │
  ├─ Request Interceptor
  │   └─ Attach Authorization header
  │      └─ config.headers.Authorization = "Bearer " + localStorage.jwt_token
  │
  └─ HTTP GET to backend
     │
     Backend
     │
     ├─ Check Authorization header
     │  └─ Extract JWT and verify signature
     │
     ├─ Validate JWT claims
     │  ├─ Check: Not expired?
     │  ├─ Check: Has required claims?
     │  └─ Check: Signature valid?
     │
     ├─ If valid → Process request
     │  └─ Return 200 OK with data
     │
     └─ If invalid (expired) → Return 401 Unauthorized
        │
        Response 401
        │
        └─ Frontend
           │
           └─ Response Interceptor detects 401
              │
              ├─ Check: _retry flag?
              │  └─ If already retried once, reject
              │
              ├─ Set _retry = true
              │
              ├─ Check: /auth/ endpoint?
              │  └─ If auth endpoint, don't refresh (prevent loop)
              │
              ├─ Lock new requests
              │  └─ isRefreshing = true
              │
              ├─ Queue this failed request
              │
              ├─ Check: Already refreshing?
              │  ├─ YES → Add to queue, wait for token
              │  └─ NO → Proceed to refresh
              │
              └─ POST /api/auth/refresh-token
                 │
                 ├─ Body: { refreshToken: localStorage.refresh_token }
                 │
                 Backend
                 │
                 └─ POST /auth/refresh-token handler
                    │
                    └─ AuthService.refreshToken()
                       │
                       └─ RefreshTokenService.verifyRefreshToken()
                          │
                          ├─ RefreshTokenRepository.findByToken()
                          │  └─ DB Query: SELECT * FROM refresh_tokens WHERE token = ?
                          │
                          ├─ Check: Token exists?
                          │  └─ If not → throw InvalidToken
                          │
                          ├─ ZJwtService.isValid()
                          │  └─ Verify JWT signature
                          │
                          ├─ Check: Not expired?
                          │  └─ If isExpired() → throw ExpiredToken
                          │
                          ├─ Check: Not revoked?
                          │  └─ If isRevoked() → throw RevokedToken
                          │
                          └─ Return RefreshToken object with User
                       │
                       ├─ Generate new Access Token
                       │  └─ JwtService.generateToken()
                       │
                       ├─ Revoke old Refresh Token
                       │  └─ refreshTokenRepository.save(token.setRevokedAt(NOW))
                       │
                       ├─ Create new Refresh Token
                       │  └─ RefreshTokenService.createRefreshToken()
                       │
                       └─ Return RefreshTokenResponse
                          └─ { token: "new_access", refreshToken: "new_refresh" }
                 │
                 Response 200 OK
                 └─ JSON: { token: "...", refreshToken: "..." }
              │
              ├─ Update localStorage
              │  ├─ localStorage.jwt_token = response.token
              │  └─ localStorage.refresh_token = response.refreshToken
              │
              ├─ Unlock new requests
              │  └─ isRefreshing = false
              │
              ├─ Process queued requests
              │  └─ Resolve all queued promises with new token
              │
              ├─ Update original request header
              │  └─ originalRequest.headers.Authorization = "Bearer " + newToken
              │
              └─ Retry original request
                 │
                 Backend (now with valid token)
                 │
                 ├─ Process request
                 └─ Return 200 OK with data
              │
              └─ Frontend receives success response
                 └─ Application continues normally
```

### 3. Logout Flow

```
Client Request (Browser)
  │
  ├─ User clicks "Logout" button
  │
Frontend
  │
  └─ authService.logout()
      │
      ├─ Get refreshToken from localStorage
      │  └─ const refreshToken = localStorage.getItem('refresh_token')
      │
      ├─ HTTP POST to backend
      │  │
      │  └─ POST /api/auth/logout
      │      └─ Body: { refreshToken }
      │
      Backend
      │
      └─ AuthController.logout()
         │
         └─ AuthService.logout()
            │
            └─ RefreshTokenService.revokeRefreshToken()
               │
               ├─ RefreshTokenRepository.findByToken()
               │  └─ DB Query: SELECT * FROM refresh_tokens WHERE token = ?
               │
               ├─ If found
               │  ├─ token.setRevokedAt(NOW)
               │  └─ RefreshTokenRepository.save()
               │     └─ DB Update: UPDATE refresh_tokens SET revoked_at = NOW WHERE id = ?
               │
               └─ Return 200 OK
      │
      ├─ Frontend clears tokens
      │  ├─ localStorage.removeItem('jwt_token')
      │  └─ localStorage.removeItem('refresh_token')
      │
      ├─ Redirect to login
      │   └─ window.location.href = '/login'
      │
      └─ User sees login page
```

---

## Configuration Management

```
Production Environment
│
└─ .env.properties (Server)
   ├─ DB_URL=jdbc:postgresql://prod-db:5432/banking
   ├─ JWT_SECRET=<strong-random-secret>
   ├─ JWT_EXPIRATION_MINUTES=15
   ├─ JWT_REFRESH_TOKEN_DAYS=7
   └─ SSL_KEYSTORE_PASSWORD=<keystore-password>
```

---

## Error Handling

```
Potential Errors & Recovery
│
├─ 401 Unauthorized (access token expired)
│  └─ Auto-refresh: POST /auth/refresh-token
│     ├─ Success → Retry original request
│     └─ Failure → Logout user
│
├─ 401 Unauthorized (refresh token expired/revoked)
│  └─ Cannot refresh → Logout user, force re-login
│
├─ 401 Unauthorized (invalid signature)
│  └─ Token tampered → Logout user
│
├─ 500 Internal Server Error (DB issue)
│  └─ Refresh endpoint fails → Logout user
│
└─ Network Error
   └─ Cannot reach refresh endpoint → Logout user
```

---

## Performance Characteristics

### Token Generation
- Access Token: ~50ms (JWT signing)
- Refresh Token: ~50ms (JWT signing + DB insert)

### Token Verification
- Access Token: ~30ms (JWT verify)
- Refresh Token: ~50ms (JWT verify + DB lookup + validation)

### Database Operations
- Create refresh token: O(1) INSERT
- Find refresh token: O(1) SELECT (index on token)
- Revoke refresh token: O(1) UPDATE (primary key)
- Active tokens query: O(1) SELECT (composite index)

### Request Queueing
- Lock overhead: <1ms
- Queue processing: <100ms for up to 100 requests
- Memory: ~1KB per queued request

---

## Scalability Considerations

### Horizontal Scaling
- ✅ Stateless token verification (JWT signature only)
- ✅ Shared refresh token store (database)
- ✅ No session affinity required
- ⚠️ Database becomes bottleneck with 10K+ refresh ops/sec

### Database Optimization
```sql
-- Recommended index strategy
CREATE INDEX idx_refresh_tokens_lookup ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_status ON refresh_tokens(user_id, revoked_at, expiry_date);

-- Partitioning by user_id for very large datasets
CREATE TABLE refresh_tokens_2024_01 PARTITION OF refresh_tokens
    FOR VALUES FROM (MINVALUE) TO (1000000);
```

### Cache Layer (Optional)
```java
// Redis cache for recently verified tokens
@Cacheable(value = "refreshTokens", key = "#token.hashCode()", 
           sync = true, unless = "#result == null")
public RefreshToken verifyRefreshToken(String token) {
    // ... verification logic
}
```

---

## Security Considerations

```
Security Layers
│
├─ Transport Layer
│  └─ HTTPS/TLS 1.3
│     ├─ Tokens encrypted in transit
│     └─ Man-in-middle prevention
│
├─ Storage Layer
│  ├─ Access Token
│  │  └─ localStorage (vulnerable to XSS)
│  │     → Can be mitigated with Content Security Policy
│  │
│  └─ Refresh Token
│     ├─ localStorage (vulnerable to XSS)
│     └─ Database (secure, revocable)
│
├─ Token Generation
│  ├─ Strong secret (min 32 chars, random)
│  ├─ HMAC-SHA256 signing
│  └─ Unique issuer/audience claims
│
├─ Token Validation
│  ├─ Signature verification
│  ├─ Expiration check
│  ├─ State check (revoked_at)
│  └─ Database validation
│
├─ Token Rotation
│  └─ Old refresh token revoked immediately
│     ├─ Prevents token reuse if stolen
│     └─ Forces attacker to refresh, revealing compromise
│
└─ Rate Limiting (Recommended)
   └─ Limit refresh attempts per user/IP
      ├─ Prevent brute force on refresh token
      └─ Detect unusual activity
```

---

## Monitoring & Metrics

```
Key Metrics to Track
│
├─ Token Operations
│  ├─ Tokens created per hour
│  ├─ Tokens refreshed per hour
│  ├─ Tokens revoked per hour
│  ├─ Failed refresh attempts
│  └─ Average refresh operation time
│
├─ Database Performance
│  ├─ Query execution times
│  ├─ Index usage
│  ├─ Row count in refresh_tokens table
│  └─ Disk space usage
│
├─ Security Events
│  ├─ Failed token verifications
│  ├─ Expired token usage attempts
│  ├─ Revoked token usage attempts
│  └─ Suspicious patterns (multiple IPs, etc.)
│
└─ User Experience
   ├─ Auto-refresh success rate
   ├─ Request queue depth
   ├─ Average time to refresh
   └─ Session persistence rate
```

---

This architecture ensures:
- ✅ Secure token management
- ✅ Seamless user experience
- ✅ Scalable token handling
- ✅ Easy monitoring and maintenance
- ✅ Clear separation of concerns
