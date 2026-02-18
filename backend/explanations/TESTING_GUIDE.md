# Testing Guide

This guide explains how to test the online banking system's features.

## Quick Test Checklist

- [ ] Login with pre-created accounts
- [ ] Register a new user
- [ ] Test 2FA setup and verification
- [ ] Test refresh token auto-refresh
- [ ] Test logout revokes tokens
- [ ] View accounts and transactions
- [ ] Transfer money between accounts
- [ ] Test admin dashboard

---

## 1. Testing Authentication

### Test Login (No 2FA)

**Using the UI:**
1. Start the application (see QUICK_START.md)
2. Go to http://localhost:5174/login
3. Enter:
   - Email: `user@cashtactics.com`
   - Password: `password123`
4. Click "Login"
5. Should redirect to dashboard

**Using curl:**
```bash
curl -X POST https://localhost:8443/api/auth/login \
  -H "Content-Type: application/json" \
  -k \
  -d '{
    "usernameOrEmail": "user@cashtactics.com",
    "password": "password123"
  }'
```

**Expected response:**
```json
{
  "token": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "clientId": 1,
  "role": "USER",
  "twoFactorRequired": false
}
```

### Test Registration

**Using the UI:**
1. Go to http://localhost:5174/register
2. Enter:
   - Client ID: 5 (any number 1-25)
   - Email: `newuser@test.com`
   - Password: `password123`
3. Click "Create Account"
4. Should redirect to login

**Using curl:**
```bash
curl -X POST https://localhost:8443/api/auth/register \
  -H "Content-Type: application/json" \
  -k \
  -d '{
    "clientId": 5,
    "usernameOrEmail": "newuser@test.com",
    "password": "password123"
  }'
```

---

## 2. Testing Refresh Tokens

### Test 1: Verify tokens are stored

1. Login to the application
2. Open DevTools (F12)
3. Go to: Application → Local Storage → http://localhost:5174
4. Verify you see:
   - `jwt_token` - Access token
   - `refresh_token` - Refresh token

### Test 2: Manual token refresh

**Using curl:**
```bash
# First, login to get a refresh token
REFRESH_TOKEN="<token from login response>"

# Then refresh:
curl -X POST https://localhost:8443/api/auth/refresh-token \
  -H "Content-Type: application/json" \
  -k \
  -d "{
    \"refreshToken\": \"$REFRESH_TOKEN\"
  }"
```

**Expected response:**
```json
{
  "token": "eyJhbGc...",        # New access token
  "refreshToken": "eyJhbGc..."  # New refresh token
}
```

### Test 3: Auto-refresh in browser

This tests if expired tokens are automatically refreshed:

1. Login to the application
2. Open DevTools → Application → Local Storage
3. Edit `jwt_token` - change it to: `invalid.token.here`
4. Open DevTools → Network tab
5. Navigate somewhere (e.g., click on "Transactions")
6. Watch the Network tab - you should see:
   - First request: 401 Unauthorized
   - Second request: POST `/api/auth/refresh-token` (success)
   - Third request: Retry of original request (success)
   - Page loads normally!

### Test 4: Token rotation

Token rotation means the old refresh token is revoked when you get a new one:

1. Login and save the `refreshToken` value
2. Call `/api/auth/refresh-token` with that token → Success
3. Try using the SAME token again → Should fail (it's been revoked)

**Using curl:**
```bash
# First refresh - success
curl -X POST https://localhost:8443/api/auth/refresh-token \
  -H "Content-Type: application/json" \
  -k \
  -d '{ "refreshToken": "old_token_here" }'

# Second refresh with SAME token - should fail
curl -X POST https://localhost:8443/api/auth/refresh-token \
  -H "Content-Type: application/json" \
  -k \
  -d '{ "refreshToken": "old_token_here" }'
```

**Expected:**
- First call: 200 OK
- Second call: 401 Unauthorized

---

## 3. Testing Logout

### Test logout revokes refresh token

1. Login to get tokens
2. Call logout endpoint
3. Try to use the refresh token again → Should fail

**Using curl:**
```bash
# Login
LOGIN_RESPONSE=$(curl -X POST https://localhost:8443/api/auth/login \
  -H "Content-Type: application/json" \
  -k \
  -d '{
    "usernameOrEmail": "user@cashtactics.com",
    "password": "password123"
  }')

# Extract refresh token
REFRESH_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.refreshToken')

# Logout
curl -X POST https://localhost:8443/api/auth/logout \
  -H "Content-Type: application/json" \
  -k \
  -d "{ \"refreshToken\": \"$REFRESH_TOKEN\" }"

# Try to refresh (should fail)
curl -X POST https://localhost:8443/api/auth/refresh-token \
  -H "Content-Type: application/json" \
  -k \
  -d "{ \"refreshToken\": \"$REFRESH_TOKEN\" }"
```

**Expected:**
- Logout: 200 OK
- Refresh attempt: 401 Unauthorized

---

## 4. Testing Two-Factor Authentication (2FA)

### Test 2FA setup

1. Login as a user without 2FA
2. Go to settings/profile
3. Click "Enable 2FA"
4. Scan the QR code with Google Authenticator or Authy
5. Enter the 6-digit code shown in the app
6. Submit

**Using curl:**
```bash
# 1. Setup 2FA (get QR code)
curl -X POST https://localhost:8443/api/auth/2fa/setup \
  -H "Authorization: Bearer <your_access_token>" \
  -k

# Response contains QR code (base64 image) and secret

# 2. Confirm 2FA with code from authenticator app
curl -X POST https://localhost:8443/api/auth/2fa/confirm \
  -H "Authorization: Bearer <your_access_token>" \
  -H "Content-Type: application/json" \
  -k \
  -d '{
    "code": "123456"
  }'
```

### Test login with 2FA

1. Logout
2. Login with username/password
3. You should get a temp token and `twoFactorRequired: true`
4. Enter the 6-digit code from authenticator app
5. You should get the real access token

**Using curl:**
```bash
# 1. Login (first step)
curl -X POST https://localhost:8443/api/auth/login \
  -H "Content-Type: application/json" \
  -k \
  -d '{
    "usernameOrEmail": "user@cashtactics.com",
    "password": "password123"
  }'

# Response: { "twoFactorRequired": true, "token": "temp_token..." }

# 2. Verify 2FA
curl -X POST https://localhost:8443/api/auth/2fa/verify \
  -H "Content-Type: application/json" \
  -k \
  -d '{
    "tempToken": "temp_token_from_step_1",
    "code": "123456"
  }'

# Response: { "token": "real_token", "refreshToken": "...", ... }
```

---

## 5. Testing Banking Features

### Test viewing accounts

**Using the UI:**
1. Login as user
2. Dashboard should show all your accounts
3. Check that balances are displayed

**Using curl:**
```bash
curl -X GET https://localhost:8443/api/accounts \
  -H "Authorization: Bearer <your_access_token>" \
  -k
```

### Test money transfer

**Using the UI:**
1. Login as user
2. Click "Transfer Money"
3. Select source account
4. Enter recipient IBAN
5. Enter amount
6. Submit

**Using curl:**
```bash
curl -X POST https://localhost:8443/api/transactions \
  -H "Authorization: Bearer <your_access_token>" \
  -H "Content-Type: application/json" \
  -k \
  -d '{
    "accountId": 1,
    "recipientIban": "RO49AAAA1B31007593840000",
    "amount": 100.00,
    "description": "Test transfer"
  }'
```

### Test transaction history

**Using the UI:**
1. Login as user
2. View transactions list

**Using curl:**
```bash
curl -X GET https://localhost:8443/api/transactions \
  -H "Authorization: Bearer <your_access_token>" \
  -k
```

---

## 6. Testing Admin Features

### Test admin login

1. Login with:
   - Email: `admin@cashtactics.com`
   - Password: `password123`
2. Should redirect to `/admin` (admin dashboard)

### Test view all clients

**Using the UI:**
1. Login as admin
2. Admin dashboard shows all clients

**Using curl:**
```bash
curl -X GET https://localhost:8443/api/clients/view \
  -H "Authorization: Bearer <admin_access_token>" \
  -k
```

### Test view all transactions

**Using the UI:**
1. Login as admin
2. Click "All Transactions"
3. Should see transactions from all users

**Using curl:**
```bash
curl -X GET https://localhost:8443/api/transactions/view-all \
  -H "Authorization: Bearer <admin_access_token>" \
  -k
```

---

## 7. Testing Error Cases

### Test wrong password

Login with wrong password - should get 401 Unauthorized:

```bash
curl -X POST https://localhost:8443/api/auth/login \
  -H "Content-Type: application/json" \
  -k \
  -d '{
    "usernameOrEmail": "user@cashtactics.com",
    "password": "wrongpassword"
  }'
```

### Test expired refresh token

This is hard to test without waiting 7 days, but you can check the database:

```sql
-- Mark a token as expired
UPDATE refresh_tokens 
SET expiry_date = NOW() - INTERVAL '1 day'
WHERE token = 'your_token_here';

-- Try to use it
-- Should fail with "Token has expired"
```

### Test missing authentication

Try to access a protected endpoint without a token:

```bash
curl -X GET https://localhost:8443/api/accounts \
  -k

# Expected: 401 Unauthorized
```

---

## 8. Database Verification

### Check refresh tokens are saved

```sql
-- View all refresh tokens
SELECT 
    id,
    SUBSTRING(token, 1, 20) || '...' as token_preview,
    user_id,
    created_at,
    expiry_date,
    revoked_at,
    CASE 
        WHEN revoked_at IS NOT NULL THEN 'REVOKED'
        WHEN expiry_date < NOW() THEN 'EXPIRED'
        ELSE 'ACTIVE'
    END as status
FROM refresh_tokens
ORDER BY created_at DESC;
```

### Check token is revoked after logout

```sql
-- After logout, check your token
SELECT * FROM refresh_tokens 
WHERE token = 'your_token_here';

-- revoked_at should have a timestamp
```

### Check expired tokens

```sql
-- Find expired tokens
SELECT * FROM refresh_tokens
WHERE expiry_date < NOW();
```

---

## 9. Browser Testing Scenarios

### Happy Path
1. Register new user ✓
2. Login ✓
3. View accounts ✓
4. Make a transfer ✓
5. View transaction history ✓
6. Logout ✓

### Token Refresh Path
1. Login ✓
2. Wait 15 minutes (or manually expire access token)
3. Navigate to another page
4. Auto-refresh should happen
5. Page loads normally ✓

### 2FA Path
1. Login without 2FA ✓
2. Enable 2FA ✓
3. Logout
4. Login (enter password)
5. Enter 2FA code
6. Access granted ✓

### Admin Path
1. Login as admin ✓
2. View all clients ✓
3. View all transactions ✓
4. Logout ✓

---

## Common Issues & Solutions

### Issue: "Invalid token" when refreshing
**Solution:** Token might have been revoked. Login again.

### Issue: Infinite refresh loop
**Solution:** Check that `/auth/*` endpoints don't trigger refresh interceptor.

### Issue: 2FA code not working
**Solution:** 
- Check phone time is synced
- Code changes every 30 seconds
- Use the most recent code

### Issue: Transfer fails with insufficient funds
**Solution:** Check account balance first. Test with smaller amount.

### Issue: Cannot access admin endpoints as user
**Solution:** This is expected. Admin endpoints require ADMIN role.

---

## Testing Complete Workflow

Test the complete user journey:

1. **Day 1**: User registers and logs in
2. **Day 1**: User makes several transactions
3. **Day 2-7**: User uses the app daily (tokens auto-refresh)
4. **Day 8**: Refresh token expires, user must login again
5. **User logs out**: All tokens revoked immediately

This demonstrates:
- Tokens work correctly
- Auto-refresh works
- Token expiration works
- Logout works
- Security is maintained

---

## Pro Tips

- Use browser DevTools Network tab to see all API calls
- Use DevTools Application tab to inspect localStorage
- Check backend console logs for errors
- Use database queries to verify data changes
- Test both happy path and error cases
- Try multiple browsers to ensure consistency

---

Happy testing! 🧪
