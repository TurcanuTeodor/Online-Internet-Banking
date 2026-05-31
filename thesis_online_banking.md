# BUCHAREST ACADEMY OF ECONOMIC STUDIES
## Faculty of Cybernetics, Statistics and Economic Informatics
### Department of Economic Informatics

---

&nbsp;

# BACHELOR'S THESIS

&nbsp;

## CashTactics: Design and Implementation of a Secure Online Internet Banking Platform with Tiered Fraud Detection

&nbsp;

**Scientific Coordinator:**  
[Professor Name], PhD  
Department of Economic Informatics

&nbsp;

**Graduate:**  
[Student Name]  
Economic Informatics, Year III  
Group [XXX]

&nbsp;

**Bucharest, 2026**

---

&nbsp;

---

# TABLE OF CONTENTS

1. [Introduction](#chapter-1-introduction)
2. [Problem Statement and Related Work](#chapter-2-problem-statement-and-related-work)
3. [Technologies and Methods](#chapter-3-technologies-and-methods)
4. [System Architecture](#chapter-4-system-architecture)
5. [Implementation](#chapter-5-implementation)
6. [Testing and Evaluation](#chapter-6-testing-and-evaluation)
7. [Conclusions](#chapter-7-conclusions)
8. [Bibliography](#bibliography)
9. [Appendices](#appendices)

---

&nbsp;

---

# CHAPTER 1. INTRODUCTION

## 1.1 Motivation and Context

Online banking has changed the way people manage their money. Instead of visiting a branch, customers can transfer funds, check balances, and top up accounts from their phones at any hour of the day. This convenience comes with a price: the more digital a banking system becomes, the more it attracts fraudulent activity. According to industry data, card-not-present fraud and account takeover attacks continue to rise every year, and a bank that lacks the ability to detect unusual transaction patterns in real time is essentially flying blind.

At the same time, banking systems must comply with strict privacy regulations such as the EU General Data Protection Regulation (GDPR). This means that personal information — names, email addresses, phone numbers, home addresses — cannot simply sit in a plain-text database column waiting to be read by anyone with access to the server. And if a customer exercises their right to erasure, the bank must be able to delete or anonymise that data across every system that holds it, not just one table.

The challenge, then, is to build a banking system that is functional, secure, privacy-preserving, and fraud-aware all at the same time — and to do it in a way that does not end up as an unmanageable monolithic codebase where a bug in the payment module can accidentally bring down authentication for everyone.

This thesis presents **CashTactics**, a full-stack online internet banking platform built from the ground up to treat security, privacy, and fraud detection as first-class requirements rather than afterthoughts. The system is implemented as a collection of Spring Boot microservices backed by a React single-page frontend and an API Gateway that serves as the single public entry point. Fraud analysis is handled by a dedicated service with three tiers: a deterministic rule engine, a behavioural scoring model, and an offline-trained Isolation Forest machine learning model.

## 1.2 Project Scope

The platform provides the following capabilities:

- **Authentication and authorisation**: user registration and login with BCrypt-hashed passwords, HMAC-signed JWT access tokens, rotating refresh tokens stored in the database, and optional two-factor authentication (TOTP, compatible with Google Authenticator).
- **Client profile management**: creation and update of client profiles with personal and contact data encrypted at rest using AES-256-GCM. When a client changes their password, all encrypted data is transparently re-encrypted with a key derived from the new password.
- **Account management**: opening and closing bank accounts in four currencies (EUR, USD, RON, GBP), querying balances, freezing and unfreezing accounts.
- **Money transfers**: real-time internal transfers between accounts with exchange rate conversion fetched daily from the European Central Bank, distributed locking through Redis and Redisson to prevent race conditions, and an optional two-factor step-up for high-value transactions.
- **External payments (Stripe integration)**: one-time card top-ups via Stripe PaymentIntents, with the card number never touching the CashTactics servers (Stripe.js handles it directly), and asynchronous settlement triggered by Stripe webhook events.
- **Transaction history**: full ledger of debit and credit records, queryable by account, IBAN, date range, type, or client.
- **Fraud detection**: a three-tier pipeline that evaluates every transfer synchronously (Tiers 1 and 2) and asynchronously (Tier 3), persisting a fraud decision record for every evaluated transaction, with an alert resolution workflow for both users and administrators.
- **GDPR compliance**: a right-to-erasure endpoint that orchestrates deletion across all services (auth, client, account, transaction), plus a data export endpoint covering all personal information held about a client.
- **Admin dashboard**: read-only views of all clients, accounts, and transactions, with masked PII that can only be revealed by explicitly logging a business reason.

The system is deployed with Docker Compose and supports both a development overlay with live-reload and a production overlay with TLS termination at the gateway.

## 1.3 Contributions

The primary contributions of this work are:

1. A working microservices implementation of a banking platform with schema-per-service database isolation, where each service owns and manages exactly one PostgreSQL schema through Flyway migrations.
2. A user-derived encryption key model for PII: instead of a single server-side encryption key, each client's personal data is encrypted with a key derived (via PBKDF2) from that client's own password. The key travels inside the JWT's `ek` claim and never rests on the server between requests.
3. A three-tier fraud detection pipeline implemented as an independent microservice, where Tier 1 is a deterministic rule engine, Tier 2 is a weighted behavioural scoring algorithm, and Tier 3 is an Isolation Forest model trained offline on the PaySim financial simulation dataset.
4. A gateway-level token blacklist backed by Redis that invalidates access tokens on logout before they expire, preventing replay attacks.
5. A fully audited sensitive-data reveal workflow: administrators cannot see plain-text PII in the UI without explicitly selecting a business reason that is recorded in a persistent audit table.

## 1.4 Document Structure

The rest of this thesis is organised as follows. Chapter 2 defines the problem domain and surveys related work on fraud detection, microservices security, and data privacy. Chapter 3 describes all technologies and methods used. Chapter 4 presents the overall architecture. Chapter 5 goes through the implementation service by service. Chapter 6 covers testing and evaluation. Chapter 7 concludes with lessons learned and future directions.

---

# CHAPTER 2. PROBLEM STATEMENT AND RELATED WORK

## 2.1 The Problem Domain

Building a digital banking system that is both easy to use and genuinely secure is harder than it sounds. There are several distinct challenges that need to be addressed simultaneously.

**Authentication and session management.** Short-lived tokens reduce the window of exposure if a token is stolen, but they force users to re-authenticate constantly, which degrades the user experience. Refresh tokens extend sessions but introduce their own risks: a stolen refresh token can be used indefinitely unless the system tracks and revokes them. Balancing convenience and security here is a non-trivial design problem.

**Sensitive data protection.** Banking records contain some of the most sensitive personal information imaginable: names, addresses, phone numbers, and email addresses — all of which are regulated under GDPR. Encrypting this data at rest protects it from direct database access, but if the encryption key is stored next to the data, the protection is largely illusory. A meaningful encryption strategy needs to separate the key from the data.

**Fraud detection.** Rule-based fraud detection (e.g., "block transactions above 10,000 EUR") is fast and deterministic but brittle: fraudsters quickly learn the thresholds and structure their attacks around them. Behavioural models are more adaptive but require historical data per user and careful feature engineering. Machine learning models can detect subtle anomalies but need training data, periodic retraining, and a deployment strategy that does not add unacceptable latency to every transaction.

**Regulatory compliance.** GDPR's right to erasure (Article 17) means the system must be able to delete a client's data on request, across every microservice that holds it, without leaving orphaned records. This is surprisingly complex in a distributed system where each service owns its own database schema.

**Architecture.** A monolithic banking application solves the coordination problem simply — one process, one database — but it sacrifices isolation, independent scalability, and fault containment. A poorly designed microservices system, on the other hand, can become a distributed monolith where every service needs to call every other service before it can respond. The architecture must be designed so that services are genuinely independent, with clearly defined boundaries and minimal coupling.

## 2.2 Related Work on Fraud Detection

The fraud detection literature is extensive, and it is worth briefly situating the approach taken in this project within that landscape.

### 2.2.1 Rule-Based Systems

The earliest fraud detection systems were simple threshold rules: if a transaction exceeded a certain amount, or if multiple transactions occurred within a short window, a flag was raised. Abdallah et al. [6] provide a useful survey of rule-based approaches and note that while they are transparent, interpretable, and fast, they suffer from high false-positive rates and are easily circumvented once the rules are known. In this project, Tier 1 of the fraud pipeline implements exactly this kind of rule engine — it is the first line of defence precisely because it is cheap and deterministic, not because it is sufficient on its own.

### 2.2.2 Behavioural and Statistical Models

A more robust approach models the historical behaviour of each user and flags deviations. Bahnsen et al. [7] describe feature engineering strategies for credit card fraud that go beyond simple transaction amounts and include time-of-day, recipient novelty, and velocity measures. The Tier 2 scoring model in this project draws on similar ideas: it computes a weighted risk score based on six behavioural signals (amount anomaly relative to the user's history, transaction frequency, unusual hour, unknown recipient, transaction category, and 24-hour spend velocity) and returns a decision — ALLOW, STEP\_UP\_REQUIRED, or FLAG — to the account service before the transfer is committed.

### 2.2.3 Machine Learning Approaches

Liu, Ting, and Zhou introduced the Isolation Forest algorithm [1], which detects anomalies by measuring how easily a data point can be "isolated" from the rest of the dataset using random decision trees. Points that are isolated with very few splits are likely anomalies. This is the algorithm used for Tier 3 of the fraud pipeline. The key advantages for this use case are that it does not require labelled normal/fraud examples for training (it is an unsupervised method), and it scales well to high-dimensional feature spaces.

The training dataset is the PaySim financial transaction simulator [2], which was designed specifically to generate realistic synthetic mobile money transaction data that preserves the statistical properties of real data without exposing actual customer records. Dal Pozzolo et al. [3] discuss the challenges of realistic fraud detection modelling, particularly the severe class imbalance (typically less than 0.2% of transactions are fraudulent). He and Garcia [4] survey techniques for learning from imbalanced data, and the ModelTrainerCli in this project addresses this by computing a dynamic contamination parameter from the actual fraud rate in the training sample rather than using a hardcoded value. Finally, Gama et al. [5] discuss concept drift — the phenomenon where a model trained on old data becomes less accurate as transaction patterns change over time — which is why the audit endpoint in the fraud service reports the model's age in days and warns when it exceeds 90 days.

## 2.3 Related Work on Microservices and API Security

Richardson [8] provides one of the most comprehensive treatments of microservices patterns, including the API Gateway pattern, the Database-per-Service pattern, and the Saga pattern for distributed transactions. This project uses the API Gateway and Database-per-Service (implemented as schema-per-service rather than separate database instances, which is a practical compromise for a single-host deployment) patterns from this playbook.

Fowler [10] introduced the concept of the Strangler Fig application as a migration path from monolith to microservices, but more relevant here is his treatment of patterns of enterprise application architecture, particularly around transaction management and data access. The account transfer flow in this project uses an in-process database transaction for balance updates and a best-effort REST call to create transaction records in the transaction service — a pragmatic trade-off between strong consistency and independence.

Spring Boot [9] is the de facto standard for building microservices in Java, and the project uses it as the backbone for all six backend services. Spring Cloud Gateway provides the reactive API gateway layer built on Project Reactor and Netty.

## 2.4 Privacy and Regulatory Context

The GDPR imposes obligations that are directly relevant to the design of this system. Article 5(1)(c) requires data minimisation — collecting only the data that is necessary for a given purpose. The admin client list view in this project returns only non-PII operational fields (client type, risk level, active status) without decrypting names or contact details, implementing data minimisation at the application layer. Article 25 requires privacy by design and by default. The user-derived encryption key model means that PII is encrypted by default, and decryption requires the user's key, which is derived from their password and travels in the JWT — it is never stored server-side. Article 17 (right to erasure) is implemented as a multi-service orchestration that deactivates the auth user, closes all accounts, anonymises transaction details, and overwrites the client's name and contact info with an encrypted placeholder.

---

# CHAPTER 3. TECHNOLOGIES AND METHODS

## 3.1 Overview

This chapter describes the algorithms, methodologies, and engineering mechanisms employed in the CashTactics platform. The focus is on the analytical and security methods that solve the core problems — fraud anomaly detection, behavioural risk scoring, cryptographic data protection, distributed concurrency control, and fault-tolerant service communication — not on the commodity infrastructure that supports them. A brief summary of the core infrastructure follows in Section 3.2; the remaining sections are dedicated to the methods that represent the non-trivial problem-solving choices of this project.

## 3.2 Core Infrastructure and Frameworks

The system is built on the Java 21 / Spring Boot 3.3 ecosystem, using a microservices architecture routed through Spring Cloud Gateway (reactive, Netty-based). Persistence is handled by Spring Data JPA backed by PostgreSQL 16, with Flyway managing schema versioning for each of the six independently owned database schemas (`auth`, `clients`, `accounts`, `transactions`, `payments`, `fraud`). The frontend is a React 18 single-page application built with Vite, using Axios for HTTP communication and Stripe.js for PCI-DSS-compliant card data collection. Deployment is managed through Docker Compose with layered configuration for development and production environments. These technologies are industry-standard choices; the methods applied on top of them are described in the sections that follow.

---

## 3.3 Anomaly Detection Methodology: Isolation Forest

### 3.3.1 The Algorithm

The Isolation Forest algorithm [1] is an unsupervised anomaly detection method with a fundamentally different philosophy from density-based or distance-based detectors. Rather than modelling what "normal" looks like, it models how easy it is to isolate a data point from all others. The core insight is: **anomalies are rare and different, so they can be isolated with far fewer random partitions than normal points**.

The algorithm builds an ensemble of *isolation trees* (iTrees). To construct one iTree:

1. Select a random feature dimension *q*.
2. Select a random split value *p* uniformly in the range `[min(q), max(q)]`.
3. Partition the data into two subsets: those below *p* and those above *p*.
4. Recursively repeat on each subset until each point is isolated or a maximum tree depth is reached.

An anomalous point, being far from the data mass, will be separated near the root of the tree in very few splits. A normal point, surrounded by similar neighbours, requires many more splits to be isolated. The **anomaly score** for a point *x* is:

```
s(x, n) = 2^( -E[h(x)] / c(n) )
```

where `E[h(x)]` is the expected path length (number of splits) to isolate *x* across all trees in the forest, `n` is the number of training samples, and `c(n)` is the average path length of an unsuccessful search in a Binary Search Tree with `n` nodes, used as a normalisation factor:

```
c(n) = 2 * H(n-1) - (2*(n-1)/n)
```

where `H(i)` is the harmonic number. The score ranges from 0 to 1: values close to 1 indicate anomalies; values close to 0.5 indicate normal points.

### 3.3.2 Why Isolation Forest for Financial Fraud

Standard supervised classifiers (logistic regression, gradient boosting) require a labelled dataset of known fraud cases. In practice, genuine fraud labels are scarce, noisy, and biased — institutions often mislabel legitimate chargebacks as fraud and vice versa. Isolation Forest is anomaly detection: it needs only normal transaction data to learn what normality looks like, then flags deviations. This makes it more robust to label noise and directly applicable to the PaySim training dataset [2], which provides ground-truth fraud flags as a validation tool rather than a training signal.

The class imbalance problem (fewer than 0.2% of transactions are fraudulent in real datasets [3]) is another argument in favour of Isolation Forest. Methods that maximise overall accuracy on imbalanced data trivially achieve high accuracy by predicting the majority class for everything; Isolation Forest sidesteps this by working entirely in the anomaly space. He and Garcia [4] survey the impact of imbalance on supervised methods and find that without careful oversampling or cost-sensitive learning, they consistently underperform on the minority class.

### 3.3.3 Feature Engineering for the Model

Six features are extracted from each transaction. The same six features are computed from the PaySim training records (`PaySimFeatureMapper`) and from live transfer requests (`FeatureVectorBuilder`), ensuring there is no train-serve skew:

| Index | Feature | Formula | Motivation |
|-------|---------|---------|------------|
| 0 | Amount ratio | `min(amount / 50000, 1.0)` | Normalises transaction size to Romanian legal Transfond cap |
| 1 | Type risk | `{POS: 0.0, INTERNAL: 1.0, EXTERNAL/INSTANT: 3.0}` | Irreversible external transfers carry higher inherent risk |
| 2 | Hour suspicion | `{01-05h: 3.0, 00/06-07/23h: 2.0, else: 1.0}` | Nighttime fraud exploits delayed victim notification |
| 3 | New account flag | `1.0 if age < 30 days, else 0.0` | Burner accounts are a money-laundering indicator |
| 4 | Sender depletion ratio | `min(amount / oldBalance, 1.0)` | Account-takeover signature: full account drain |
| 5 | Round amount flag | `1.0 if amount % 100 < ε` | Cash-out attacks use psychologically round amounts |

Before training and inference, all features are transformed using **MinMax scaling**:

```
x_scaled = (x - min_train) / (max_train - min_train)
```

The training set's per-feature minimum and maximum values are saved in the serialised model snapshot (`ModelStore.ModelSnapshot`) and applied identically during inference. This guarantees that out-of-distribution values at inference time are clamped to `[0, 1]` rather than causing unpredictable score behaviour.

### 3.3.4 Offline Training and Threshold Calibration

Training is decoupled from application startup. The `ModelTrainerCli` CommandLineRunner (activated only when `fraud.tier3.trainer-mode=true`) performs a full offline training pipeline:

1. **Data loading**: up to 150,000 rows from the PaySim CSV are read. Sub-sampling at 150,000 rows is intentional — the full dataset of 6.3 million rows exhibits "masking" where the sheer volume of normal transactions overwhelms the anomaly signal.
2. **Stratified 80/20 split**: the indices of fraud and normal records are partitioned separately, then each partition is independently split 80/20. This preserves the original fraud rate (~12% in the sub-sample) in both train and test sets, preventing the test set from accidentally containing a disproportionate number of anomalies.
3. **MinMax scaling**: computed on the training set only; the `min` and `max` arrays are saved with the model.
4. **Isolation Forest training**: a forest of 50 trees with maximum depth 10 is fitted with a `contamination` parameter set dynamically to the actual fraud rate in the training sample (rather than a hardcoded value). This is important because the contamination parameter influences the internal threshold of the forest.
5. **Threshold calibration**: instead of using the default threshold, the trainer sweeps from 0.30 to 0.90 in 0.01 increments and selects the threshold that maximises F-beta with beta=0.5. The beta value is deliberately below 1, which weights Precision more than Recall. In a banking context, false positives (blocking a legitimate transfer) have a direct, measurable user experience cost, while the Tier 1 and Tier 2 layers already handle the most obvious fraud cases; Tier 3 is meant to catch subtle anomalies with high confidence.
6. **Serialisation**: the `ModelSnapshot` object — containing the trained model, calibrated threshold, feature means (for perturbation analysis), feature min/max arrays, version string, training timestamp, and training statistics — is serialised to a binary `.bin` file using Java object serialisation.

### 3.3.5 Explainability via Perturbation Analysis

A fundamental limitation of the Isolation Forest algorithm—and many machine learning approaches—is its "black-box" nature, which provides anomaly scores without contextual justification. To produce human-readable explanations suitable for banking administrators, the proposed architecture implements perturbation-based feature importance. 

For a given transaction, the system calculates the relative contribution of each feature to the final anomaly score. Let $x$ be the original feature vector, $S(x)$ be the anomaly score, and $\mu_i$ be the training-set mean for the $i$-th feature. The importance $I_i$ of feature $i$ is calculated by replacing its value with the mean $\mu_i$ and measuring the absolute change in the resulting score:

$$ I_i = | S(x) - S(x_{1}, \dots, \mu_i, \dots, x_{n}) | $$

The feature that yields the largest absolute difference $\Delta S$ when perturbed is identified as the primary driver of the anomaly. 

Once the importance vector is calculated, an internal reasoning component translates these numerical drivers into natural language. This ensures that the technical output of the machine learning model is accessible to non-technical fraud analysts. For instance, the system might generate the following explanation:

> *"Transaction flagged as suspicious (anomaly score: 0.78). Primary factor: sender account heavily or fully drained (62% contribution). Secondary factor: high-risk transaction type (28% contribution)."*

These generated explanations are persisted alongside the fraud decision record and exposed directly within the administrative dashboard, supporting a streamlined alert resolution workflow.

### 3.3.6 Concept Drift Monitoring

A known challenge in deploying machine learning models is concept drift, defined as the gradual divergence between the statistical properties of the training dataset and the live production data distribution [5]. To mitigate this risk, the fraud service exposes a dedicated Actuator endpoint (`/actuator/fraud-model`) that tracks the temporal age of the model in days and emits a system warning when this threshold exceeds 90 days. While this age-based heuristic serves as a baseline control, a rigorous production deployment should periodically compute the Population Stability Index (PSI) on the anomaly score distribution. Under this framework, an automated alert would be triggered when the PSI exceeds 0.2, which represents the conventionally accepted statistical threshold for significant distribution shift.

---

## 3.4 Behavioural Risk Scoring Methodology

### 3.4.1 Motivation

Tier 2 of the fraud pipeline is a behavioural scoring model that compares the current transaction against the user's historical transaction profile. Unlike Tier 1 (deterministic rules) and Tier 3 (anomaly detection on raw features), Tier 2 is personalised — the same transaction amount that is unremarkable for a high-volume business client is suspicious for a student account. The approach is inspired by feature engineering strategies documented in the fraud detection literature [7].

### 3.4.2 Behavioural Profile Construction

Before scoring, `BehaviorProfileService.recompute()` updates the user's `UserBehaviorProfile` entity using the transaction history fetched from transaction-service. The profile stores:

- `avgTransactionAmount` and `maxTransactionAmount`: computed over outgoing transactions.
- `transactionCount`: total number of outgoing transactions.
- `avgDailyTransactions`: total transactions divided by the number of distinct active days.
- `typicalHourStart` and `typicalHourEnd`: the two-hour window centred on the hour with the most historical transactions.
- `commonIbans`: a comma-separated list of IBANs that appear in previous transaction details (recipient history).

### 3.4.3 Composite Risk Score Formula

The composite risk score *S* is a weighted sum of six component scores, each mapped to the range [0, 100]:

```
S = 0.30 * S_amount + 0.20 * S_frequency + 0.15 * S_time
  + 0.15 * S_recipient + 0.10 * S_category + 0.10 * S_velocity
```

The component scores are computed as follows:

**Amount anomaly** (*S_amount*): based on the z-score of the current amount relative to the user's historical distribution:

```
z = (amount - μ) / σ
S_amount = { 5   if z ≤ 1.0
           { 30  if z ≤ 2.0
           { 60  if z ≤ 3.0
           { min(100, 70 + (z - 3.0) * 10)  otherwise
```

where μ is `avgTransactionAmount` and σ is the sample standard deviation computed from the transaction history (falling back to `0.3 * μ` when history is insufficient).

**Frequency anomaly** (*S_frequency*): ratio of today's transaction count to the user's average daily count:

```
ratio = todayCount / avgDailyTransactions
S_frequency = { 5   if ratio ≤ 1.5
              { 35  if ratio ≤ 2.5
              { 65  if ratio ≤ 4.0
              { min(100, 75 + (ratio - 4.0) * 5)  otherwise
```

**Time anomaly** (*S_time*): measures the distance of the current transaction hour from the user's typical window. If the current hour falls inside `[typicalHourStart, typicalHourEnd]`, the score is 5 (low risk). Outside the window, the score increases by 15 per hour of distance, up to 100.

**Recipient anomaly** (*S_recipient*): if the receiver IBAN appears in `commonIbans` (a known counterparty), score is 5. For an unknown recipient, the score is scaled by amount: 80 for amounts above 2,000, 50 for amounts above 500, 30 otherwise. Self-transfers are automatically scored 0.

**Category risk** (*S_category*): a base risk level by transaction type:

```
DEPOSIT           →  5
TRANSFER_INTERNAL →  15
WITHDRAWAL        →  30
TRANSFER_EXTERNAL →  40
```

**24-hour velocity** (*S_velocity*): total outgoing amount in the last 24 hours (including the current transaction) divided by the user's average daily outgoing spend over the last 30 days:

```
ratio = sum24h / avgDailySpend30d
S_velocity = { 5   if ratio ≤ 1.5
             { 35  if ratio ≤ 3.0
             { 65  if ratio ≤ 5.0
             { min(100, 75 + (ratio - 5.0) * 5)  otherwise
```

### 3.4.4 Decision Mapping

The composite score *S* maps to a fraud decision status according to three configurable thresholds:

```
S < lower_threshold (default 30)   →  ALLOW
S ∈ [step_up_threshold, upper)     →  STEP_UP_REQUIRED  (default [50, 70))
S ≥ upper_threshold (default 70)   →  FLAG
```

The thresholds are externally configurable through `FraudProperties.Tier2`, which allows operational tuning without code changes.

---

## 3.5 Cryptographic Methods for Data Protection

### 3.5.1 Password Hashing: BCrypt Adaptive Function

Passwords are stored as BCrypt hashes. BCrypt is an adaptive hash function designed specifically for password storage: it is built on the Blowfish cipher and includes an explicit work factor that can be increased as hardware becomes faster. Unlike MD5 or SHA-1, BCrypt is intentionally slow — a single hash computation takes on the order of 100ms at the default work factor. This makes dictionary and brute-force attacks computationally prohibitive: an attacker who steals the database can attempt at most ~10 guesses per second per CPU core, compared to billions per second for fast hashes.

### 3.5.2 Key Derivation: PBKDF2 with HMAC-SHA256

The user's PII encryption key is derived from their password using **PBKDF2WithHmacSHA256** (Password-Based Key Derivation Function 2, RFC 8018) with the following parameters:

```
key = PBKDF2(password, salt, iterations=65536, keyLength=256 bits)
```

The 65,536 iteration count is the critical parameter: each iteration applies one round of HMAC-SHA256, and 65,536 rounds means a single key derivation takes approximately 100–200ms on modern hardware. An attacker attempting a dictionary attack must pay this cost for every candidate password — a total of roughly 65,536 × number\_of\_candidates × HMAC-SHA256 operations. The 24-byte random salt (generated fresh for each user at registration) prevents rainbow table pre-computation: even if two users have the same password, their derived keys are different.

The key derivation is implemented in `AuthService.deriveEncryptionKey()` using Java's standard `SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")`, which produces a 256-bit `SecretKey` returned as a Base64-encoded string for storage in the JWT `ek` claim.

### 3.5.3 Symmetric Encryption: AES-256-GCM

All personally identifiable information is encrypted at rest using **AES-256-GCM** (Advanced Encryption Standard in Galois/Counter Mode). The choice of GCM over other AES modes (CBC, CTR) is motivated by two properties:

**Confidentiality and authenticity in a single pass.** AES-GCM is an Authenticated Encryption with Associated Data (AEAD) scheme. It produces not only a ciphertext but also an authentication tag (128 bits). Any modification to the ciphertext — even a single flipped bit — causes the authentication tag verification to fail during decryption. This prevents an attacker who can write to the database from injecting crafted ciphertexts that decrypt to attacker-chosen values.

**Counter mode (parallelisable, no padding oracle).** Unlike CBC mode, GCM does not require padding and is not susceptible to padding oracle attacks (a class of attacks that have broken many real-world CBC implementations). Each encryption call generates a fresh 12-byte random Initialisation Vector (IV), so two encryptions of the same plaintext always produce different ciphertexts.

The ciphertext storage format is:

```
Base64(salt) : Base64(iv) : Base64(ciphertext || authTag)
```

The `salt` is the PBKDF2 salt used to derive the key for this specific field encryption call. The `iv` is the AES-GCM nonce. Including both with every field means each field is independently encrypted with a fresh key derivation — there is no key reuse across fields.

### 3.5.4 JWT Signing: HMAC-SHA256

JWT tokens are signed using **HMAC-SHA256** (Hash-based Message Authentication Code with SHA-256). HMAC produces a fixed-size authentication code that is computationally infeasible to forge without knowledge of the shared secret:

```
HMAC-SHA256(key, message) = SHA256((key ⊕ opad) || SHA256((key ⊕ ipad) || message))
```

The JWT header and payload are Base64URL-encoded and concatenated, and the HMAC is computed over this string. Any modification to the token (e.g., changing the `role` claim from `USER` to `ADMIN`) invalidates the signature, which every service verifies independently.

The shared-secret approach (as opposed to asymmetric RS256) is a deliberate design choice for this project: it allows every service to verify tokens without calling auth-service, at the cost of requiring all services to share the secret. In a production system, RS256 with a public key distribution mechanism would be preferred, but for a single-tenant deployment with a controlled secret, HMAC-SHA256 is sufficient.

### 3.5.5 Two-Factor Authentication: TOTP Algorithm

Time-based One-Time Passwords are generated and verified according to RFC 6238 [23]. The algorithm extends HOTP (HMAC-based OTP, RFC 4226 [22]) by replacing the counter with a time step:

```
T = floor(current_unix_time / time_step)         // time_step = 30 seconds
TOTP = HOTP(secret, T)
     = Truncate(HMAC-SHA256(secret, T))
     = lower 31 bits of HMAC result, taken mod 10^digits
```

The `Truncate` operation extracts a 4-byte dynamic binary code from the HMAC result using the last byte as an offset index, then takes the low-order 31 bits and applies modular reduction to produce a 6-digit decimal code. The time-step of 30 seconds means codes change every half-minute, and the verifier (`DefaultCodeVerifier`) accepts codes within a ±1 step window to account for clock drift between the client device and the server.

TOTP secrets are stored encrypted with a **service-level AES-256-GCM key** (separate from user-derived PII keys), governed by the `SERVICE_ENCRYPTION_KEY` environment variable. This layered approach means that even compromising the database would not expose TOTP secrets, and even compromising the user-derived key material would not expose TOTP secrets, as long as the service-level key remains protected.

### 3.5.6 Pseudonymisation for GDPR Compliance

Pseudonymisation replaces directly identifying data with pseudonyms (identifiers that can only be re-linked with additional information held separately). In the fraud pipeline, account IBANs are a form of quasi-identifier: an IBAN uniquely identifies a bank account and can be used to link fraud decisions to specific individuals. When fraud decision data is transmitted to external analytical services, IBANs are replaced with their **SHA-256 hashes**:

```
pseudonymIban = hex(SHA-256(iban))
```

SHA-256 is a one-way function: it is computationally infeasible to recover the original IBAN from its hash. The hash is deterministic (the same IBAN always produces the same hash) which allows the external service to correlate decisions involving the same account without knowing the account identity. Internally, the original IBAN is retained for operational purposes.

---

## 3.6 Distributed Caching and Concurrency Control

### 3.6.1 Two-Level Cache Hierarchy

Account-service implements a two-level cache hierarchy to balance read performance against consistency. The first level is **Caffeine**, an in-process Java cache that provides sub-millisecond lookups for frequently accessed entries. The second level is **Redis**, a distributed in-memory data store that serves as a shared cache across multiple service instances. The `CompositeCacheManager` consults Caffeine first; on a miss it queries Redis; on a Redis miss it reads from PostgreSQL and populates both levels.

The TTL values are deliberately asymmetric: Caffeine entries expire after 5 seconds (accepting a small risk of serving a stale balance for at most 5 seconds to achieve very fast in-process reads), while Redis entries expire after 30–60 seconds (providing a longer-lived shared cache). The `exchangeRates` cache uses a 24-hour TTL in Redis, matching the ECB's daily publication schedule.

### 3.6.2 Cache Invalidation via Redis Pub/Sub

A standard problem with distributed caches is invalidation: when a write occurs on one service instance, all other instances' local Caffeine entries must be evicted. This is solved with a **Redis Publish/Subscribe channel** (`cache:account:invalidate`). Whenever a balance or account list changes, `CacheInvalidationPublisher.publish()` serialises a `CacheInvalidationMessage` (cache name + key + reason) to JSON and publishes it on the channel. Every account-service instance subscribes to this channel via `CacheInvalidationSubscriber.handleMessage()` and evicts the corresponding Caffeine entry. This ensures that a transfer processed by instance A causes instances B and C to evict their stale balance entries within milliseconds.

### 3.6.3 Distributed Locking: Redisson and the SETNX Pattern

The account transfer operation requires that no two concurrent transfers involving the same accounts can read-modify-write the balance simultaneously. A database transaction alone is insufficient in this architecture because two requests handled by different JVM instances can each read the current balance before either has written its update, producing a lost-update anomaly.

Distributed locking is implemented using **Redisson**, which implements the standard Redis distributed lock protocol over the SETNX ("SET if Not eXists") command. The acquisition attempt is:

```
SET lock:{key} {unique_value} NX PX {timeout_ms}
```

`NX` makes the set conditional — it only succeeds if the key does not already exist. `PX {timeout_ms}` sets an expiry so that locks are automatically released if the holder crashes before explicitly releasing them (preventing deadlocks from process failures). Release is performed with a Lua script that atomically verifies ownership before deleting the key:

```lua
if redis.call("get", KEYS[1]) == ARGV[1] then
    return redis.call("del", KEYS[1])
else
    return 0
end
```

Locks are acquired on both accounts involved in a transfer, always in **alphabetical IBAN order** to prevent deadlocks. Consider two concurrent transfers: T1 from account A to B, and T2 from account B to A. If T1 acquires the lock on A and T2 acquires the lock on B, both will wait forever. By enforcing alphabetical order, both T1 and T2 first try to acquire the lock on account A, so only one proceeds while the other waits — the classic solution to the dining philosophers' deadlock problem.

---

## 3.7 Fault Tolerance: Circuit Breaking and Fail-Open Design

### 3.7.1 The Circuit Breaker Pattern

The API Gateway uses the **circuit breaker pattern** (popularised by Fowler [10] and implemented here via Resilience4j) to prevent cascade failures in the microservices mesh. A circuit breaker wraps each downstream service call and exists in one of three states:

- **CLOSED**: normal operation; calls pass through and failures are counted.
- **OPEN**: the failure rate over a sliding window has exceeded the threshold (50% in this configuration); calls are short-circuited immediately and a fallback response is returned. The circuit stays open for 10 seconds.
- **HALF-OPEN**: after the open duration expires, a limited number of probe calls (3) are allowed through; if they succeed, the circuit closes; if they fail, it reopens.

This prevents a slow or failing downstream service from accumulating a backlog of blocked threads in the gateway, which could exhaust the thread pool and make the gateway itself unresponsive.

### 3.7.2 Fail-Open Design for Fraud Detection

The fraud service is invoked synchronously by account-service before every transfer. If the fraud service is unavailable (network error, restart), blocking the transfer would degrade the entire platform's availability for the sake of a fraud check. The design choice is **fail-open**: if the fraud service call throws an exception, the transfer is allowed to proceed and the exception is logged as a warning. This is a calculated trade-off — a brief window of unrestricted transfers during a fraud service outage is less harmful than denying all transfers to all users.

The same principle applies to Tier 3 specifically: if the ML model file is not found at startup, the service starts in degraded mode returning ALLOW for all Tier 3 evaluations, while Tiers 1 and 2 continue to operate normally. The circuit breaker fail-open and the ML degraded mode together ensure that no single component failure takes down the transfer capability.

### 3.7.3 Idempotency in Payment Settlement

The Stripe webhook settlement flow must be idempotent because Stripe guarantees *at-least-once* delivery — the same `payment_intent.succeeded` event may arrive more than once. The settlement guard is simple but effective: `PaymentWebhookService.handleWebhookEvent()` checks whether the local `PAYMENT` row is already in `COMPLETED` status before calling account-service. If it is, the method returns immediately. The combination of the status check and the database-level uniqueness constraint on the Stripe PaymentIntent ID makes double-crediting impossible even under concurrent webhook deliveries.

---

## 3.8 Schema-Per-Service Database Architecture

The schema-per-service pattern is the database-level enforcement of the microservices principle that each service owns its data. All six services share a single PostgreSQL instance (a pragmatic choice for single-host deployment) but maintain strict schema isolation: no service's Hibernate session is configured for any schema other than its own, and there are no foreign key constraints between schemas. All cross-service data relationships are maintained at the application layer through REST API calls with appropriate authentication.

This design has several concrete consequences:
- A `Transaction` entity stores `accountId` as a plain `Long`, not a `@ManyToOne Account`. If account-service is down, transaction-service can still serve its queries.
- A `FraudDecision` entity stores `clientId` and `accountId` as plain `Long` values. The fraud analyst can query decisions without any dependency on client-service or account-service availability.
- Schema migrations are independent: client-service can add a column to the `CLIENT` table without requiring any other service to restart or re-migrate.

The tradeoff is that referential integrity is not enforced at the database level — it is the application's responsibility to ensure consistency. This is acceptable because the system is designed around eventual consistency: a client deleted from client-service will eventually have their accounts closed by the GDPR erasure flow, not by a cascade delete constraint.

---

# CHAPTER 4. SYSTEM ARCHITECTURE

## 4.1 Overview

The CashTactics platform is organised around a single public entry point — the API Gateway — that routes requests to six independent backend microservices: auth-service, client-service, account-service, transaction-service, payment-service, and fraud-service. All services share a single PostgreSQL instance but use strictly isolated schemas. A Redis instance serves both the account-service cache layer (Caffeine+Redis hierarchy, Redisson distributed locks, Pub/Sub invalidation) and the gateway's security infrastructure (token blacklist, rate-limit counters).

---

> **[FIGURE 4.1 — System Context Diagram]**
>
> *Instructions: Render `docs/architecture/chapter4/system-context.puml` using PlantUML (e.g., via the PlantUML VS Code extension or online at plantuml.com). The diagram shows two human actors (Bank Customer and Bank Administrator), the CashTactics Platform box as the central system (labelled "API Gateway + 6 backend microservices"), and two external systems (Stripe for payments and the European Central Bank for exchange rates). Arrows show: Users interact with the platform over HTTPS from a browser; the platform calls Stripe for card top-ups and webhook reception; the platform calls the ECB daily for exchange rates in XML format. Caption: "Figure 4.1 — CashTactics system context: actors and external integrations."*

---

## 4.2 Container Architecture

At the container level, the frontend (React SPA served by Vite or Nginx) communicates exclusively with the API Gateway over HTTPS. The gateway routes each request to the appropriate backend service based on the path prefix: `/api/auth/**` to auth-service, `/api/clients/**` and `/api/gdpr/**` to client-service, `/api/accounts/**` to account-service, `/api/transactions/**` to transaction-service, `/api/payments/**` and `/api/payment-methods/**` to payment-service, and `/api/fraud/**` to fraud-service. All backend services persist their data to PostgreSQL using their designated schema. Account-service additionally communicates with Redis for a two-level cache (Caffeine + Redis), Redisson distributed locks on account transfers, and a Redis Pub/Sub channel for cross-instance cache invalidation. The gateway communicates with Redis for token blacklist checks (on every protected request) and rate-limit counters (login: 5 req/60s per IP; global: 50 req/s per IP).

---

> **[FIGURE 4.2 — Container Diagram]**
>
> *Instructions: Render `docs/architecture/chapter4/containter.puml` using PlantUML. The diagram shows: the Frontend container (React + Vite, port 5173), the API Gateway container (Spring Cloud Gateway, port 8443 — labelled with its filter responsibilities: JWT validation, login rate limit 5 req/60s, global rate limit 50 req/s, logout token blacklist, circuit breaker, audit logging), six microservice containers (auth :8081, client :8082, account :8083, transaction :8084, payment :8085, fraud :8086 — fraud-service labelled with its three tiers), the PostgreSQL 16 database container with its six schemas, and the Redis 7 container. Arrows show HTTPS from the frontend to the gateway; gateway routes to each service with path prefixes; JDBC from each service to its schema; Redis connections from the gateway (token blacklist, rate limits) and from account-service (cache, Redisson locks, Pub/Sub). External arrows show payment-service to Stripe and account-service to ECB. Caption: "Figure 4.2 — CashTactics container diagram: services, gateway, Redis, and infrastructure."*

---

## 4.3 Data Zoning Model

One of the most important architectural decisions in the system is how data is partitioned across the schemas. The design follows a three-zone model.

**Centralized Zone — Identity and Authentication**: The `auth` schema stores user credentials (username, BCrypt password hash, role), two-factor authentication secrets (encrypted with a service-level AES key), refresh token hashes, and the encryption salt used to derive the user's PII encryption key. This zone is exclusively owned by auth-service and is never directly queried by any other service.

**Shared Zone — Core Banking Data**: The `accounts`, `transactions`, and `payments` schemas store the operational banking data: account IBANs, balances, transaction records, and Stripe payment history. This data is not personally identifiable in isolation — it contains client IDs and account IDs but not names or contact details. It is the zone that admin dashboards can query without privacy concerns.

**Private Zone — Encrypted PII and Fraud Intelligence**: The `clients` schema stores first names, last names, and all contact information, all encrypted with AES-256-GCM using a key derived from the user's own password via PBKDF2. Plain-text names are never written to this schema — even the client's first name in the database is a structured ciphertext string (`Base64(salt):Base64(iv):Base64(ciphertext)`). The `fraud` schema stores fraud decisions (`FRAUD_DECISION` table), behavioural profiles (`USER_BEHAVIOR_PROFILE` table), risk scores, tier decision labels (`TIER1_RULES`, `TIER2_BEHAVIORAL`, `TIER3_ML`), and human-readable explanations generated by the perturbation analyser. IBANs in the fraud schema are pseudonymised with SHA-256 for analytical purposes. These records are sensitive enough to warrant isolation from the general banking data.

---

> **[FIGURE 4.3 — Data Zoning Model]**
>
> *Instructions: Render `docs/architecture/chapter4/data-zoning.puml` using PlantUML. The diagram shows three coloured zones: Centralized Zone (pink/red) containing the auth schema (USERS, REFRESH\_TOKENS) with fields password\_hash BCrypt, encryption\_salt Base64, two\_factor\_secret AES-encrypted with service key, role ENUM; Shared Zone (blue) containing the accounts schema (ACCOUNTS, SENSITIVE\_DATA\_REVEAL\_AUDIT), transactions schema (TRANSACTIONS, VIEW\_TRANSACTION), and payments schema (PAYMENTS, PAYMENT\_METHODS, STRIPE\_CUSTOMERS); Private Zone (green) containing the clients schema with encrypted field annotations (first\_name AES-256-GCM, last\_name AES-256-GCM, email AES-256-GCM, key derived from user password via PBKDF2) and the fraud schema with fields status, decided\_by\_tier, explanation, IBANs pseudonymised (SHA-256). A note states "Cross-schema JOINs are impossible. IDs are logical references. Data correlation requires REST API + authentication." Caption: "Figure 4.3 — CashTactics data zoning model: three security zones with a single PostgreSQL instance."*

---

## 4.4 Inter-Service Communication

Services communicate through synchronous REST calls. There are two authentication patterns in use:

**JWT forwarding**: When a user-initiated request arrives at the gateway, the JWT is validated at the gateway level and then forwarded to the downstream service as-is. The downstream service validates the token again and extracts the `JwtPrincipal` for ownership checks. This pattern is used when the call involves user data and the downstream service needs to verify that the user is allowed to access it (e.g., account-service calls transaction-service to create transaction records, forwarding the user's JWT so that transaction-service can apply its own security rules).

**Internal shared secret**: Certain inter-service calls happen in a service-initiated context without a user session, such as when account-service calls transaction-service to record a Stripe top-up deposit, or when client-service calls auth-service to verify a TOTP code for a step-up operation. These calls authenticate using a shared `X-Internal-Api-Secret` header. Each service that exposes internal endpoints has an `InternalApiAuthFilter` (or equivalent header check) that validates this secret before allowing access.

---

> **[FIGURE 4.4 — Inter-Service Communication Map]**
>
> *Instructions: Render `docs/architecture/chapter4/inter-service-communication.puml` using PlantUML. The diagram shows all six services and the directional REST calls between them, labelled with the authentication pattern used (JWT forwarded or X-Internal-Api-Secret). Key arrows: account-service → transaction-service (create debit/credit records, JWT forwarded); account-service → fraud-service (fraud evaluation synchronous — Tier 1 rules + Tier 2 behavioural scoring — internal secret); fraud-service → transaction-service (fetch transaction history for Tier 2 scoring, synchronous, internal secret); fraud-service → account-service (freeze/unfreeze account on fraud alert resolution, internal secret); payment-service → account-service (apply Stripe top-up credit, internal secret); auth-service → client-service (re-encrypt PII on password change, internal secret); account-service → auth-service (verify step-up TOTP, internal secret); client-service → auth-service (verify step-up TOTP, internal secret); client-service → auth-service, account-service, transaction-service (GDPR erasure cascade, internal secret). Caption: "Figure 4.4 — Inter-service communication: REST calls with JWT forwarding and shared-secret authentication."*

---

## 4.5 API Gateway Request Lifecycle

Every inbound request passes through a filter chain before reaching a backend service. The filters run in strict priority order (Spring's `Ordered` interface, lower number = higher priority):

1. **Logout Blacklist Filter** (order −30): For `POST /api/auth/logout` only. Extracts the access token from the `Authorization` header or the request body (`accessToken` JSON field), records it in Redis with a TTL equal to the token's remaining lifetime, then forwards the request to auth-service. Subsequent requests using that token are rejected by the JWT Auth Filter as blacklisted.
2. **Login Rate Limit Filter** (order −20): For `POST /api/auth/login` only. Limits each client IP to five requests per 60 seconds using a Redis counter (`gateway:ratelimit:login:ip:{ip}`). Exceeding the limit returns `429 Too Many Requests`. This prevents brute-force password attacks.
3. **Global Rate Limit Filter** (order −10): For all paths except `/api/auth/login`. Limits each client IP to 50 requests per second using a Redis counter (`gateway:ratelimit:global:ip:{ip}`). `OPTIONS` preflight requests are excluded. Exceeding the limit returns `429 Too Many Requests`.
4. **Audit Logging Filter** (order −1): Logs every request after it completes, recording timestamp, authenticated user identity (extracted from the JWT subject, role, and clientId claims), HTTP method, path, response status code, and source IP. Additionally, if a `USER`-role token receives a `403` on the admin client list endpoint, a persistent audit event (`ADMIN_DATA_ACCESS_ATTEMPT`) is recorded.
5. **JWT Auth Filter** (route-level, applied per-route in `GatewayConfig`): For each protected route, extracts the `Bearer` token from the `Authorization` header, checks whether it is present in the Redis blacklist, and validates the HMAC-SHA256 signature and expiry. Returns `401 Unauthorized` if the token is missing, blacklisted, or invalid. This filter is **not** applied to public routes: `/api/auth/**`, `/api/clients/sign-up`, `/api/payments/webhook`, and `/api/fraud/health`.
6. **Route matching and circuit breaker**: Matches the path to a backend service and forwards the request. Each route is wrapped in a Resilience4j circuit breaker (threshold: 50% failure rate over a sliding window, open duration: 10 seconds, 3 probe calls in half-open state) with a fallback URI pointing to the `FallbackController`, which returns `503 Service Unavailable`.

CORS is configured as a reactive `CorsConfig` bean (not a `GlobalFilter`), so it applies at the Spring WebFlux level before any of the above filters execute.

---

> **[FIGURE 4.5 — API Gateway Request Lifecycle]**
>
> *Instructions: Render `docs/architecture/appendix/api-gateway-request-lifecycle.puml` using PlantUML. The diagram is a sequence diagram showing an incoming request from the Frontend through the ordered filter chain in execution order: Logout Blacklist Filter (−30) → Login Rate Limit Filter (−20) → Global Rate Limit Filter (−10) → Audit Logging Filter (−1) → JWT Auth Filter (route-level) → Circuit Breaker + Router → Target Microservice. Show the conditional branches for each filter: logout path (token extracted and blacklisted in Redis), login rate limit exceeded (429), global rate limit exceeded (429), JWT invalid or blacklisted (401), and circuit breaker open (503 from FallbackController). The happy path shows all checks passing, the service responding, and the audit filter logging the response status. Caption: "Figure 4.5 — API Gateway request lifecycle: filter chain execution order and circuit breaker."*

---

## 4.6 Transfer Flow

The account transfer flow illustrates how multiple services cooperate for a single user operation:

1. The user initiates a transfer from the React frontend, providing source IBAN, destination IBAN, amount, and optionally a TOTP code for step-up authentication.
2. The request hits the gateway (with JWT validation and blacklist check), which forwards it to account-service.
3. Account-service acquires Redisson distributed locks on both accounts in alphabetical IBAN order (to prevent deadlocks between concurrent transfers involving the same pair), then validates ownership (the principal's `clientId` must match the source account's `clientId`) and checks for sufficient balance.
4. Account-service calls fraud-service synchronously via `POST /api/internal/fraud/evaluate` (authenticated with `X-Internal-Api-Secret`). **Fraud-service runs Tier 1 (rule engine) and Tier 2 (behavioural scoring) both synchronously** within the same evaluate call, in sequence:
   - **Tier 1** applies three deterministic rules: `LARGE_AMOUNT` (> 10,000), `BURST` (≥ 5 evaluations per 60 seconds for the same account), and `NEW_ACCOUNT_HIGH_AMOUNT` (account < 30 days old with amount > 2,000). Self-transfers are auto-allowed. Tier 1 returns either `ALLOW` or `STEP_UP_REQUIRED`.
   - **Tier 2** (runs only if Tier 1 returned `ALLOW` or `MANUAL_REVIEW`) fetches recent transaction history from transaction-service, recomputes the user's `UserBehaviorProfile`, and computes a composite behavioural risk score from six weighted components (amount anomaly 30%, frequency 20%, time-of-day 15%, recipient novelty 15%, category risk 10%, 24-hour velocity 10%). Score < 50 → `ALLOW`; 50–70 → `STEP_UP_REQUIRED`; ≥ 70 → `FLAG`.
5. Account-service receives the fraud decision. If `STEP_UP_REQUIRED` from fraud, **or** if the transfer amount meets or exceeds the large-transfer threshold (default 1,000), account-service calls auth-service's internal step-up endpoint to verify the TOTP code. If the code is absent or invalid, it returns `428 Precondition Required` immediately. A `FLAG` result allows the transfer to proceed but records the decision for admin review.
6. Account-service applies the ECB exchange rate if the source and destination currencies differ, updates both balances in a single `@Transactional` database operation, and publishes cache invalidation messages to the `cache:account:invalidate` Redis Pub/Sub channel so all service instances evict stale Caffeine entries.
7. Account-service calls transaction-service twice (forwarding the user's JWT) to create the debit record for the source account and the credit record for the destination account.
8. Locks are released in the `finally` block.
9. Asynchronously (in the `fraudAsyncExecutor` thread pool via `@Async`), **only when the final synchronous decision was `ALLOW`**, fraud-service runs Tier 3 — the local Isolation Forest ML model (`Tier3MlService.analyze()`). If the model flags the transaction post-hoc, it updates the `FRAUD_DECISION` record to `FLAG` with `decided_by_tier = TIER3_ML` and a perturbation-analysis-derived explanation. This post-hoc update does not affect the transfer, which has already completed.

---

> **[FIGURE 4.6 — Transfer Flow Sequence Diagram]**
>
> *Instructions: Render `docs/architecture/chapter4/transfer-flow.puml` using PlantUML. This is a sequence diagram with participants: User (browser), Frontend (React), API Gateway, Account Service, Auth Service, Fraud Service, Transaction Service. Show in sequence: lock acquisition, ownership and balance validation, synchronous Tier 1 rule engine (ALLOW or STEP\_UP\_REQUIRED), synchronous Tier 2 behavioural scoring with a transaction history fetch from Transaction Service (ALLOW, STEP\_UP\_REQUIRED, or FLAG), step-up TOTP verification branch (when required), ECB exchange rate conversion, balance update with cache invalidation, two transaction record creation calls to Transaction Service, lock release, and a closing note indicating that Tier 3 ML runs asynchronously only when the final status was ALLOW (see Figure 4.7). Caption: "Figure 4.6 — Transfer flow: synchronous Tier 1 + Tier 2 fraud check, step-up authentication, balance update, and transaction persistence."*

---

## 4.7 Asynchronous Tier 3 ML Analysis

When the synchronous fraud pipeline (Tier 1 + Tier 2) returns `ALLOW`, fraud-service schedules a post-hoc Isolation Forest analysis to run asynchronously in the `fraudAsyncExecutor` thread pool via `@Async`. Because this happens after account-service has already received the synchronous response, the ML analysis does not add any latency to the transfer — the user sees the result immediately.

The Tier 3 analysis proceeds as follows:

1. `Tier3MlService.analyze()` checks whether the `ModelSnapshot` was successfully loaded at startup. If the model binary file is not present (e.g., the offline training CLI has not been run), the service operates in degraded mode and returns `ALLOW` for all transactions.
2. `FeatureVectorBuilder.build()` constructs a six-dimensional feature vector from the `FraudEvaluationRequest` using the same feature formulas as the training pipeline (`PaySimFeatureMapper`), ensuring zero train-serve skew.
3. The raw feature values are scaled using the per-feature `min` and `max` arrays saved in the `ModelSnapshot` at training time.
4. The scaled vector is passed to `IsolationForest.score()`, which returns an anomaly score in the range [0, 1]. Higher values indicate higher isolation depth (more anomalous).
5. The score is compared to the calibrated threshold stored in the `ModelSnapshot` (determined offline by maximising F-beta with β = 0.5).
6. If the score exceeds the threshold, `PerturbationAnalyzer.computeFeatureImportances()` replaces each feature one at a time with its training-set mean and measures how much the anomaly score changes. The feature with the largest absolute change is the primary driver. `ReasoningBuilder` converts the importance ranking into a human-readable explanation.
7. `Tier2AsyncRunner` updates the `FRAUD_DECISION` record: `status = FLAG`, `decided_by_tier = TIER3_ML`, `risk_score = anomalyScore × 100`, `explanation = "Tier3-ML post-hoc FLAG: …"`. The admin dashboard will surface this as an unresolved alert.

---

> **[FIGURE 4.7 — Async Tier 3 ML Analysis]**
>
> *Instructions: Render `docs/architecture/chapter5/transfer-flow-async-fraud.puml` using PlantUML. This is a sequence diagram showing the background thread (fraudAsyncExecutor) running inside Fraud Service only, with Fraud DB as the second participant. Show: feature vector construction (6 features listed), MinMax scaling, IsolationForest.score(), the conditional branch (anomaly score above/below threshold), and on the flagged path: perturbation analysis, reasoning builder, and DB update (status=FLAG, decided\_by\_tier=TIER3\_ML, explanation). On the non-flagged path: no DB update. Include a note that the transfer has already completed before this runs. Caption: "Figure 4.7 — Post-hoc asynchronous Tier 3 Isolation Forest analysis: non-blocking ML flagging."*

---

## 4.7 Stripe Top-Up Flow

The card top-up flow follows the PCI-DSS principle that card data should never touch the merchant's servers:

1. The user enters an amount in the React frontend and clicks "Top up".
2. The frontend calls `POST /api/payments/top-up/intent` with `accountId` and `amount`.
3. Payment-service fetches the target account from account-service (to get the currency), creates a local `PAYMENT` row with status `PENDING`, creates a Stripe `PaymentIntent`, and returns the `clientSecret` to the frontend.
4. The frontend uses Stripe.js's `confirmCardPayment(clientSecret, { card: CardElement })` to submit the card details. This call goes directly to Stripe's API — card data never touches CashTactics servers.
5. Stripe processes the payment and asynchronously sends a `payment_intent.succeeded` webhook event to `/api/payments/webhook`. The gateway forwards this request without JWT validation (the webhook uses Stripe's own signature verification instead).
6. Payment-service verifies the webhook signature using the Stripe webhook secret, updates the `PAYMENT` row to `COMPLETED`, and calls account-service's internal top-up endpoint.
7. Account-service credits the account balance and calls transaction-service to create a `DEPOSIT` ledger record.

---

# CHAPTER 5. IMPLEMENTATION

## 5.1 auth-service

### 5.1.1 Overview

Auth-service is the identity authority for the entire platform. It is the only service that generates JWTs; all other services only validate them. It runs on port 8081 with the `auth` PostgreSQL schema and a Redis connection for token blacklisting (via the Redis template) and health monitoring.

### 5.1.2 Registration and Key Derivation

The `AuthService.register()` method creates a new `User` entity with a BCrypt-hashed password and a freshly generated 24-byte random salt stored in Base64. The encryption key for the new user's PII is immediately derived as:

```
encryptionKey = PBKDF2WithHmacSHA256(password, salt, iterations=65536, keyLength=256)
```

Auth-service then calls client-service's `/api/internal/clients/migrate-legacy` endpoint to re-encrypt any data that was previously encrypted with the server-side fallback key (which is used at sign-up before the user has set a password) with the newly derived user key.

### 5.1.3 Login and Token Generation

`AuthService.login()` verifies the password with BCrypt, derives the encryption key from the submitted password and the stored salt, and creates two tokens:

- **Access token**: a JWT with a 60-minute expiry containing `role`, `clientId`, `2fa` = `"ok"`, and `ek` (the derived encryption key) in the claims. The `ek` claim allows client-service to decrypt the user's PII on subsequent requests without a database round-trip.
- **Refresh token**: a JWT with a 7-day expiry, whose SHA-256 hash is stored in the `REFRESH_TOKENS` table. Only the hash is stored — the raw token is returned to the client once and never persisted. This means that even if the database is compromised, an attacker cannot extract valid refresh tokens from it.

If the user has 2FA enabled, the login flow returns a **temporary token** with `2fa` = `"pending"` instead of a full access token. The frontend must then call `POST /api/auth/2fa/verify` with this temp token and a valid TOTP code to receive the full access token.

### 5.1.4 Refresh Token Rotation

`TokenService.refreshToken()` validates the refresh token's JWT signature, looks up the hash in the database, checks that it is neither expired nor revoked, generates a new access token, revokes the old refresh token by setting `revokedAt`, and creates a new refresh token. This rotation strategy means a stolen refresh token can only be used once — the next legitimate refresh attempt will fail with `401`.

The `ek` claim is preserved across token refreshes: `TokenService` reads it from the (potentially expired) access token that the client submits alongside the refresh token, and copies it into the new access token.

### 5.1.5 Two-Factor Authentication

`TwoFaService.setup2fa()` generates a TOTP secret using `DefaultSecretGenerator`, encrypts it with `ServiceEncryptionService` (AES-256-GCM with a service-level key from the `SERVICE_ENCRYPTION_KEY` environment variable), and stores the encrypted value. The plaintext secret is returned to the frontend inside an OTPAuth URI that encodes the issuer, label, and algorithm parameters for QR code display.

`TwoFaService.verifyStepUp()` is called by account-service and client-service when a high-risk operation requires step-up authentication. It retrieves the user's encrypted TOTP secret, decrypts it, and validates the submitted TOTP code.

### 5.1.6 Password Change and Re-encryption

`AuthService.changePassword()` verifies the current password, derives old and new encryption keys, calls client-service to re-encrypt all PII, updates the password hash and salt, and revokes all existing refresh tokens. If the client-service call fails (e.g., a network error), the entire operation is rolled back — the password is not changed, ensuring the encryption key and password always remain consistent.

### 5.1.7 Rate Limiting

Auth-service has its own in-process rate limiter (`RateLimitService`) that tracks failed login attempts per IP using a `ConcurrentHashMap`. After five failed attempts, subsequent attempts are rejected. The map is cleared every 60 seconds by a scheduled task. This local rate limiter complements the gateway-level rate limiter.

### 5.1.8 GDPR

`AuthGdprInternalService.deactivateUserForGdpr()` is called by client-service during the GDPR erasure flow. It revokes all active refresh tokens, sets `enabled = false` on the user (preventing any future logins), clears the TOTP secret, and disables 2FA. The user cannot log in after this operation.

## 5.2 client-service

### 5.2.1 Overview

Client-service manages the personal profile of each banking client. It is the only service that holds PII (names and contact details) and is the only place where PII is encrypted and decrypted. It runs on port 8082 with the `clients` schema.

### 5.2.2 Client Profile Lifecycle

`ClientProfileService.createClient()` encrypts the client's first and last name using the provided encryption key (or the fallback server-side key if no user key is available, as is the case during the public sign-up flow) before persisting. The `searchByName()` method decrypts results before returning them — search is currently performed against the database using a `LIKE` query on the encrypted column (which means it finds all rows and decrypts on the Java side, not a limitation for prototype scale but a known scalability concern).

### 5.2.3 PII Encryption Architecture

`EncryptionService` implements AES-256-GCM. The `encrypt()` method generates a random 16-byte salt and 12-byte IV for every encryption call (so two encryptions of the same plaintext with the same key produce different ciphertexts). The ciphertext format is `Base64(salt):Base64(iv):Base64(ciphertext)`.

`decryptFlexible()` tries the primary key (user-derived, from the JWT's `ek` claim) first. If that fails, it falls back to the legacy server-side key. This two-key fallback supports the key migration period after a user first logs in and their data has been re-encrypted with their user-derived key.

`ClientKeyResolver` abstracts key selection. It can work with keys provided directly as strings (user-derived keys from the JWT) or with keys from `KeyManagementProvider` implementations. Two implementations are provided: `EnvKeyManagementProvider` (reads from environment variables, default) and `KmsKeyManagementProvider` (a stub that throws if no KMS client is implemented, marked with `@ConditionalOnProperty(name="app.security.key-management.mode", havingValue="kms")`).

### 5.2.4 Contact Information Management with Step-Up

`ClientContactService.updateClientContactInfo()` requires a valid TOTP code (`X-TOTP-Code` request header) before updating contact information. The step-up is enforced via `AuthStepUpClient`, which calls auth-service's internal step-up endpoint. If 2FA is not enabled, the service returns `428 Precondition Required`. This design prevents an attacker who steals an access token from silently updating a victim's phone number or email.

### 5.2.5 Admin View with Data Masking

`ClientViewProjectionService.getAllViewClients()` returns a list of `ViewClientDTO` objects populated from the `VIEW_CLIENT` database view. The admin list endpoint deliberately omits PII (first name, last name, email, phone, address, city, postal code are all `null` in the DTO) and returns only operational fields (client type, risk level, active status, created date). This implements GDPR data minimisation — administrators can see that a client exists and their risk profile without seeing who they are.

To see PII, administrators must use the reveal flow: `POST /api/accounts/audit/reveal` in account-service logs a reveal event with the actor's identity, the scope, the target, and a mandatory business reason code (`DISPUTE_INVESTIGATION`, `FRAUD_REVIEW`, `REGULATORY_AUDIT`, `SUPPORT_REQUEST`, `RECONCILIATION`, or `OTHER`). For `OTHER`, a minimum 8-character description is required. These events are persisted in `SENSITIVE_DATA_REVEAL_AUDIT` and can be queried by reason code.

### 5.2.6 Encryption Key Migration

`ClientEncryptionLifecycleService.migrateLegacyEncryption()` is called after every login (by auth-service). It attempts to decrypt the client's first name with the user-derived key. If decryption succeeds, the data is already in the correct state and no action is taken. If it fails, the service tries the active fallback key and then the previous fallback key (for rotation scenarios). If a legacy key works, the service re-encrypts all PII fields (first name, last name, and all contact info fields) with the user-derived key. After migration, the server-side key can no longer decrypt the client's data — it is locked to the user's password.

### 5.2.7 GDPR Erasure Orchestration

`ClientGdprService.performRightToErasure()` orchestrates the full GDPR erasure flow across four services using `GdprDownstreamRestClient`:

1. Calls auth-service to deactivate the user (revoke tokens, disable login, erase TOTP).
2. Calls account-service to retrieve the list of account IDs for the client.
3. Calls transaction-service to anonymise the `details` field of all transactions for those accounts (replacing merchant names and transfer notes with "ANONYMIZED").
4. Calls account-service to close all accounts.
5. Locally: deletes all contact info records, overwrites first name and last name with `encrypt("DELETED", serverKey)`, and sets the client as inactive.

## 5.3 account-service

### 5.3.1 Overview

Account-service is the financial core of the platform. It manages account lifecycles, account balances, multi-currency transfers, exchange rate fetching, Stripe top-up credit application, and a two-level cache. It runs on port 8083 with the `accounts` schema, a Redis connection, and a Redisson connection for distributed locking.

### 5.3.2 Account Lifecycle

`AccountLifecycleService` handles opening (IBAN generation using ISO 13616 check digits, starting balance zero), closing (requires zero balance), freezing (sets status to `SUSPENDED`), and unfreezing (sets status back to `ACTIVE`). The `IbanService` generates random IBANs with valid check digits and checks uniqueness against the database before committing.

### 5.3.3 Transfer Service and Distributed Locking

`AccountTransferService.transfer()` is the most complex method in the system. Its steps are:

1. Normalise and validate IBANs.
2. Acquire Redisson distributed locks on both accounts, always in alphabetical IBAN order (alphabetical ordering prevents deadlocks when two transfers involving the same pair of accounts run concurrently).
3. Load both accounts and verify ownership.
4. Call fraud-service synchronously.
5. If the fraud decision is `STEP_UP_REQUIRED`, or if the transfer amount exceeds the `large-transfer-threshold`, call auth-service to verify the TOTP code.
6. Apply exchange rate conversion if the accounts have different currencies.
7. Update balances and save.
8. Publish Redis cache invalidation events for the affected cache keys.
9. Create transaction records in transaction-service.
10. Release locks in the finally block.

The method is annotated with `@Transactional` and `@Caching(evict = ...)` to ensure that both the database and the cache remain consistent on success, and that the database rolls back on failure.

### 5.3.4 Exchange Rate Service

`ExchangeRateService.getRate()` fetches the day's exchange rates from the European Central Bank's daily XML feed (`https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml`) using Spring's `RestClient`. The rates are cached in Redis for 24 hours with the key pattern `exchangeRates::{from}:{to}`. The cache is invalidated by calling `clearExchangeRatesCache()`, though in practice the 24-hour TTL is sufficient for daily rates.

### 5.3.5 Redis Caching Architecture

The `CacheConfig` bean sets up a `CompositeCacheManager` that consults Caffeine first, then Redis. The caches are:

- `accountsByClient` (Caffeine TTL: 5 seconds, Redis TTL: 60 seconds) — lists of accounts per client.
- `accountDetails` (Caffeine TTL: 5 seconds, Redis TTL: 30 seconds) — individual account DTOs by ID or IBAN.
- `balance` (Caffeine TTL: 5 seconds, Redis TTL: 30 seconds) — balance snapshots by IBAN.
- `exchangeRates` (Redis TTL: 24 hours) — currency pair exchange rates.

The `CacheInvalidationPublisher` sends messages to the `cache:account:invalidate` Redis Pub/Sub channel whenever a write occurs. The `CacheInvalidationSubscriber` in every account-service instance listens to this channel and evicts the corresponding Caffeine entries. This ensures that all instances see consistent data even when they each maintain their own in-process Caffeine caches.

### 5.3.6 Stripe Top-Up Application

`AccountTopUpService.applyStripeTopUpCredit()` is called by payment-service (via the internal `InternalStripeTopUpController`) after a Stripe webhook confirms a successful card payment. It loads the target account, verifies the currency matches the payment currency, adds the amount to the balance, publishes cache invalidation events, and calls transaction-service to create a `DEPOSIT` record. The `Caching(evict = ...)` annotation ensures the cache is updated after the balance change.

### 5.3.7 Fraud Decision Integration and Step-Up

Account-service calls fraud-service's internal `/api/internal/fraud/evaluate` endpoint via `RestTemplate` (not forwarding the JWT — using the internal secret). The request body includes account ID, client ID, amount, currency, sender IBAN, receiver IBAN, transaction type, `selfTransfer` flag (true if both accounts belong to the same client), and `accountAgeDays`. If the fraud service is unavailable, the method catches the exception and fails open (allows the transfer), logging a warning. This is a deliberate fail-open choice for Tier 3 availability.

## 5.4 transaction-service

### 5.4.1 Overview

Transaction-service is a ledger. It persists transaction records and provides querying capabilities. It does not hold account balances — it just records what happened. It runs on port 8084 with the `transactions` schema.

### 5.4.2 Transaction Persistence

Transactions are created through two paths:

- **Internal path** (`InternalTransactionController`): used by account-service for transfer debit/credit records and Stripe deposit records. Protected by `X-Internal-Api-Secret`. Accepts a `TransactionDTO` and creates a `Transaction` entity.
- **Public path** (`TransactionController`): used for manual transaction creation (e.g., deposits or withdrawals initiated directly through the API). Protected by JWT.

Each `Transaction` has an `accountId`, a `destinationAccountId` (for transfers), a `TransactionType` enum (`DEPOSIT`, `WITHDRAWAL`, `TRANSFER_INTERNAL`, `TRANSFER_EXTERNAL`), a `TransactionCategory` enum (with a `baseRiskScore` attribute), an `amount`, an `originalAmount` and `originalCurrencyCode` (before conversion), a `sign` (`+` or `-`), a `merchant`, `details`, `riskScore`, and `flagged` flag.

### 5.4.3 Transaction Queries

`TransactionQueryService` provides querying by account ID, multiple account IDs, date range, type, and client (the client query uses `TransactionOrchestrationService`, which first calls account-service to get the list of account IDs for the client, then loads all transactions for those accounts). The `ViewTransaction` entity maps to a read-only database view (`VIEW_TRANSACTION`) that exposes the same fields without a cross-schema JOIN.

### 5.4.4 GDPR Anonymisation

The `GDPR_ANONYMISE` endpoint (`POST /api/internal/transactions/gdpr/anonymize-details`) accepts a list of account IDs and replaces the `details` field of all matching transactions with `"ANONYMIZED"`. This removes merchant names, IBAN references, and other narrative information from the transaction history while preserving the financial record (amounts, types, timestamps).

## 5.5 payment-service

### 5.5.1 Overview

Payment-service handles all interaction with the Stripe API. It is the only service with a Stripe SDK dependency. It runs on port 8085 with the `payments` schema.

### 5.5.2 Payment Intent Flow (Card Top-Up)

`PaymentCreationService.createTopUpIntent()` implements the top-up intent flow:

1. Fetches the target account from account-service (forwarding the user JWT) to verify it exists, is active, and has a supported currency (currently EUR or RON only — this is a product decision driven by Stripe's currency support in the test environment).
2. Verifies ownership using `OwnershipChecker`.
3. Creates a local `PAYMENT` row with status `PENDING`.
4. Creates a Stripe `PaymentIntent` with `AutomaticPaymentMethods` enabled, attaches the Stripe Customer ID (created on demand per client via `StripeCustomerService`), and sets metadata tags (`internalPaymentId`, `accountId`, `clientId`, `topUp = true`).
5. Returns the `clientSecret` to the frontend.

### 5.5.3 Webhook Settlement

`WebhookController.handleWebhook()` receives Stripe webhook events, verifies the `Stripe-Signature` header using `Webhook.constructEvent()`, and delegates to `PaymentWebhookService.handleWebhookEvent()`. On `payment_intent.succeeded`, the service calls `settleSucceededIntent()`, which calls `PaymentCreditService.applyCreditViaAccountService()` to credit the account. The settlement is idempotent: if the payment is already `COMPLETED`, the method returns immediately. This ensures that Stripe's guaranteed-at-least-once delivery of webhooks does not cause double-crediting.

### 5.5.4 Payment Methods (Saved Cards)

`PaymentMethodAttachmentService.attachPaymentMethod()` retrieves a PaymentMethod from Stripe by its ID, attaches it to the Stripe Customer, extracts card metadata (brand, last four digits, expiry), and persists the payment method locally. The first payment method added for a client is automatically set as the default. The `deletePaymentMethod()` operation also calls Stripe to detach the payment method so it cannot be reused.

### 5.5.5 Refunds

`PaymentRefundService.refundPayment()` creates a Stripe Refund using the PaymentIntent ID and updates the local payment status to `REFUNDED`. Only payments with status `COMPLETED` can be refunded.

## 5.6 fraud-service

### 5.6.1 Overview

Fraud-service is the most technically elaborate service in the system. It implements a three-tier fraud detection pipeline and a complete alert management workflow. It runs on port 8086 with the `fraud` schema.

### 5.6.2 Evaluation Orchestration

`FraudService.evaluate()` is called synchronously by account-service for every transfer. It coordinates the three tiers:

1. Runs Tier 1 (rule engine) synchronously.
2. If the Tier 1 result is `ALLOW` or `MANUAL_REVIEW`, runs Tier 2 (behavioural scoring) synchronously.
3. Tier 2 can escalate the decision to `STEP_UP_REQUIRED` or `FLAG`.
4. If the final synchronous decision is `ALLOW`, schedules Tier 3 (ML model) to run asynchronously in the `fraudAsyncExecutor` thread pool.
5. Saves the fraud decision and returns the final status to account-service.

Self-transfers (same client) are automatically allowed by Tier 1 without further analysis.

### 5.6.3 Tier 1: Rule Engine

`RuleEngine.evaluate()` applies three deterministic rules:

- **LARGE\_AMOUNT**: If the transaction amount exceeds the configured threshold (default 10,000), a risk score of 70 + (excess / 1,000) is assigned, capped at 100. Returns `STEP_UP_REQUIRED`.
- **BURST**: If more than the configured burst limit (default 5) fraud evaluations have been created for the same account in the last 60 seconds, a risk score of 90 is assigned. Returns `STEP_UP_REQUIRED`.
- **NEW\_ACCOUNT\_HIGH\_AMOUNT**: If the account is less than `newAccountAgeDays` old (default 30) and the amount exceeds 2,000, a risk score of 75 is assigned. Returns `STEP_UP_REQUIRED`.

If no rules trigger, the result is `ALLOW` with score 0. These thresholds are all configurable through `FraudProperties.Tier1`.

### 5.6.4 Tier 2: Behavioural Scoring

`BehavioralScoringService.score()` computes a weighted composite score from six components, each on a 0–100 scale:

| Component | Weight | Description |
|---|---|---|
| Amount anomaly | 30% | Z-score of amount vs user's average and std dev |
| Frequency anomaly | 20% | Ratio of today's transactions to the user's daily average |
| Time anomaly | 15% | Distance from the user's typical transaction hours |
| Recipient anomaly | 15% | Whether the recipient IBAN appears in previous transactions |
| Category risk | 10% | Base risk by transaction type (EXTERNAL > INTERNAL > DEPOSIT) |
| 24-hour velocity | 10% | 24-hour outgoing total vs. 30-day daily average |

The composite score is then mapped to a status: below 30 is `ALLOW`, between 30 and 50 is also `ALLOW` (handled by Tier 2 passing it to Tier 3 async), between 50 and 70 is `STEP_UP_REQUIRED`, and 70 or above is `FLAG`.

`BehaviorProfileService.recompute()` is called before scoring to update the user's `UserBehaviorProfile` entity with fresh statistics from the transaction history fetched from transaction-service.

### 5.6.5 Tier 3: Isolation Forest Machine Learning

The Tier 3 model is an Isolation Forest trained on the PaySim financial transaction simulation dataset. The feature vector has six dimensions, all aligned between training (PaySim mapping via `PaySimFeatureMapper`) and inference (live data via `FeatureVectorBuilder`):

| Index | Feature | Description |
|---|---|---|
| 0 | `amountRatio` | Amount / legal cap (50,000 RON), capped at 1.0 |
| 1 | `typeRisk` | Transaction type risk (0.0 for POS, 1.0 for internal transfer, 3.0 for external/instant) |
| 2 | `hourSuspicion` | Time-of-day risk (3.0 for 01:00–05:00, 2.0 for late night/early morning, 1.0 for day) |
| 3 | `newAccountFlag` | 1.0 if account is less than 30 days old, 0.0 otherwise |
| 4 | `senderDepletionRatio` | amount / oldBalance, capped at 1.0 |
| 5 | `isRoundAmount` | 1.0 if the amount is a multiple of 100 (with floating-point epsilon tolerance) |

The same six features are extracted from PaySim rows during training and from live `FraudEvaluationRequest` objects during inference. This alignment is critical: a train-serve skew would cause the model to produce meaningless scores on live data.

**Training process** (`ModelTrainerCli`): The trainer reads up to 150,000 rows from the PaySim CSV, applies `PaySimFeatureMapper`, shuffles with a fixed seed for reproducibility, performs a stratified 80/20 train-test split (preserving the fraud rate in both sets), fits MinMax scalers on the training data (saving `min` and `max` per feature in the model snapshot), trains an Isolation Forest with a contamination parameter set dynamically to the actual fraud rate in the training sample (approximately 12% for PaySim), calibrates the decision threshold by maximising F-beta (beta=0.5, favouring precision over recall — appropriate for banking where false positives annoy users), evaluates on the test set (logging Precision, Recall, F1, F0.5, and AUC-ROC), and serialises the `ModelSnapshot` to a binary file using Java object serialisation. The snapshot includes the model, threshold, feature means (for perturbation analysis), feature min/max (for MinMax scaling at inference), model version string, training timestamp, and training dataset statistics.

**Inference process** (`Tier3MlService.analyze()`): At startup, the service attempts to load the model snapshot from the configured path. If the file is not found, the service starts in degraded mode (all transactions ALLOW, a warning is logged). During inference, `FeatureVectorBuilder.build()` computes the six raw feature values, applies the saved MinMax scaling using `MlUtils.minMaxScaleSingle()`, and passes the scaled vector to `IsolationForest.score()`. The anomaly score is compared to the calibrated threshold. If flagged, `PerturbationAnalyzer.computeFeatureImportances()` is used to estimate which features contributed most to the anomaly score by measuring how much the score drops when each feature is replaced with its training mean. `ReasoningBuilder` turns these importances into a human-readable explanation.

**Model observability**: A custom Spring Boot Actuator endpoint (`/actuator/fraud-model`) exposes model metadata: status (ready/disabled/model\_not\_found), model version, threshold, training timestamp, dataset description, and a drift warning if the model is older than 90 days.

### 5.6.6 Alert Resolution Workflow

The fraud service maintains `FraudDecision` entities in the `fraud.FRAUD_DECISION` table. Each decision records the transaction evaluated, the final status and tier, the risk score, the rule hits and explanation, the optional ML reasoning, and a user resolution (`PENDING`, `LEGITIMATE`, or `FRAUD_REPORTED`).

Users can view their own alerts via `GET /api/fraud/user/alerts` (filtered to their `clientId`) and resolve them via `POST /api/fraud/user/alerts/{id}/resolve`. When a user resolves an alert as `FRAUD_REPORTED`, account-service is called to freeze the account. When resolved as `LEGITIMATE`, account-service is called to unfreeze it.

Administrators can view all pending alerts via `GET /api/fraud/alerts` and perform manual reviews via `PUT /api/fraud/decisions/{id}/review`, updating the decision status and recording their username and notes.

## 5.7 API Gateway

### 5.7.1 Overview

The API Gateway is the only public-facing service. It runs on port 8443 (HTTPS in production) or port 8080 (HTTP in development). It is built with Spring Cloud Gateway (reactive, Netty-based) and Spring Security configured to allow all exchanges (security is handled entirely through the custom filter chain).

### 5.7.2 Route Configuration

`GatewayConfig.routes()` defines the routing table:

| Route | Path | JWT Required | Notes |
|---|---|---|---|
| auth-service | `/api/auth/**` | No | Login, register, 2FA, refresh |
| client-sign-up | `/api/clients/sign-up` | No | Must come before the wildcard client route |
| client-service | `/api/clients/**`, `/api/gdpr/**` | Yes | Profile, contact, GDPR |
| account-service | `/api/accounts/**` | Yes | Accounts, transfers, balance |
| transaction-service | `/api/transactions/**` | Yes | Transaction history |
| payment-webhook | `/api/payments/webhook` | No | Stripe signature verification only |
| payment-service | `/api/payments/**`, `/api/payment-methods/**` | Yes | Payments, methods |
| fraud-health | `/api/fraud/health` | No | Health check |
| fraud-service | `/api/fraud/**` | Yes | Decisions, alerts |

### 5.7.3 Token Blacklist

`TokenBlacklistService` stores SHA-256 hashes of invalidated access tokens in Redis under the key `gateway:blacklist:token:{hash}`. The TTL is set to the remaining lifetime of the token (so the entry expires precisely when the token would have expired). Before forwarding any JWT-protected request, `JwtAuthFilter` calls `tokenBlacklistService.isBlacklisted(token)` reactively. Storing the hash (not the token itself) ensures that even if the Redis blacklist is read by an attacker, no valid tokens are exposed.

### 5.7.4 Rate Limiting

`RedisRateLimitService.allowRequest()` uses a Redis counter (INCR + EXPIRE) to implement a sliding window rate limiter. The counter is incremented on each request; the expiry is set on the first increment of each window. Two rate limiters are configured:

- **Login rate limiter** (`LoginRateLimitFilter`): 5 requests per 60 seconds per IP on `POST /api/auth/login`.
- **Global rate limiter** (`RateLimitFilter`): 50 requests per second per IP for all other routes except OPTIONS and `/api/auth/login`.

### 5.7.5 Logout Blacklisting

`LogoutBlacklistFilter` intercepts `POST /api/auth/logout` requests. It reads the access token from either the `Authorization` header or the request body (`accessToken` field). Before forwarding the request to auth-service, it calls `tokenBlacklistService.blacklist(token)` to immediately invalidate the token. This ensures that logged-out tokens cannot be replayed even within their remaining validity period.

### 5.7.6 Audit Logging

`AuditLoggingFilter` logs every completed request with its timestamp, authenticated user (parsed from JWT claims), HTTP method, path, response status, and source IP. It additionally logs a `SECURITY_AUDIT` event for `403` responses to `/api/clients/view`, which indicates an unauthorised attempt by a non-admin user to access the admin client list.

## 5.8 Frontend

### 5.8.1 Structure

The React SPA has two main dashboards: the user dashboard (accessible to users with the `USER` role) and the admin dashboard (accessible to users with the `ADMIN` role). Navigation is handled by React Router, with protected routes that redirect unauthenticated users to the login page.

### 5.8.2 Authentication Flow

The frontend stores the access token and refresh token in `sessionStorage`. The `apiClient` Axios instance attaches the `Authorization: Bearer {token}` header to every request. The response interceptor catches `401 Unauthorized` responses (but not for auth endpoints themselves), calls `authService.refreshAccessToken()`, and retries the original request with the new token. If the refresh fails (e.g., the refresh token is also expired or revoked), the user is redirected to the login page.

The 2FA flow is handled by the `TwoFactorVerify` page: after a login that returns `twoFactorRequired: true`, the frontend stores the temporary token and presents a TOTP code input form.

### 5.8.3 Stripe Integration in the Frontend

The card top-up UI uses the Stripe.js `loadStripe()` function and the `<Elements>` provider with a `<CardElement>` component. The flow: the user enters an amount, the frontend calls its own backend to create a PaymentIntent and get the `clientSecret`, the user enters card details (handled entirely by Stripe.js — no card data touches CashTactics servers), the frontend calls `stripe.confirmCardPayment()`, and on success, optionally calls the `/api/payments/top-up/confirm` endpoint for local/dev scenarios where webhook delivery cannot be guaranteed.

---

# CHAPTER 6. TESTING AND EVALUATION

## 6.1 Testing Strategy

The project uses three levels of testing: unit tests for isolated logic, integration tests for service behavior through the Spring MockMvc layer, and manual end-to-end tests via the API Gateway.

## 6.2 Unit Tests

### 6.2.1 Fraud Detection Unit Tests

The fraud service has the most thorough unit test coverage, reflecting the complexity and sensitivity of its logic.

`FraudFeatureEngineTest` covers all six feature computation methods with parameterised tests:
- `amountRatio` is correctly capped at 1.0 for amounts exceeding the legal cap.
- `typeRiskLive()` maps `POS_PAYMENT` to 0.0, `TRANSFER_INTERNAL` to 1.0, and `TRANSFER_EXTERNAL`/`TRANSFER_INSTANT` to 3.0.
- `hourSuspicion` returns 3.0 for hours 01:00–05:00, 2.0 for 00:00, 06:00–07:00, 23:00, and 1.0 for daytime hours.
- `newAccountFlag` returns 1.0 for accounts younger than 30 days and 0.0 otherwise.
- `senderDepletionRatio` handles null balances, zero balances, and over-depletion correctly.
- `isRoundAmount` includes an epsilon tolerance test for amounts like `500.0000000001` resulting from floating-point arithmetic.

`PaySimFeatureMapperTest` verifies that training features (PaySim mapping) and inference features (live PSD2 mapping) produce consistent values for equivalent transaction types — specifically that `PaySim:TRANSFER` maps to the same risk level as `Live:TRANSFER_EXTERNAL`.

`RuleEngineTest` tests the three Tier 1 rules: a normal transaction returns `ALLOW`; a large amount triggers `STEP_UP_REQUIRED`; a self-transfer always returns `ALLOW` regardless of amount; burst detection works when the mock repository returns a count at or above the burst limit.

`MlUtilsTest` covers MinMax scaling (including out-of-distribution clamping to [0, 1]), column-wise mean and min/max computation, and `argmax`/`argmax2`.

`ModelStoreTest` verifies round-trip serialisation: a model snapshot can be saved to a temp file and loaded back with all fields intact (threshold, version, feature means, mins, maxes, fraud rate, trained-on-rows count).

`ReasoningBuilderTest` verifies that the text explanations correctly identify the primary and secondary contributing features from the importance vector.

`FeatureVectorBuilderTest` verifies that the full inference pipeline (raw feature computation + MinMax scaling using a test snapshot) produces the expected scaled values for each feature.

### 6.2.2 Gateway Unit Tests

`RedisRateLimitServiceTest` verifies: the first request to a new key is allowed and the expiry is set; a request whose counter exceeds the limit is blocked; the expiry is not re-set after the first increment.

`TokenBlacklistServiceTest` verifies: blacklisting stores the SHA-256-hashed token key with a TTL proportional to the remaining token lifetime; checking blacklist status uses the hashed key; an already-expired token is skipped without a Redis write.

### 6.2.3 Service Unit Tests

`ClientEncryptionLifecycleServiceTest` covers four scenarios: no migration when the data is already encrypted with the new key; migration using the active fallback key; migration using the previous fallback key (key rotation scenario); and no migration when no candidate key can decrypt the data.

`ClientKeyResolverTest` verifies that a provided key takes priority over the fallback, and that a blank or null key falls back to the active key.

`AuthStepUpClientTest` verifies that a `428 Precondition Required` response is mapped to `StepUpRequiredException` and a `401 Unauthorized` response is mapped to `BusinessRuleViolationException`.

Controller unit tests (`AccountControllerTest`, `ClientControllerTest`, `PaymentControllerTest`, `TransactionControllerTest`, `PaymentMethodControllerTest`) verify that the `OwnershipChecker` is called with the correct arguments for each protected endpoint.

## 6.3 Integration Tests

`AccountControllerIT` (WebMvcTest) verifies access control for the account endpoints: a client cannot access another client's accounts (403), an admin can access any client's accounts (200), a client can access their own accounts (200), and the admin-only `close` endpoint returns 403 for non-admin users.

`TransactionControllerIT`, `PaymentControllerIT`, `PaymentMethodControllerIT`, and `ClientControllerIT` follow the same pattern: verifying that ownership enforcement and role-based access control work correctly at the controller layer.

## 6.4 Manual End-to-End Testing

### 6.4.1 Authentication Flows

The authentication flows can be tested through the gateway at `https://localhost:8443`:

```
POST /api/auth/login
{ "usernameOrEmail": "admin@cashtactics.com", "password": "password" }
→ 200 { "token": "...", "refreshToken": "...", "clientId": 1, "role": "ADMIN" }
```

After login, the access token can be used for any subsequent protected call. Testing refresh token rotation requires using the returned refresh token in a `POST /api/auth/refresh-token` request, verifying that the old refresh token is rejected on a second use.

### 6.4.2 Transfer with Fraud Check

A transfer can be tested by calling:

```
POST /api/accounts/transfer
Authorization: Bearer {token}
{ "fromIban": "RO49BANK0000000001EUR", "toIban": "RO49BANK0000000002EUR", "amount": 100.00 }
```

A transfer above the large-transfer threshold (1,000) without a TOTP code returns `428 Precondition Required`. A transfer above the Tier 1 rule threshold (10,000) triggers the fraud rule engine and also requires step-up. The `X-TOTP-Code` request header carries the 6-digit TOTP code.

### 6.4.3 Stripe Top-Up

Manual Stripe testing follows the flow described in `services/payment-service/TESTING_GUIDE.md`: the Stripe CLI is used to create test PaymentMethods with `tok_visa` (always succeeds) or `tok_chargeDeclined` (always fails), the `stripe listen --forward-to localhost:8085/api/payments/webhook` command forwards webhook events to the local service, and the standard Stripe test card numbers are used.

### 6.4.4 Gateway Security Tests

JWT enforcement is verified by calling a protected endpoint without a token (expecting 401) and with a deliberately invalid token (expecting 401). Rate limiting is verified by sending more than 50 requests per second to a protected endpoint (expecting 429 on subsequent requests). Circuit breaker fallback is verified by stopping a backend service and observing the 503 response from the gateway.

### 6.4.5 GDPR Erasure

The GDPR right-to-erasure flow is tested by:
1. Calling `DELETE /api/gdpr/clients/{id}/delete` with admin credentials.
2. Verifying that the auth user is disabled (login returns 401).
3. Verifying that the accounts are CLOSED.
4. Verifying that transaction details are replaced with "ANONYMIZED".
5. Verifying that the client's name fields contain an encrypted placeholder.

## 6.5 Evaluation Summary

The system achieves its primary goals:

- **Security**: Access to protected endpoints requires a valid, non-blacklisted JWT. Ownership checks prevent users from accessing each other's data. Step-up authentication is enforced for high-risk operations.
- **Privacy**: PII is encrypted at rest with user-derived keys. Admin views do not expose PII without an audited reveal. GDPR erasure is fully implemented across all services.
- **Fraud detection**: The three-tier pipeline provides defence in depth: fast rules for obvious cases, behavioural scoring for pattern detection, and ML anomaly detection for subtle signals. The fail-open design for Tier 3 ensures that ML model unavailability does not block legitimate transactions.
- **Availability**: Circuit breakers prevent cascade failures. Redis falls back gracefully (rate limiter logs a warning and allows the request; token blacklist check fails open on Redis errors).

---

# CHAPTER 7. CONCLUSIONS

## 7.1 Summary of Achievements

This thesis has presented the design and implementation of CashTactics, a full-stack online banking platform built as a microservices system. The project successfully demonstrates that it is possible to build a banking application where security, privacy, and fraud detection are treated as structural requirements rather than features bolted on at the end.

The most technically significant outcome is the **user-derived encryption key model**: the insight that a user's password can be used as the source of the encryption key for their own data, so that the server genuinely cannot read PII without the user's cooperation. This design would survive a database breach — the data is unreadable without the key, and the key is never stored.

The **three-tier fraud pipeline** is another contribution worth highlighting. Most academic fraud detection projects focus on a single algorithm evaluated on a historical dataset. This project integrates three qualitatively different approaches (rule engine, weighted behavioural scoring, ML anomaly detection) in a single service with a coherent decision flow, a defined fallback policy for each tier's failure modes, and a human-in-the-loop alert resolution workflow.

The **schema-per-service database isolation** prevents the most common distributed-system antipattern in educational projects: the "microservices" application where all services share a single database and the microservices boundaries exist only at the API layer. By giving each service its own schema with its own Flyway migration history, the project achieves genuine data ownership.

## 7.2 Lessons Learned

Several things turned out to be harder in practice than they appeared in design:

**Keeping diagrams and code aligned is a continuous effort.** By the time the implementation was complete, some of the earlier architecture diagrams were already out of date. Treating diagrams as living artefacts that are updated alongside code changes would have saved a lot of catching-up.

**Inter-service calls create operational dependencies that do not disappear with careful API design.** The GDPR erasure flow calls four services in sequence with no rollback mechanism beyond returning an error. A proper production system would use the Saga pattern with compensating transactions or an event-driven choreography approach to make the erasure reliably atomic.

**The user-derived key model has a usability cost.** If a user forgets their password and an administrator resets it, the old key (derived from the old password) cannot be recovered, so any data encrypted with it becomes permanently unreadable. A production system would need a carefully designed key recovery mechanism — possibly involving a key escrow process that itself respects privacy.

**Rate-limiting in memory does not scale.** The auth-service's in-process `ConcurrentHashMap` rate limiter works for a single instance but would not provide consistent protection across multiple running instances. The gateway's Redis-backed rate limiter is the correct solution, but the in-service rate limiter was not removed when the gateway version was added, resulting in duplicate logic.

## 7.3 Limitations

- The system runs on a single PostgreSQL instance. While schema-per-service isolation provides logical separation, a single instance means there is no physical fault isolation between the data of different services.
- The ML model (Tier 3) is trained on the PaySim synthetic dataset, which simulates mobile money transactions in Africa. While useful for demonstrating the pipeline architecture, the model's feature space may not generalise perfectly to European banking transaction patterns.
- The frontend stores tokens in `sessionStorage`, which provides better isolation than `localStorage` but is still accessible to JavaScript in the same tab. An `HttpOnly` cookie would be more resistant to XSS attacks in a production context.
- The exchange rate integration fetches from the ECB XML feed, which provides rates only for EUR cross-rates, once per day. Intraday rate changes are not reflected.
- There is no CI/CD pipeline. Tests are run manually. A production system would require automated testing on every commit and deployment automation.

## 7.4 Future Work

Several directions for future development are evident from the implementation:

1. **Distributed saga for GDPR erasure**: replace the sequential HTTP calls with a choreography-based saga using a message broker (e.g., Apache Kafka or RabbitMQ) to make the erasure flow reliably atomic and observable.
2. **Better fraud model evaluation**: collect real feedback data from the alert resolution workflow (users confirming or denying fraud), use it to build a labelled dataset, and evaluate the Isolation Forest against a supervised baseline (e.g., XGBoost with SMOTE oversampling for the minority class).
3. **Production observability**: add distributed tracing (OpenTelemetry/Jaeger), structured log correlation IDs, and Prometheus metrics dashboards (Grafana). The Actuator and Micrometer infrastructure is already present in the services — it just needs a collection and visualisation layer.
4. **Horizontal scaling**: test the system under load with multiple instances of each service, using the Redis Pub/Sub cache invalidation to verify consistency, and Redisson distributed locks to verify that concurrent transfers do not produce incorrect balances.
5. **KMS integration for production**: implement the `KmsKeyManagementProvider` stub using a real KMS provider (AWS KMS, Azure Key Vault, or HashiCorp Vault) for production-grade key management.
6. **Concept drift monitoring**: implement a feature in fraud-service that computes the Population Stability Index (PSI) on the anomaly score distribution periodically and alerts when the distribution shifts significantly from the training distribution, signalling that model retraining is needed.

---

# BIBLIOGRAPHY

[1] Liu, F. T., Ting, K. M., & Zhou, Z. H. (2008). Isolation forest. *2008 Eighth IEEE International Conference on Data Mining*, 413–422. Available at: https://www.lamda.nju.edu.cn/publication/icdm08b.pdf

[2] Lopez-Rojas, E. A., Elmir, A., & Axelsson, S. (2016). PaySim: A financial mobile money simulator for fraud detection. *EMSS 2016: 28th European Modeling and Simulation Symposium*. Available at: https://www.msc-les.org/proceedings/emss/2016/EMSS2016_249.pdf

[3] Dal Pozzolo, A., Caelen, O., Johnson, R. A., & Bontempi, G. (2017). Calibrating probability with undersampling for unbalanced classification. *IEEE Transactions on Neural Networks and Learning Systems*, 23(8), 1502–1516. Pre-print available at: https://re.public.polimi.it/bitstream/11311/1044896/1/08038008.pdf

[4] He, H., & Garcia, E. A. (2009). Learning from imbalanced data. *IEEE Transactions on Knowledge and Data Engineering*, 21(9), 1263–1284. Available at: https://www.academia.edu/86894835/Learning_from_Imbalanced_Data

[5] Gama, J., Žliobaitė, I., Bifet, A., Pechenizkiy, M., & Bouchachia, A. (2014). A survey on concept drift adaptation. *ACM Computing Surveys*, 46(4), 1–37. Accepted manuscript available at: https://mpechen.win.tue.nl/publications/pubs/Gama_ACMCS_AdaptationCD_accepted.pdf

[6] Abdallah, A., Maarof, M. A., & Zainal, A. (2016). Fraud detection system: A survey. *Journal of Network and Computer Applications*, 68, 90–113. Available at: https://www.semanticscholar.org/paper/Fraud-detection-system%3A-A-survey-Abdallah-Maarof/5187df17503f78c7e063c2ea0a707e3e59c48235

[7] Bahnsen, A. C., Aouada, D., Stojanovic, A., & Ottersten, B. (2016). Feature engineering strategies for credit card fraud detection. *Expert Systems with Applications*, 51, 134–142. Available at: https://albahnsen.github.io/files/Feature%20Engineering%20Strategies%20for%20Credit%20Card%20Fraud%20Detection_published.pdf

[8] Richardson, C. (2018). *Microservices Patterns: With Examples in Java*. Manning Publications. Available at: https://www.academia.edu/41827915/_Microservice_Pattern

[9] Walls, C. (2016). *Spring Boot in Action*. Manning Publications. Available at: https://www.nitinagrawal.com/uploads/2/1/3/6/21361954/spring_boot_in_action.pdf

[10] Fowler, M. (2002). *Patterns of Enterprise Application Architecture*. Addison-Wesley. Available at: https://raw.githubusercontent.com/ZoranLi/Books1/master/Patterns%20of%20Enterprise%20Application%20Architecture.pdf

[11] Stripe, Inc. (2024). *Stripe API Reference — PaymentIntents*. Retrieved May 2026 from https://stripe.com/docs/api/payment_intents

[12] Spring Framework (2024). *Spring Cloud Gateway Reference Documentation*. Retrieved May 2026 from https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/

[13] Spring Framework (2024). *Spring Security Reference Documentation*. Retrieved May 2026 from https://docs.spring.io/spring-security/reference/

[14] PostgreSQL Global Development Group (2024). *PostgreSQL 16 Documentation*. Retrieved May 2026 from https://www.postgresql.org/docs/16/

[15] Redis Ltd. (2024). *Redis Documentation — Commands Reference*. Retrieved May 2026 from https://redis.io/commands/

[16] Flyway by Redgate (2024). *Flyway Documentation*. Retrieved May 2026 from https://documentation.red-gate.com/fd

[17] Resilience4j (2024). *Resilience4j Documentation*. Retrieved May 2026 from https://resilience4j.readme.io/docs

[18] Javaxt.com (2023). *JJWT - Java JWT: JSON Web Tokens for Java and Android*. Retrieved May 2026 from https://github.com/jwtk/jjwt

[19] European Central Bank (2024). *ECB Reference Exchange Rates*. Retrieved May 2026 from https://www.ecb.europa.eu/stats/policy_and_exchange_rates/euro_reference_exchange_rates/html/index.en.html

[20] Haifeng Li (2024). *SMILE: Statistical Machine Intelligence and Learning Engine (version 3.1.1)*. Retrieved May 2026 from https://haifengl.github.io/

[21] European Parliament (2016). *General Data Protection Regulation (GDPR) — Regulation (EU) 2016/679*. Official Journal of the European Union, L 119/1. Available at: https://eur-lex.europa.eu/eli/reg/2016/679/oj

[22] M'Raihi, D., Bellare, M., Hoornaert, F., Naccache, D., & Ranen, O. (2005). *HOTP: An HMAC-Based One-Time Password Algorithm*. RFC 4226. IETF. Available at: https://tools.ietf.org/html/rfc4226

[23] M'Raihi, D., Machani, S., Pei, M., & Rydell, J. (2011). *TOTP: Time-Based One-Time Password Algorithm*. RFC 6238. IETF. Available at: https://tools.ietf.org/html/rfc6238

---

# APPENDICES

## Appendix A — List of Figures

| Figure | Title | Section | Source File |
|---|---|---|---|
| 4.1 | System Context Diagram | 4.1 | `docs/architecture/chapter4/system-context.puml` |
| 4.2 | Container Diagram | 4.2 | `docs/architecture/chapter4/containter.puml` |
| 4.3 | Data Zoning Model | 4.3 | `docs/architecture/chapter4/data-zoning.puml` |
| 4.4 | Inter-Service Communication Map | 4.4 | `docs/architecture/chapter4/inter-service-communication.puml` |
| 4.5 | API Gateway Request Lifecycle | 4.5 | `docs/architecture/appendix/api-gateway-request-lifecycle.puml` |
| 4.6 | Transfer Flow Sequence Diagram | 4.6 | `docs/architecture/chapter4/transfer-flow.puml` |
| 5.1 | Tier 3 Fraud Detection Pipeline | 5.6 | `docs/architecture/chapter5/transfer-flow-async-fraud.puml` |

All diagrams are authored in PlantUML and can be rendered using the free PlantUML online server at https://www.plantuml.com/plantuml or via the PlantUML plugin for VS Code or IntelliJ IDEA.

---

## Appendix B — List of Tables

| Table | Title | Section |
|---|---|---|
| 3.1 | Technology stack summary | 3 |
| 5.1 | Tier 2 scoring components and weights | 5.6.4 |
| 5.2 | Isolation Forest feature vector | 5.6.5 |
| 5.3 | API Gateway routing table | 5.7.2 |
| 6.1 | Unit test coverage summary | 6.2 |

---

## Appendix C — Acronyms

| Acronym | Full Form |
|---|---|
| 2FA | Two-Factor Authentication |
| AES | Advanced Encryption Standard |
| API | Application Programming Interface |
| AUC-ROC | Area Under the Receiver Operating Characteristic Curve |
| BCrypt | Blowfish Crypt (password hashing function) |
| CORS | Cross-Origin Resource Sharing |
| DDoS | Distributed Denial of Service |
| DTO | Data Transfer Object |
| ECB | European Central Bank |
| GDPR | General Data Protection Regulation |
| GCM | Galois/Counter Mode (AES cipher mode) |
| HMAC | Hash-based Message Authentication Code |
| HTTP | Hypertext Transfer Protocol |
| HTTPS | HTTP Secure |
| IBAN | International Bank Account Number |
| JWT | JSON Web Token |
| JPA | Java Persistence API |
| KMS | Key Management Service |
| LLM | Large Language Model |
| MVC | Model-View-Controller |
| ORM | Object-Relational Mapping |
| PBKDF2 | Password-Based Key Derivation Function 2 |
| PCI-DSS | Payment Card Industry Data Security Standard |
| PII | Personally Identifiable Information |
| PSI | Population Stability Index |
| REST | Representational State Transfer |
| SHA | Secure Hash Algorithm |
| SPA | Single-Page Application |
| SQL | Structured Query Language |
| SSL | Secure Sockets Layer |
| TLS | Transport Layer Security |
| TOTP | Time-based One-Time Password |
| TTL | Time to Live |
| URL | Uniform Resource Locator |

---

## Appendix D — Key Configuration Snippets

### D.1 Docker Compose Service Definition (excerpt)

```yaml
# deploy/docker-compose.yml (excerpt)
services:
  fraud-service:
    build: ../services/fraud-service
    env_file:
      - ../services/fraud-service/.env.properties
    environment:
      DB_PASSWORD: ${POSTGRES_PASSWORD}
      DB_URL: jdbc:postgresql://postgres:5432/banking?currentSchema=fraud
      TRANSACTION_SERVICE_URL: http://transaction-service:8084
      ACCOUNT_SERVICE_URL: http://account-service:8083
    depends_on:
      postgres:
        condition: service_healthy
    expose:
      - "8086"
```

### D.2 Fraud Service Properties (excerpt)

```properties
# services/fraud-service/src/main/resources/application.properties (excerpt)

# Tier 1 Rule Engine thresholds
fraud.tier1.large-amount-threshold=10000.0
fraud.tier1.new-account-age-days=30
fraud.tier1.burst-limit=5

# Tier 2 Behavioral Scoring thresholds
fraud.tier2.lower-threshold=30.0
fraud.tier2.upper-threshold=70.0
fraud.tier2.step-up-threshold=50.0

# Tier 3 ML Model configuration
fraud.tier3.ml-enabled=true
fraud.tier3.model-path=classpath:ml/model/isolation_forest_model.bin
fraud.tier3.ml-threshold=0.62
fraud.tier3.trainer-mode=false
```

### D.3 Key Flyway Migration SQL (excerpt)

```sql
-- V2__Create_fraud_decision.sql
CREATE TABLE IF NOT EXISTS "FRAUD_DECISION" (
    "ID"                    BIGSERIAL       PRIMARY KEY,
    "TRANSACTION_ID"        BIGINT,
    "ACCOUNT_ID"            BIGINT          NOT NULL,
    "CLIENT_ID"             BIGINT          NOT NULL,
    "STATUS"                FRAUD_DECISION_STATUS_ENUM NOT NULL DEFAULT 'ALLOW',
    "DECIDED_BY_TIER"       FRAUD_TIER_ENUM NOT NULL DEFAULT 'TIER1_RULES',
    "RISK_SCORE"            DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    "RULE_HITS"             TEXT,
    "EXPLANATION"           TEXT,
    "CREATED_AT"            TIMESTAMP       NOT NULL DEFAULT NOW()
);
```

### D.4 Selected API Endpoints (Postman Reference)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/login` | None | Login, returns access + refresh tokens |
| POST | `/api/auth/refresh-token` | None | Refresh access token using refresh token |
| POST | `/api/auth/logout` | JWT | Revoke refresh token and blacklist access token |
| POST | `/api/auth/2fa/setup` | JWT | Generate TOTP secret and QR URI |
| POST | `/api/auth/2fa/verify` | None | Verify TOTP code for temp token → full token |
| GET | `/api/accounts/by-client/{id}` | JWT | List accounts for a client |
| POST | `/api/accounts/transfer` | JWT | Execute a transfer (with optional TOTP step-up) |
| POST | `/api/payments/top-up/intent` | JWT | Create Stripe PaymentIntent for card top-up |
| GET | `/api/fraud/user/alerts` | JWT (USER) | List fraud alerts for the authenticated user |
| POST | `/api/fraud/user/alerts/{id}/resolve` | JWT (USER) | Resolve a fraud alert |
| GET | `/api/fraud/alerts` | JWT (ADMIN) | List all unresolved fraud alerts |
| DELETE | `/api/gdpr/clients/{id}/delete` | JWT | Trigger full GDPR right-to-erasure |
| GET | `/api/gdpr/clients/{id}/export` | JWT | Export all personal data for a client |

---

*End of Thesis*

---

**Bucharest, 2026**

*I hereby declare that this thesis is my own work, was written without unauthorised assistance, and has not been submitted for examination at any other institution.*

**Signature:** ___________________________

**Date:** ___________________________
