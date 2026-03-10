# Implementation Guide

This guide explains how the key features of the banking system are implemented.

## Refresh Token System

The refresh token system allows users to stay logged in for 7 days without re-entering their password, while still maintaining security with short-lived access tokens.

### Files

**auth-service:**
1. `RefreshToken.java` - Entity (database table representation)
2. `RefreshTokenRepository.java` - Database operations
3. `RefreshTokenService.java` - Business logic for tokens
4. `RefreshTokenRequest.java` & `RefreshTokenResponse.java` - DTOs for API

**Frontend:**
- `authService.js` - Stores/uses refresh tokens
- `apiClient.js` - Auto-refresh interceptor

**Database:**
- Migration in auth-service creates `auth.refresh_tokens` table

---

## Backend Implementation Details

### 1. RefreshToken Entity

**Location:** `services/auth-service/.../model/entity/RefreshToken.java`

```java
@Entity
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, length = 2048)
    private String token;  // The JWT string
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    private LocalDateTime createdAt;
    private LocalDateTime expiryDate;  // 7 days from creation
    private LocalDateTime revokedAt;   // NULL if still valid
    
    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }
}
```

**Why store in database:** Allows revoking tokens on logout, force logout from all devices, audit trail.

### 2. RefreshTokenService

**Location:** `services/auth-service/.../security/jwt/RefreshTokenService.java`

Key methods:
- `createRefreshToken(User)` — Generates JWT + saves to DB
- `verifyRefreshToken(String)` — Validates JWT signature + checks DB (not revoked, not expired)
- `revokeRefreshToken(String)` — Sets `revokedAt` timestamp

### 3. AuthService Login Flow

```java
public LoginResponse login(LoginRequest request) {
    User user = validateCredentials(request);
    String accessToken = jwtService.generateToken(user);
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

### 4. Token Refresh (with rotation)

```java
public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
    RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(request.getRefreshToken());
    User user = refreshToken.getUser();
    
    String newAccessToken = jwtService.generateToken(user);
    
    // Token rotation: revoke old, create new
    refreshTokenService.revokeRefreshToken(refreshToken.getToken());
    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);
    
    return new RefreshTokenResponse(newAccessToken, newRefreshToken.getToken());
}
```

---

## Frontend Implementation Details

### 1. Auto-Refresh Interceptor (apiClient.js)

```javascript
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (originalRequest.url.includes('/auth/')) {
        return Promise.reject(error);  // Don't refresh for auth endpoints
      }
      
      originalRequest._retry = true;
      
      // Refresh the token
      const { token } = await authService.refreshAccessToken();
      originalRequest.headers['Authorization'] = 'Bearer ' + token;
      
      // Retry original request
      return apiClient(originalRequest);
    }
    return Promise.reject(error);
  }
);
```

**How this works:**
1. User makes API call with expired access token
2. Gateway/service returns 401
3. Interceptor catches 401, calls `/api/auth/refresh-token`
4. Gets new access token, retries original request
5. User never sees an error

### 2. Request Queueing

If multiple requests fail simultaneously, only one refresh call is made. Other requests wait in a queue and retry after the new token is obtained.

---

## Inter-Service Communication (Transfers)

### account-service → transaction-service

**Location:** `services/account-service/.../service/AccountService.java`

When a transfer is executed:

```java
public void transfer(TransferRequest request, HttpServletRequest httpRequest) {
    // 1. Update balances
    senderAccount.setBalance(senderAccount.getBalance().subtract(amount));
    receiverAccount.setBalance(receiverAccount.getBalance().add(convertedAmount));
    accountRepository.save(senderAccount);
    accountRepository.save(receiverAccount);
    
    // 2. Forward JWT token from incoming request
    String authHeader = httpRequest.getHeader("Authorization");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", authHeader);
    
    // 3. Create transaction records via REST
    RestTemplate restTemplate = new RestTemplate();
    
    // Debit record (sender)
    Map<String, Object> debitTx = Map.of(
        "accountId", senderAccount.getId(),
        "amount", amount.negate(),
        "transactionTypeCode", "TRF",
        "recipientIban", receiverAccount.getIban(),
        "description", "Transfer to " + receiverAccount.getIban()
    );
    restTemplate.postForEntity(
        "http://localhost:8084/api/transactions",
        new HttpEntity<>(debitTx, headers),
        String.class
    );
    
    // Credit record (receiver)
    // ... similar with positive amount
}
```

**Key point:** JWT is forwarded from the incoming request to authenticate with transaction-service.

---

## API Gateway Configuration

### Route Setup (GatewayConfig.java)

```java
@Bean
public RouteLocator routes(RouteLocatorBuilder builder) {
    return builder.routes()
        // Auth — public (no JWT filter)
        .route("auth-service", r -> r
            .path("/api/auth/**")
            .filters(f -> f.circuitBreaker(cb -> cb.setName("authCB")
                .setFallbackUri("forward:/fallback/auth")))
            .uri("http://localhost:8081"))
        
        // Accounts — JWT required
        .route("account-service", r -> r
            .path("/api/accounts/**")
            .filters(f -> f
                .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config()))
                .circuitBreaker(cb -> cb.setName("accountCB")
                    .setFallbackUri("forward:/fallback/service")))
            .uri("http://localhost:8083"))
        // ... similar for other services
        .build();
}
```

### JWT Pre-validation Filter

```java
public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
        String authHeader = exchange.getRequest().getHeaders()
            .getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        
        String token = authHeader.substring(7);
        if (!jwtService.isValid(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        
        return chain.filter(exchange);  // Forward to service
    };
}
```

---

## Configuration

### Per-service (application.properties)
```properties
# JWT (same secret across all services)
app.jwt.secret=EyJKunD/duQ0XmRynIKwdOPxYAsaoYtO7nqMcpLVBuc=
app.jwt.issuer=CashTactics
app.jwt.expiration-minutes=15
app.jwt.refresh-token-days=7

# Database (schema-specific)
spring.datasource.url=jdbc:postgresql://localhost:5432/banking
spring.flyway.schemas=auth  # or clients, accounts, etc.
```

### Frontend (vite.config.js)
```javascript
server: {
  proxy: {
    '/api': {
      target: 'https://localhost:8443',  // API Gateway
      changeOrigin: true,
      secure: false  // Self-signed cert
    }
  }
}
```

---

## Summary

The system provides:
- **Security**: Short-lived access tokens + revocable refresh tokens
- **Convenience**: Users stay logged in for 7 days with seamless auto-refresh
- **Resilience**: Circuit breaker + fallbacks in API Gateway
- **Isolation**: Each microservice owns its schema and business logic
- **Scalability**: Services can be independently deployed and scaled
