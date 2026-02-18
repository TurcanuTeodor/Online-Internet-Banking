# System Architecture

This document explains how the Online Banking System is structured and how its components work together.

## Overall System Design

The system follows a **layered architecture** with clear separation between frontend, backend, and database:

```
┌─────────────────────────────────────────────────────────────────┐
│                     FRONTEND (React)                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  React Pages (Login.jsx, Dashboard.jsx, etc.)                   │
│                    ↓                                             │
│  Service Layer (authService.js, apiClient.js)                   │
│                    ↓                                             │
│  localStorage (jwt_token, refresh_token)                        │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
                       ↕ HTTP/HTTPS (JSON)
┌─────────────────────────────────────────────────────────────────┐
│                  BACKEND (Spring Boot)                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  REST Controllers (AuthController, AccountController, etc.)     │
│                    ↓                                             │
│  Service Layer (AuthService, RefreshTokenService, etc.)         │
│                    ↓                                             │
│  Repository Layer (JPA Repositories)                            │
│                    ↓                                             │
│  Entity/Domain Models (User, RefreshToken, Account, etc.)       │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
                       ↕ JDBC
┌─────────────────────────────────────────────────────────────────┐
│                   DATABASE (PostgreSQL)                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Tables: users, refresh_tokens, accounts, transactions, etc.    │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Frontend Architecture

### Component Structure

```
Frontend Components
├── Pages (React Components)
│   ├── Login.jsx - User login page
│   ├── Register.jsx - New user registration
│   ├── TwoFactorVerify.jsx - 2FA verification
│   ├── Dashboard.jsx - User dashboard (accounts, transactions)
│   └── AdminDashboard.jsx - Admin view (all clients/transactions)
│
├── Services (API Communication)
│   ├── authService.js - Authentication (login, logout, 2FA)
│   ├── apiClient.js - HTTP client with auto-refresh
│   ├── accountService.js - Account operations
│   ├── transactionService.js - Transaction operations
│   └── clientService.js - Client data operations
│
└── Storage
    └── localStorage
        ├── jwt_token - Access token (15 min lifespan)
        └── refresh_token - Refresh token (7 day lifespan)
```

### How Auto-Refresh Works

The `apiClient.js` has a **response interceptor** that automatically handles expired tokens:

1. User makes an API call
2. If response is **401 Unauthorized** (token expired):
   - Automatically calls `/api/auth/refresh-token` with refresh token
   - Gets new access token
   - Retries the original request
   - User doesn't notice anything!

---

## Backend Architecture

### Layer Responsibilities

**1. Controller Layer** - Handles HTTP requests
- Receives JSON requests
- Validates input
- Calls service layer
- Returns JSON responses

**2. Service Layer** - Business logic
- Implements core functionality
- Handles transactions
- Coordinates between different services

**3. Repository Layer** - Database access
- CRUD operations
- Custom database queries

**4. Entity Layer** - Data models
- Represents database tables as Java classes
- Defines relationships between tables

### Key Services

**AuthService** - Authentication
- `login()` - Validates credentials, creates tokens
- `verify2fa()` - Verifies 2FA codes
- `refreshToken()` - Issues new access token
- `logout()` - Revokes refresh token

**RefreshTokenService** - Token management
- `createRefreshToken()` - Generates and stores refresh token
- `verifyRefreshToken()` - Validates refresh token
- `revokeRefreshToken()` - Marks token as revoked

**JwtService** - JWT operations
- `generateToken()` - Creates access token (15 min)
- `generateRefreshToken()` - Creates refresh token (7 days)
- `parseClaims()` - Extracts data from token
- `isValid()` - Validates token signature

---

## Database Schema (Key Tables)

### users
```sql
id (PK)
username_or_email (UNIQUE)
password_hash
role (ADMIN or USER)
two_factor_enabled
two_factor_secret
client_id (FK → clients)
```

### refresh_tokens
```sql
id (PK)
token (UNIQUE) - The JWT refresh token
user_id (FK → users.id)
created_at
expiry_date - When the token expires (7 days from creation)
revoked_at - NULL if still valid, timestamp if revoked
```

**Why store refresh tokens in database?**
- Can revoke tokens when user logs out
- Can force logout from all devices
- Can clean up expired tokens
- Provides audit trail

### accounts
```sql
id (PK)
client_id (FK → clients.id)
iban (UNIQUE)
balance
currency_type_id (FK → currency_type)
status (ACTIVE, CLOSED, SUSPENDED)
opened_date
```

### transactions
```sql
id (PK)
account_id (FK → accounts.id)
amount
timestamp
transaction_type_id (FK → transaction_type)
recipient_iban
description
```

---

## Data Flow Diagrams

### 1. Login Flow

```
User enters credentials
    ↓
POST /api/auth/login
    ↓
Backend validates username/password
    ↓
Generate 2 tokens:
  - Access token (JWT, 15 min) 
  - Refresh token (JWT, 7 days) → saved to database
    ↓
Return: { token, refreshToken, role, clientId }
    ↓
Frontend stores both tokens in localStorage
```

### 2. Auto-Refresh Flow (When Access Token Expires)

```
User makes API call
    ↓
Access token expired → 401 Unauthorized
    ↓
apiClient interceptor detects 401
    ↓
Automatically calls POST /api/auth/refresh-token
    ↓
Backend:
  - Verifies refresh token signature
  - Checks database (not revoked, not expired)
  - Generates new access token
  - Optionally: revokes old refresh token, generates new one
    ↓
Returns: { token, refreshToken }
    ↓
Frontend updates localStorage
    ↓
Retries original API call with new token
    ↓
Success! User never noticed the token expired
```

### 3. Logout Flow

```
User clicks logout
    ↓
POST /api/auth/logout with refresh token
    ↓
Backend marks refresh token as revoked in database
    ↓
Frontend clears localStorage
    ↓
Redirect to login page
```

---

## Security Features

### Token-Based Authentication
- **Access Token**: Short-lived (15 min), stored in localStorage
  - Contains user info (role, clientId)
  - Verified on every API request
  - Cannot be revoked (but expires quickly)

- **Refresh Token**: Long-lived (7 days), stored in database + localStorage
  - Can be revoked (logout functionality)
  - Used to get new access tokens
  - Tracked in database for security

### Two-Factor Authentication (2FA)
- Uses TOTP (Time-based One-Time Password)
- Compatible with Google Authenticator, Authy, etc.
- User scans QR code during setup
- Required on every login if enabled

### Password Storage
- Passwords are hashed using BCrypt
- Never stored in plain text
- Salt is automatically generated per password

---

## Key Configuration

### Backend (application.properties)
```properties
# JWT Configuration
app.jwt.secret=your-secret-key-here
app.jwt.issuer=CashTactics
app.jwt.expiration-minutes=15       # Access token lifetime
app.jwt.temp-expiration-minutes=5   # Temporary tokens for 2FA
app.jwt.refresh-token-days=7        # Refresh token lifetime

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/banking
spring.datasource.username=postgres
spring.datasource.password=your-password

# Flyway (database migrations)
spring.flyway.enabled=true
```

### Frontend (apiClient.js)
```javascript
const API_BASE_URL = '/api';  // Backend runs on same domain in production

// Tokens stored in localStorage:
// - jwt_token: Access token
// - refresh_token: Refresh token
```

---

## Why This Architecture?

### Separation of Concerns
- Frontend handles UI/UX
- Backend handles business logic and data
- Database handles data persistence

### Scalability
- Frontend can be deployed to CDN
- Backend can run on multiple servers
- Database can be replicated

### Security
- JWT tokens can't be tampered with (signed)
- Refresh tokens can be revoked
- Passwords are securely hashed
- 2FA adds extra security layer

### Maintainability
- Clear layer boundaries
- Each component has single responsibility
- Easy to test individual parts
- Well-documented code structure

---

## Common Scenarios

### How logging in works:
1. User enters username/password
2. Backend validates credentials
3. Backend creates access + refresh tokens
4. Frontend stores tokens
5. User is redirected based on role (admin/user)

### How API calls work:
1. Frontend attaches access token to request
2. Backend verifies token
3. If valid: processes request and returns data
4. If expired: frontend auto-refreshes and retries

### How logging out works:
1. User clicks logout
2. Frontend sends refresh token to backend
3. Backend marks it as revoked in database
4. Frontend clears all tokens
5. User redirected to login page

### How 2FA works:
1. User enables 2FA in settings
2. Backend generates secret and QR code
3. User scans with authenticator app
4. On login: user must enter 6-digit code
5. Backend verifies code matches

---

This architecture provides a solid foundation for a secure, maintainable online banking system suitable for learning and demonstration purposes.
