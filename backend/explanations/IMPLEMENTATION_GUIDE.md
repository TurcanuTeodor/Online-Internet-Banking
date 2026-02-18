# Implementation Guide

This guide explains how the key features of the banking system are implemented.

## Refresh Token System

The refresh token system allows users to stay logged in for 7 days without re-entering their password, while still maintaining security with short-lived access tokens.

### Files Created

**Backend:**
1. `RefreshToken.java` - Entity (database table representation)
2. `RefreshTokenRepository.java` - Database operations
3. `RefreshTokenService.java` - Business logic for tokens
4. `RefreshTokenRequest.java` & `RefreshTokenResponse.java` - DTOs for API

**Frontend:**
- Modified `authService.js` - Added refresh token handling
- Modified `apiClient.js` - Added auto-refresh interceptor

**Database:**
- `V13__Create_refresh_tokens_table.sql` - Flyway migration

### Modified Files

**Backend:**
- `JwtService.java` - Added `generateRefreshToken()` method
- `AuthService.java` - Updated login/logout to handle refresh tokens
- `AuthController.java` - Added `/refresh-token` and `/logout` endpoints
- `LoginResponse.java` - Added `refreshToken` field

**Frontend:**
- `authService.js` - Store/use refresh tokens
- `apiClient.js` - Auto-refresh on 401 errors

---

## Backend Implementation Details

### 1. RefreshToken Entity

**Location:** `ro.app.banking.model.entity.RefreshToken`

```java
@Entity
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, length = 2048)
    private String token;  // The JWT string
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;  // Which user owns this token
    
    private LocalDateTime createdAt;
    private LocalDateTime expiryDate;  // 7 days from creation
    private LocalDateTime revokedAt;   // NULL if still valid
    
    // Helper methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
    
    public boolean isRevoked() {
        return revokedAt != null;
    }
    
    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }
}
```

**Why we need this:**
- Access tokens cannot be revoked (they're just JWT strings)
- By storing refresh tokens in the database, we can revoke them
- Enables "logout from all devices" functionality

### 2. RefreshTokenRepository

**Location:** `ro.app.banking.repository.RefreshTokenRepository`

```java
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findByToken(String token);
    
    List<RefreshToken> findByUser(User user);
    
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user " +
           "AND rt.revokedAt IS NULL AND rt.expiryDate > CURRENT_TIMESTAMP")
    List<RefreshToken> findActiveTokensByUser(@Param("user") User user);
    
    void deleteByUser(User user);
}
```

**What these methods do:**
- `findByToken()` - Lookup token when user tries to refresh
- `findActiveTokensByUser()` - Get all valid tokens for a user
- `deleteByUser()` - Remove all tokens when needed

### 3. RefreshTokenService

**Location:** `ro.app.banking.security.jwt.RefreshTokenService`

Key methods:

**createRefreshToken(User user)**
```java
public RefreshToken createRefreshToken(User user) {
    // Generate JWT
    String tokenValue = jwtService.generateRefreshToken(user);
    
    // Create entity
    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setToken(tokenValue);
    refreshToken.setUser(user);
    refreshToken.setCreatedAt(LocalDateTime.now());
    refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
    
    // Save to database
    return refreshTokenRepository.save(refreshToken);
}
```

**verifyRefreshToken(String token)**
```java
public RefreshToken verifyRefreshToken(String token) {
    // Verify JWT signature first
    if (!jwtService.isValid(token)) {
        throw new InvalidTokenException("Invalid token");
    }
    
    // Find in database
    RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
        .orElseThrow(() -> new InvalidTokenException("Token not found"));
    
    // Check if revoked or expired
    if (refreshToken.isRevoked()) {
        throw new InvalidTokenException("Token has been revoked");
    }
    
    if (refreshToken.isExpired()) {
        throw new InvalidTokenException("Token has expired");
    }
    
    return refreshToken;
}
```

**revokeRefreshToken(String token)**
```java
public void revokeRefreshToken(String token) {
    refreshTokenRepository.findByToken(token).ifPresent(rt -> {
        rt.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(rt);
    });
}
```

### 4. JwtService Updates

**Location:** `ro.app.banking.security.jwt.JwtService`

Added method:
```java
public String generateRefreshToken(User user) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime expiry = now.plusDays(refreshTokenDays);  // 7 days
    
    return Jwts.builder()
        .setSubject(user.getUsernameOrEmail())
        .setIssuedAt(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()))
        .setExpiration(Date.from(expiry.atZone(ZoneId.systemDefault()).toInstant()))
        .claim("type", "refresh")  // Mark as refresh token
        .claim("userId", user.getId())
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
}
```

### 5. AuthService Updates

**Location:** `ro.app.banking.service.auth.AuthService`

**login() method - now creates refresh token:**
```java
public LoginResponse login(LoginRequest request) {
    // Validate credentials...
    User user = validateCredentials(request);
    
    // Generate access token
    String accessToken = jwtService.generateToken(user);
    
    // Generate refresh token
    RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
    
    return LoginResponse.builder()
        .token(accessToken)
        .refreshToken(refreshToken.getToken())
        .clientId(user.getClient().getId())
        .role(user.getRole().name())
        .twoFactorRequired(user.isTwoFactorEnabled())
        .build();
}
```

**New refreshToken() method:**
```java
public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
    // Verify refresh token
    RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(
        request.getRefreshToken()
    );
    
    // Get user
    User user = refreshToken.getUser();
    
    // Generate new access token
    String newAccessToken = jwtService.generateToken(user);
    
    // Optional: Token rotation (revoke old, create new)
    refreshTokenService.revokeRefreshToken(refreshToken.getToken());
    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);
    
    return RefreshTokenResponse.builder()
        .token(newAccessToken)
        .refreshToken(newRefreshToken.getToken())
        .build();
}
```

**New logout() method:**
```java
public void logout(RefreshTokenRequest request) {
    refreshTokenService.revokeRefreshToken(request.getRefreshToken());
}
```

### 6. AuthController Updates

**Location:** `ro.app.banking.controller.auth.AuthController`

**New endpoints:**
```java
@PostMapping("/refresh-token")
public ResponseEntity<RefreshTokenResponse> refreshToken(
    @RequestBody RefreshTokenRequest request
) {
    RefreshTokenResponse response = authService.refreshToken(request);
    return ResponseEntity.ok(response);
}

@PostMapping("/logout")
public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
    authService.logout(request);
    return ResponseEntity.ok().build();
}
```

---

## Frontend Implementation Details

### 1. authService.js Updates

**Location:** `frontend/services/authService.js`

**login() - now stores refresh token:**
```javascript
export async function login(usernameOrEmail, password) {
  const response = await apiClient.post('/auth/login', {
    usernameOrEmail,
    password
  });
  
  const { token, refreshToken, twoFactorRequired } = response.data;
  
  if (!twoFactorRequired) {
    // Store both tokens
    localStorage.setItem('jwt_token', token);
    localStorage.setItem('refresh_token', refreshToken);
  }
  
  return response.data;
}
```

**New refreshAccessToken() function:**
```javascript
export async function refreshAccessToken() {
  const refreshToken = localStorage.getItem('refresh_token');
  
  if (!refreshToken) {
    throw new Error('No refresh token available');
  }
  
  try {
    // Use special axios instance without interceptors (prevents infinite loop)
    const response = await axios.post(
      `${API_BASE_URL}/auth/refresh-token`,
      { refreshToken }
    );
    
    const { token, refreshToken: newRefreshToken } = response.data;
    
    // Update stored tokens
    localStorage.setItem('jwt_token', token);
    if (newRefreshToken) {
      localStorage.setItem('refresh_token', newRefreshToken);
    }
    
    return response.data;
  } catch (error) {
    // Refresh failed, clear tokens and logout
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('refresh_token');
    window.location.href = '/login';
    throw error;
  }
}
```

**Updated logout() - now async:**
```javascript
export async function logout() {
  const refreshToken = localStorage.getItem('refresh_token');
  
  try {
    // Try to revoke token on server
    if (refreshToken) {
      await apiClient.post('/auth/logout', { refreshToken });
    }
  } catch (error) {
    console.error('Logout error:', error);
    // Continue with local logout even if server call fails
  }
  
  // Clear local storage
  localStorage.removeItem('jwt_token');
  localStorage.removeItem('refresh_token');
  
  // Redirect to login
  window.location.href = '/login';
}
```

### 2. apiClient.js - Auto-Refresh Interceptor

**Location:** `frontend/services/apiClient.js`

**Response interceptor with auto-refresh:**
```javascript
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Response interceptor
apiClient.interceptors.response.use(
  (response) => response,  // Success case
  async (error) => {
    const originalRequest = error.config;
    
    // If error is 401 and we haven't tried to refresh yet
    if (error.response?.status === 401 && !originalRequest._retry) {
      
      // Don't try to refresh for auth endpoints
      if (originalRequest.url.includes('/auth/')) {
        return Promise.reject(error);
      }
      
      if (isRefreshing) {
        // Already refreshing, queue this request
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
        .then(token => {
          originalRequest.headers['Authorization'] = 'Bearer ' + token;
          return apiClient(originalRequest);
        })
        .catch(err => {
          return Promise.reject(err);
        });
      }
      
      originalRequest._retry = true;
      isRefreshing = true;
      
      try {
        // Refresh the token
        const { token } = await authService.refreshAccessToken();
        
        // Update auth header
        apiClient.defaults.headers.common['Authorization'] = 'Bearer ' + token;
        originalRequest.headers['Authorization'] = 'Bearer ' + token;
        
        // Process queued requests
        processQueue(null, token);
        
        // Retry original request
        return apiClient(originalRequest);
      } catch (refreshError) {
        // Refresh failed
        processQueue(refreshError, null);
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }
    
    return Promise.reject(error);
  }
);
```

**How this works:**
1. User makes API call with expired access token
2. Server returns 401
3. Interceptor catches 401
4. Calls `refreshAccessToken()` to get new token
5. Updates the Authorization header
6. Retries the original request
7. User never sees an error!

**Request queueing:**
- If multiple requests fail at the same time, only one refresh call is made
- Other requests wait in a queue
- When refresh succeeds, all queued requests retry with the new token

---

## Database Migration

**Location:** `backend/src/main/resources/db/migration/V13__Create_refresh_tokens_table.sql`

```sql
CREATE TABLE refresh_tokens (
    id SERIAL PRIMARY KEY,
    token VARCHAR(2048) UNIQUE NOT NULL,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expiry_date TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL
);

-- Index for fast token lookup
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);

-- Index for finding user's tokens
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- Index for finding active tokens
CREATE INDEX idx_refresh_tokens_valid ON refresh_tokens(user_id, revoked_at, expiry_date) 
    WHERE revoked_at IS NULL;
```

**Why these indexes?**
- `token` - Fast lookup when verifying refresh token
- `user_id` - Fast lookup of all tokens for a user
- Composite index - Fast filtering of active tokens

---

## Configuration

### Backend (application.properties)

```properties
# Access token lifetime (short)
app.jwt.expiration-minutes=15

# Refresh token lifetime (long)
app.jwt.refresh-token-days=7

# Secret for signing tokens
app.jwt.secret=your-secret-key-here

# Issuer claim
app.jwt.issuer=CashTactics
```

---

## Testing the Implementation

### 1. Test login returns refresh token
```bash
POST /api/auth/login
{
  "usernameOrEmail": "user@cashtactics.com",
  "password": "password123"
}

Expected response:
{
  "token": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "clientId": 1,
  "role": "USER"
}
```

### 2. Test refresh token works
```bash
POST /api/auth/refresh-token
{
  "refreshToken": "eyJhbGc..."
}

Expected response:
{
  "token": "eyJhbGc...",  # New access token
  "refreshToken": "eyJhbGc..."  # New refresh token
}
```

### 3. Test logout revokes token
```bash
POST /api/auth/logout
{
  "refreshToken": "eyJhbGc..."
}

# Try to use same token again:
POST /api/auth/refresh-token
{
  "refreshToken": "eyJhbGc..."  # Same token
}

Expected: 401 Unauthorized (token has been revoked)
```

### 4. Test auto-refresh in frontend
1. Login to the application
2. Open browser DevTools → Application → Local Storage
3. See `jwt_token` and `refresh_token`
4. Change `jwt_token` to "invalid.token.here"
5. Make any API call (navigate to dashboard)
6. Check Network tab - should see:
   - Failed request with 401
   - POST to `/api/auth/refresh-token`
   - Retry of original request (success)

---

## Summary

The refresh token system provides:
- **Security**: Short-lived access tokens limit damage if stolen
- **Convenience**: Users stay logged in for 7 days
- **Control**: Tokens can be revoked when user logs out
- **Seamless UX**: Automatic refresh happens behind the scenes

The implementation touches both frontend and backend, with the database acting as the source of truth for token validity.
