# Security: Keys, Tokens și Algoritmi

## 1) Scop
Documentul centralizează toate elementele de securitate din proiect: chei, token-uri, hashing, semnare JWT, 2FA și TLS.

> Notă: documentul listează **numele cheilor/configurațiilor** și modul de folosire, fără a expune valori secrete în clar.

---

## 2) Configurații cheie (Environment / Properties)

### 2.1 Microservicii (`.env.properties` + `application.properties`)

Fiecare microserviciu are propriul `.env.properties`:

- `DB_URL` – conexiune PostgreSQL
- `DB_USERNAME` – user DB
- `DB_PASSWORD` – parolă DB
- `SERVER_PORT` – port serviciu (8081-8085)
- `JWT_SECRET` – secretul folosit la semnarea/verificarea JWT (identic pe toate serviciile)
- `JWT_EXPIRATION_MINUTES` – expirare access token
- `JWT_TEMP_EXPIRATION_MINUTES` – expirare temp token (flux 2FA, doar auth-service)
- `JWT_REFRESH_TOKEN_DAYS` – expirare refresh token (doar auth-service)
- `JWT_ISSUER` – issuer claim pentru JWT
- `TOTP_APP_NAME` – issuer/nume aplicație în autenticatorul 2FA (doar auth-service)

### 2.2 API Gateway (`.env.properties` + `application.yml`)

- `SSL_KEYSTORE_PASSWORD` – parola pentru `keystore.p12`
- `JWT_SECRET` – același secret ca în celelalte servicii (validare-only)

### 2.3 Frontend (`.env.local`)

- `VITE_API_URL` – URL API Gateway (HTTPS)

---

## 3) Keys / Secrets folosite în aplicație

### 3.1 Cheia JWT (simetrică)

- Tip: secret HMAC (simetric)
- Sursă: `app.jwt.secret` (din `JWT_SECRET`)
- Utilizare: semnare (auth-service) și verificare (toate serviciile + gateway)
- Implementare: `JwtService` folosește `Keys.hmacShaKeyFor(...)`
- **Important:** Același secret pe toate serviciile pentru verificare token

### 3.2 Secret 2FA per utilizator (TOTP)

- Tip: secret random generat pentru fiecare user
- Generare: `DefaultSecretGenerator`
- Stocare: coloana `two_factor_secret` în entitatea `User` (auth schema)
- Utilizare: validarea codurilor TOTP (Google/Microsoft Authenticator)

### 3.3 SSL Keystore (API Gateway)

- Tip: certificat + cheie privată server în `keystore.p12` (PKCS12, RSA 2048)
- Config: `server.ssl.*` în `application.yml` (gateway)
- Utilizare: HTTPS/TLS pentru transport securizat
- Port: 8443

---

## 4) Tokens – tipuri, scop și lifecycle

### 4.1 Access Token (JWT)

- Scop: autorizare la endpoint-uri protejate
- Expirare: `JWT_EXPIRATION_MINUTES` (15 min)
- Algoritm semnare: HS256
- Claims: `iss`, `sub`, `iat`, `exp`, `role`, `clientId`, `2fa`, `2fa_verified`
- Trimitere: header `Authorization: Bearer <token>`
- Stocare frontend: `localStorage.jwt_token`
- **Validat de:** API Gateway (pre-validation) + fiecare microserviciu (full validation)

### 4.2 Temp Token (JWT pentru 2FA)

- Scop: etapă intermediară la login când 2FA este activ
- Expirare: `JWT_TEMP_EXPIRATION_MINUTES` (5 min)
- Claims relevante: `2fa = pending`, `purpose = 2fa`
- Emis de: auth-service

### 4.3 Refresh Token

- Scop: obținere access token nou după expirare
- Expirare: `JWT_REFRESH_TOKEN_DAYS` (7 zile)
- Implementare: token JWT + persistență în tabela `auth.refresh_tokens`
- Validare: existență, nerevocat, neexpirat + semnătură validă
- Stocare frontend: `localStorage.refresh_token`
- Rotație: la refresh se revocă token-ul vechi și se emite unul nou

---

## 5) Algoritmi crypto / hash

### 5.1 BCrypt (parole)

- Utilizare: hash parole user
- Implementare: `BCryptPasswordEncoder` (Spring Security)
- Flux: register → `encode(...)`, login → `matches(...)`
- Observație: hashing cu salt (nu criptare reversibilă)

### 5.2 HMAC-SHA256 (JWT)

- Utilizare: semnare/verificare JWT
- Implementare: `signWith(key, Jwts.SIG.HS256)`
- Observație: JWT este „signed", nu „encrypted"

### 5.3 TOTP cu SHA256

- Utilizare: coduri 2FA cu valabilitate scurtă
- Implementare: `DefaultCodeGenerator(HashingAlgorithm.SHA256)`
- Parametri: 6 digits, perioadă 30 sec

### 5.4 TLS (transport)

- Protocoale: TLSv1.3 și TLSv1.2
- Utilizare: criptare trafic client → gateway (HTTPS)
- Intern (gateway → servicii): HTTP (rețea locală)

---

## 6) Encrypt / Decrypt – ce există și ce NU există

### Există
- Hashing parole (BCrypt)
- Semnare/verificare JWT (HS256)
- Generare/verificare cod TOTP (SHA256)
- Criptare transport (TLS la nivel gateway)
- Criptare câmpuri PII în `client-service` prin `EncryptionService` (AES/GCM + PBKDF2)
- Re-encrypt și migrate legacy key prin `InternalClientController`

### Nu există explicit în cod (la nivel aplicație)
- Criptare/decriptare simetrică explicită a datelor business (ex: AES pe câmpuri)
- Criptare/decriptare asimetrică explicită (ex: RSA la payload)
- JWE (JWT encrypted)

---

## 7) Unde sunt implementate (fișiere cheie)

### Per microserviciu
Fiecare serviciu (auth, client, account, transaction, payment) are:
- `SecurityConfig.java` — configurare Spring Security, `BCryptPasswordEncoder`
- `JwtService.java` — validare/parsare JWT, HS256 + key din `app.jwt.secret`
- `JwtAuthFilter.java` — filtru HTTP care extrage și validează JWT pe fiecare request

### Specific auth-service
- `RefreshTokenService.java` — emitere, verificare, revocare refresh token
- `TotpService.java` — secret TOTP, OTPAuth URI, verificare cod 2FA
- `AuthService.java` — fluxuri login/register/2FA/refresh/logout

### API Gateway
- `JwtAuthFilter.java` — pre-validare JWT (reactive, GatewayFilter)
- `JwtService.java` — validate-only (nu generează token-uri)
- `RateLimitFilter.java` — 50 req/sec via Resilience4j
- `SecurityConfig.java` — dezactivare CSRF/httpBasic/formLogin
- `keystore.p12` — certificat SSL

### Frontend
- `services/authService.js` — persistare/ștergere `jwt_token` și `refresh_token`
- `services/apiClient.js` — atașare `Authorization` header, interceptor 401 + refresh flow
- `.env.local` — `VITE_API_URL`

---

## 8) Recomandări rapide

1. Nu commit-ui valori secrete în repo (`JWT_SECRET`, `DB_PASSWORD`, `SSL_KEYSTORE_PASSWORD`).
2. În producție, folosește secret manager/variables la nivel de deployment.
3. Menține `JWT_SECRET` lung și aleator (cel puțin 256 biți).
4. Păstrează TLS activ și certificatele gestionate corect.
5. Evaluează mutarea token-urilor din `localStorage` în cookie-uri `HttpOnly` (pentru reducere risc XSS).

---

## 10) Key Management Centralizat (client-service)

### 10.1 Principiu

Cheile pentru fallback encryption sunt rezolvate doar în `client-service` prin:
- `ClientKeyResolver`
- `KeyManagementProvider` (strategie)

Astfel, nu se introduc utilitare crypto/key lookup în alte microservicii.

### 10.2 Moduri suportate

1. `env` (default local/demo)
- `KEY_MANAGEMENT_MODE=env`
- `ENCRYPTION_KEY` = cheia activă
- `ENCRYPTION_KEY_PREVIOUS` = cheia anterioară (opțional, pentru rotație)
- `ENCRYPTION_KEY_VERSION` = etichetă versiunii active (ex: `v2-2026q2`)

2. `kms` (producție)
- `KEY_MANAGEMENT_MODE=kms`
- `KMS_ACTIVE_KEY_URI`, `KMS_PREVIOUS_KEY_URI`, `KMS_ACTIVE_KEY_VERSION`
- Implementarea concretă a clientului KMS se face în `KmsKeyManagementProvider`

### 10.3 Runbook standard de rotație chei

1. Provisionezi cheia nouă în KMS (sau local env pentru demo).
2. Setezi:
- cheia nouă ca activă (`ENCRYPTION_KEY` sau `KMS_ACTIVE_KEY_URI`)
- cheia veche ca previous (`ENCRYPTION_KEY_PREVIOUS` sau `KMS_PREVIOUS_KEY_URI`)
3. Rulezi migrarea/re-encrypt pentru date legacy prin fluxul intern existent.
4. Monitorizezi erori decrypt/re-encrypt.
5. După stabilizare, elimini cheia veche din `previous`.

### 10.4 Ce nu se face

- Nu se copiază cod crypto în `account-service`, `payment-service`, `fraud-service`.
- Nu se gestionează direct chei hardcodate în cod sursă.
- Nu se rotește cheia fără fereastră `active + previous`.

---

## 9) Concluzie
Sistemul folosește un stack modern și coerent pentru autentificare/autorizare:
- Parole hashuite cu BCrypt
- JWT semnate HS256, validate la nivel gateway + serviciu
- 2FA TOTP pe SHA256
- Transport criptat prin TLS (gateway)
- Rate limiting și circuit breaker (Resilience4j)

Secretul JWT este partajat între toate microserviciile pentru a permite validarea token-urilor fără apel inter-serviciu la auth-service.
