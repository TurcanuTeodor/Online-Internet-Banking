# Deployment & Setup Guide

## Pre-Deployment Checklist

### Backend
- [x] Refresh token entity created
- [x] Database migration created (V13)
- [x] Services implemented
- [x] Controllers updated
- [x] DTOs created
- [x] Code compiles successfully
- [x] No runtime errors in compilation

### Frontend
- [x] authService.js updated with refresh logic
- [x] apiClient.js updated with auto-refresh interceptor
- [x] localStorage keys defined (jwt_token, refresh_token)
- [x] No breaking changes to existing code

### Documentation
- [x] REFRESH_TOKEN_IMPLEMENTATION.md - Technical details
- [x] REFRESH_TOKEN_TESTING.md - Testing guide
- [x] IMPLEMENTATION_SUMMARY.md - Change summary
- [x] QUICK_REFERENCE.md - Developer reference
- [x] This file - Deployment guide

---

## Configuration for Production

### 1. Backend Environment Variables

Create `.env.properties` file:
```properties
# Database
DB_URL=jdbc:postgresql://your-host:5432/your-db
DB_USERNAME=your_user
DB_PASSWORD=your_password

# JWT Configuration - IMPORTANT: Use strong secret!
JWT_SECRET=your-very-long-and-random-secret-min-32-chars-preferably-random
JWT_ISSUER=your-app-name
JWT_EXPIRATION_MINUTES=15
JWT_TEMP_EXPIRATION_MINUTES=5
JWT_REFRESH_TOKEN_DAYS=7

# SSL/HTTPS
SERVER_PORT=8443
SSL_KEYSTORE_PASSWORD=your_keystore_password

# 2FA
TOTP_APP_NAME=YourBankingAppName
```

**Security Tips**:
- Use strong random secret: `openssl rand -base64 32`
- Different values for dev/staging/production
- Never commit .env.properties to git
- Use secure password manager for secrets

### 2. Frontend Configuration

Update `frontend/services/apiClient.js` if needed:
- Base URL already uses relative `/api`
- For production with different domain:
  ```javascript
  const API_BASE_URL = process.env.REACT_APP_API_URL || '/api';
  ```

### 3. Database Migration

The migration file `V13__Create_refresh_tokens_table.sql` will run automatically on startup via Flyway:

```bash
cd backend
mvn spring-boot:run
```

Flyway will:
1. Check existing migrations
2. Run V13 (create refresh_tokens table)
3. Update schema version

---

## Deployment Steps

### Step 1: Prepare Backend

```bash
cd backend

# Clean build
mvn clean package -DskipTests

# Or with tests:
mvn clean package
```

This creates: `target/banking-backend-0.0.1-SNAPSHOT.jar`

### Step 2: Prepare Frontend

```bash
cd frontend

# Install dependencies
npm install

# Build for production
npm run build

# Output: dist/ folder with static files
```

### Step 3: Deploy Backend JAR

```bash
# Place JAR and .env.properties on production server
scp backend/target/banking-backend-0.0.1-SNAPSHOT.jar user@prod:/app/
scp .env.properties user@prod:/app/

# SSH into server
ssh user@prod

# Start application (background)
nohup java -jar banking-backend-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

# Or use systemd (recommended):
# Create /etc/systemd/system/banking-app.service
sudo systemctl start banking-app
sudo systemctl enable banking-app
```

### Step 4: Deploy Frontend

Option A: Serve as static files from Spring Boot:
```bash
# Copy frontend build to backend resources
cp -r frontend/dist/* backend/src/main/resources/static/

# Rebuild backend JAR
mvn clean package
```

Option B: Separate web server (nginx):
```bash
# Copy frontend to web server
scp -r frontend/dist/* user@prod:/var/www/banking/

# Update nginx config:
server {
    listen 443 ssl;
    server_name yourdomain.com;
    
    root /var/www/banking;
    index index.html;
    
    location /api {
        proxy_pass https://backend:8443;
    }
}

sudo systemctl restart nginx
```

---

## Post-Deployment Verification

### 1. Database Check
```sql
-- Verify refresh_tokens table exists
\dt refresh_tokens

-- Verify indexes
\di refresh_tokens*

-- Sample data check (after first login)
SELECT COUNT(*) FROM refresh_tokens;
```

### 2. Backend Health
```bash
# Check if running
curl -k https://localhost:8443/api/auth/login

# Should return 405 Method Not Allowed (GET not allowed on POST endpoint)
# This confirms backend is running

# Check logs
tail -f /app/app.log
```

### 3. Frontend Access
```bash
# Test login
curl -k https://yourdomain.com/login
# Should return HTML

# Test API call
curl -k https://yourdomain.com/api/auth/login
# Should be forwarded to backend
```

### 4. Functional Test
1. Open browser: https://yourdomain.com
2. Go to Login page
3. Enter valid credentials
4. Check DevTools → Application → Local Storage:
   - `jwt_token` should exist
   - `refresh_token` should exist
5. Navigate to Dashboard
6. Open DevTools → Network tab
7. Make API call (refresh page)
8. Verify Authorization header contains token

---

## Monitoring & Maintenance

### Log Monitoring
```bash
# Watch logs in real-time
tail -f /app/app.log | grep -E "RefreshToken|ERROR"

# Check for failed token operations
grep "Refresh token revoked\|Invalid refresh token" /app/app.log
```

### Database Maintenance

#### Weekly: Cleanup expired tokens
```sql
DELETE FROM refresh_tokens 
WHERE expiry_date < NOW() AND revoked_at IS NOT NULL;

-- Or schedule with application:
-- Add to application.properties:
# spring.task.scheduling.pool.size=1
# Then schedule in RefreshTokenService
```

#### Monthly: Review token statistics
```sql
SELECT 
    DATE_TRUNC('day', created_at) as date,
    COUNT(*) as total_issued,
    COUNT(CASE WHEN revoked_at IS NOT NULL THEN 1 END) as revoked,
    COUNT(CASE WHEN expiry_date < NOW() THEN 1 END) as expired
FROM refresh_tokens
GROUP BY DATE_TRUNC('day', created_at)
ORDER BY date DESC;
```

#### Check for suspicious activity
```sql
-- Users with many active tokens (possible multiple devices)
SELECT 
    u.username_or_email,
    COUNT(rt.id) as active_tokens,
    MAX(rt.created_at) as last_login
FROM users u
LEFT JOIN refresh_tokens rt ON u.id = rt.user_id 
    AND rt.revoked_at IS NULL 
    AND rt.expiry_date > NOW()
GROUP BY u.id, u.username_or_email
HAVING COUNT(rt.id) > 5
ORDER BY COUNT(rt.id) DESC;
```

### Performance Monitoring

Monitor these metrics:
- Average refresh token requests per hour
- Average time to refresh token
- Number of expired tokens
- Number of active sessions

```sql
-- Requests per hour (if you log them)
SELECT DATE_TRUNC('hour', created_at), COUNT(*)
FROM refresh_tokens
WHERE created_at > NOW() - INTERVAL '24 hours'
GROUP BY DATE_TRUNC('hour', created_at);
```

---

## Troubleshooting

### Issue: Database migration fails
```
ERROR: relation "refresh_tokens" already exists
```
**Solution**: Manual migration already ran. 
- Check V13 in flyway_schema_history
- Delete if error, re-run migration

### Issue: Tokens not refreshing (401 loops)
**Check**:
1. Is refresh-token endpoint accessible?
   ```bash
   curl -X POST https://localhost:8443/api/auth/refresh-token \
     -H "Content-Type: application/json"
   ```

2. Is RefreshTokenService bean registered?
   - Check Spring logs for bean creation
   
3. Is refresh_token in localStorage?
   - Debug in browser console: `localStorage.getItem('refresh_token')`

### Issue: Database connection errors
```
ERROR: could not connect to database
```
**Check**:
- .env.properties values
- Database server running
- Network connectivity
- User permissions

### Issue: High memory usage
**Solution**: Cleanup expired tokens
```sql
DELETE FROM refresh_tokens 
WHERE expiry_date < NOW() - INTERVAL '30 days';
```

---

## Backup & Recovery

### Backup Refresh Tokens
```bash
# Daily backup
pg_dump --table=refresh_tokens your_db > refresh_tokens_$(date +%Y%m%d).sql

# Full database backup
pg_dump your_db > full_backup_$(date +%Y%m%d).sql
```

### Recovery
```bash
# If refresh tokens corrupted
psql your_db < refresh_tokens_backup.sql

# If table missing
psql -d your_db -f backend/src/main/resources/db/migration/V13__Create_refresh_tokens_table.sql
```

---

## Performance Optimization (Optional)

### 1. Add caching for token verification
```java
@Cacheable(value = "validRefreshTokens", key = "#token")
public RefreshToken verifyRefreshToken(String token) {
    // ...
}
```

### 2. Batch cleanup job
```java
@Service
public class TokenCleanupJob {
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    public void cleanupExpiredTokens() {
        refreshTokenService.deleteExpiredTokens();
        logger.info("Expired tokens cleaned up");
    }
}
```

### 3. Monitor refresh token requests
```java
// Add metric in RefreshTokenService
meterRegistry.counter("refresh.token.requests").increment();
meterRegistry.counter("refresh.token.errors").increment();
```

---

## Security Hardening

### 1. Rate limiting for refresh endpoint
```java
@Bean
public RateLimiter refreshTokenRateLimiter() {
    return RateLimiter.create(10.0); // 10 requests per second
}

// In controller
@PostMapping("/refresh-token")
public ResponseEntity<RefreshTokenResponse> refreshToken(
    @Valid @RequestBody RefreshTokenRequest req) {
    if (!rateLimiter.tryAcquire()) {
        return ResponseEntity.status(429).build(); // Too Many Requests
    }
    // ...
}
```

### 2. Token binding to IP
```java
// Store IP when creating token
RefreshToken rt = new RefreshToken();
rt.setCreatedIp(request.getRemoteAddr());

// Verify on refresh
if (!request.getRemoteAddr().equals(rt.getCreatedIp())) {
    throw new SecurityException("Token used from different IP");
}
```

### 3. Device fingerprinting
```java
// Store device fingerprint on token creation
rt.setDeviceFingerprint(calculateDeviceHash(request));

// Verify on refresh
if (!calculateDeviceHash(request).equals(rt.getDeviceFingerprint())) {
    throw new SecurityException("Token used from different device");
}
```

---

## Rollback Plan

If issues occur after deployment:

### Option 1: Rollback Backend
```bash
# Stop current
pkill -f banking-backend

# Restore previous version
java -jar banking-backend-0.0.0-SNAPSHOT.jar > app.log 2>&1 &

# Previous migration will continue working
# (refresh_tokens table exists, will just be unused)
```

### Option 2: Rollback Frontend
```bash
# If using nginx, keep previous build
mv /var/www/banking /var/www/banking-new
cp -r /var/www/banking.backup/* /var/www/banking/
sudo systemctl restart nginx
```

### Option 3: Database Rollback
This is only needed if migration corrupted data:
```bash
# Restore from backup
psql your_db < backup_before_migration.sql
```

---

## Post-Deployment Testing

1. **Manual Testing**
   - [ ] Login with correct credentials
   - [ ] Fail login with wrong password
   - [ ] Login with 2FA enabled
   - [ ] Make API call with valid token
   - [ ] Wait for token expiry (15 min)
   - [ ] Make API call → should auto-refresh
   - [ ] Check refresh token in DB
   - [ ] Logout → verify token revoked
   - [ ] Multiple tabs → logout in one affects all

2. **Automated Testing** (Optional)
   - Add integration tests for refresh flow
   - Add E2E tests for token rotation
   - Monitor token generation rate

3. **Load Testing**
   - Simulate 100+ concurrent refreshes
   - Verify request queueing works
   - Monitor database performance

---

## Documentation Artifacts

All documentation files are included:
- `REFRESH_TOKEN_IMPLEMENTATION.md` - Full technical reference
- `REFRESH_TOKEN_TESTING.md` - Test cases and scenarios
- `IMPLEMENTATION_SUMMARY.md` - Change overview
- `QUICK_REFERENCE.md` - Developer quick start
- `DEPLOYMENT_GUIDE.md` - This file

---

## Support

For issues or questions:
1. Check logs: `tail -f /app/app.log`
2. Review database: `psql your_db`
3. Check browser console for frontend errors
4. Review testing guide: `REFRESH_TOKEN_TESTING.md`
5. Check quick reference: `QUICK_REFERENCE.md`

---

**Deployment Status**: ✅ **READY FOR PRODUCTION**
- Code compiled successfully
- Database schema ready
- Configuration documented
- Testing guide available
- Monitoring recommendations included
