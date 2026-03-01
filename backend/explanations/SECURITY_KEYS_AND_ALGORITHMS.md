# Security: Keys, Tokens și Algoritmi (Backend + Frontend)

## 1) Scop
Documentul centralizează toate elementele de securitate din proiect: chei, token-uri, hashing, semnare JWT, 2FA și TLS.

> Notă: documentul listează **numele cheilor/configurațiilor** și modul de folosire, fără a expune valori secrete în clar.

---

## 2) Configurații cheie (Environment / Properties)

### 2.1 Backend (`.env.properties` + `application.properties`)

- `DB_URL` – conexiune PostgreSQL
- `DB_USERNAME` – user DB
- `DB_PASSWORD` – parolă DB
- `SERVER_PORT` – port aplicație (implicit HTTPS)
- `SSL_KEYSTORE_PASSWORD` – parola pentru `keystore.p12`
- `JWT_SECRET` – secretul folosit la semnarea/verificarea JWT
- `JWT_EXPIRATION_MINUTES` – expirare access token
- `JWT_TEMP_EXPIRATION_MINUTES` – expirare temp token (flux 2FA)
- `JWT_REFRESH_TOKEN_DAYS` – expirare refresh token
- `JWT_ISSUER` – issuer claim pentru JWT
- `TOTP_APP_NAME` – issuer/nume aplicație în autenticatorul 2FA

### 2.2 Frontend (`.env.local`)

- `VITE_API_URL` – URL API (în prezent HTTPS local)

---

## 3) Keys / Secrets folosite în aplicație

### 3.1 Cheia JWT (simetrică)

- Tip: secret HMAC (simetric)
- Sursă: `app.jwt.secret` (din `JWT_SECRET`)
- Utilizare: semnare și verificare JWT
- Implementare: `JwtService` folosește `Keys.hmacShaKeyFor(...)`

### 3.2 Secret 2FA per utilizator (TOTP)

- Tip: secret random generat pentru fiecare user
- Generare: `DefaultSecretGenerator`
- Stocare: coloana `two_factor_secret` în entitatea `User`
- Utilizare: validarea codurilor TOTP (Google/Microsoft Authenticator)

### 3.3 SSL Keystore

- Tip: certificat + cheie privată server în `keystore.p12`
- Config: `server.ssl.*` în `application.properties`
- Utilizare: HTTPS/TLS pentru transport securizat

---

## 4) Tokens – tipuri, scop și lifecycle

### 4.1 Access Token (JWT)

- Scop: autorizare la endpoint-uri protejate
- Expirare: `JWT_EXPIRATION_MINUTES`
- Algoritm semnare: HS256
- Claims uzuale:
  - `iss` (issuer)
  - `sub` (username/email)
  - `iat`, `exp`
  - custom: `role`, `clientId`, `2fa`, `2fa_verified`
- Trimitere: header `Authorization: Bearer <token>`
- Stocare frontend: `localStorage.jwt_token`

### 4.2 Temp Token (JWT pentru 2FA)

- Scop: etapă intermediară la login când 2FA este activ
- Expirare: `JWT_TEMP_EXPIRATION_MINUTES`
- Claims relevante: `2fa = pending`, `purpose = 2fa`
- Folosire: endpoint-ul de verificare 2FA

### 4.3 Refresh Token

- Scop: obținere access token nou după expirare
- Expirare: `JWT_REFRESH_TOKEN_DAYS`
- Implementare: token JWT + persistență în tabela `REFRESH_TOKENS`
- Validare: existență, nerevocat, neexpirat + semnătură validă
- Stocare frontend: `localStorage.refresh_token`
- Rotație: la refresh se poate revoca token-ul vechi și emite unul nou

---

## 5) Algoritmi crypto / hash identificați

### 5.1 BCrypt (parole)

- Utilizare: hash parole user
- Implementare: `BCryptPasswordEncoder`
- Flux:
  - register: `encode(...)`
  - login: `matches(...)`
- Observație: este hashing cu salt (nu criptare reversibilă)

### 5.2 HMAC-SHA256 (JWT)

- Utilizare: semnare/verificare JWT
- Implementare: `signWith(key, Jwts.SIG.HS256)`
- Observație: JWT este „signed”, nu „encrypted”

### 5.3 TOTP cu SHA256

- Utilizare: coduri 2FA cu valabilitate scurtă
- Implementare: `DefaultCodeGenerator(HashingAlgorithm.SHA256)`
- Parametri observați:
  - 6 digits
  - perioadă 30 sec

### 5.4 TLS (transport)

- Protocoale activate: TLSv1.3 și TLSv1.2
- Utilizare: criptare trafic client-server (HTTPS)

---

## 6) Encrypt / Decrypt – ce există și ce NU există

### Există
- Hashing parole (BCrypt)
- Semnare/verificare JWT (HS256)
- Generare/verificare cod TOTP (SHA256)
- Criptare transport (TLS)

### Nu există explicit în cod (la nivel aplicație)
- criptare/decriptare simetrică explicită a datelor business (ex: AES pe câmpuri)
- criptare/decriptare asimetrică explicită (ex: RSA la payload)
- JWE (JWT encrypted)

---

## 7) Unde sunt implementate (fișiere cheie)

### Backend
- `src/main/java/ro/app/banking/security/SecurityConfig.java`
  - configurare Spring Security
  - `PasswordEncoder` = `BCryptPasswordEncoder`
- `src/main/java/ro/app/banking/security/jwt/JwtService.java`
  - generare/validare/parsare JWT
  - HS256 + key din `app.jwt.secret`
- `src/main/java/ro/app/banking/security/jwt/RefreshTokenService.java`
  - emitere, verificare, revocare refresh token
- `src/main/java/ro/app/banking/service/auth/TotpService.java`
  - secret TOTP, OTPAuth URI, verificare cod 2FA
- `src/main/java/ro/app/banking/service/auth/AuthService.java`
  - fluxuri login/register/2FA/refresh/logout
- `src/main/resources/application.properties`
  - SSL/TLS, JWT config, import env
- `.env.properties`
  - valori runtime pentru secrete/config

### Frontend
- `services/authService.js`
  - persistare/ștergere `jwt_token` și `refresh_token`
  - apeluri `login`, `verify2FA`, `refresh-token`, `logout`
- `services/apiClient.js`
  - atașare `Authorization` header
  - interceptor 401 + refresh flow
- `.env.local`
  - `VITE_API_URL`

---

## 8) Recomandări rapide

1. Nu commit-ui valori secrete în repo (`JWT_SECRET`, `DB_PASSWORD`, `SSL_KEYSTORE_PASSWORD`).
2. În producție, folosește secret manager/variables la nivel de deployment.
3. Menține `JWT_SECRET` lung și aleator (cel puțin 256 biți).
4. Păstrează TLS activ și certificatele gestionate corect.
5. Evaluează mutarea token-urilor din `localStorage` în cookie-uri `HttpOnly` (pentru reducere risc XSS).

---

## 9) Concluzie
Sistemul folosește un stack modern și coerent pentru autentificare/autorizare:
- parole hashuite cu BCrypt,
- JWT semnate HS256,
- 2FA TOTP pe SHA256,
- transport criptat prin TLS.

În codul actual nu există criptare/decriptare explicită a datelor de business la nivel aplicație (în afara TLS și mecanismelor de autentificare).