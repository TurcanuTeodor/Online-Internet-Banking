# Fraud-Service Tier 3 — Ghid Complet de Utilizare

## Ce s-a schimbat față de versiunea anterioară

| Aspect | ÎNAINTE | ACUM |
|--------|---------|------|
| Date antrenament | Sintetice (random Java) | **PaySim CSV real (Kaggle)** |
| Când se antrenează | La fiecare pornire (`@PostConstruct`) | **O singură dată, offline** |
| Unde trăiește modelul | În RAM, recrăat la restart | **`data/isolation_forest_model.bin`** |
| Cold start | ~2 sec la fiecare pornire | **~100ms (doar deserializare)** |
| Reproductibilitate | Seed fix dar date diferite între versiuni | **Model identic între deployments** |

---

## Structura fișierelor noi

```
services/fraud-service/src/main/java/ro/app/fraud/tier3/
├── PaySimRow.java          ← record cu câmpurile CSV PaySim
├── PaySimCsvReader.java    ← cititor CSV cu sub-sampling 150k
├── PaySimFeatureMapper.java← CLASA CENTRALĂ: PaySim/Live → double[6]
├── ModelStore.java         ← serializare/deserializare .bin
├── ModelTrainerCli.java    ← antrenor offline (CommandLineRunner)
├── Tier3MlService.java     ← REFACTORIZAT: încarcă din disc
│
│   (păstrate, nu mai sunt folosite direct)
├── FeatureVectorBuilder.java
└── TrainingDataGenerator.java
```

---

## Pasul 1: Obține dataset-ul PaySim

1. Mergi pe [Kaggle PaySim](https://www.kaggle.com/datasets/ealaxi/paysim1)
2. Descarcă `PS_20174392719_1491204439457_log.csv` (~470MB)
3. Pune fișierul în: `data/PS_20174392719_1491204439457_log.csv`

> Fișierul CSV este în `.gitignore` — nu se commitează în repo.

---

## Pasul 2: Antrenează modelul (o singură dată)

```bash
# Din directorul fraud-service:
.\mvnw.cmd package -DskipTests

# Rulează trainer-ul:
java -jar target/fraud-service-0.0.1-SNAPSHOT.jar \
  --fraud.tier3.trainer.mode=true \
  --fraud.tier3.pay-sim-csv-path=data/PS_20174392719_1491204439457_log.csv \
  --fraud.tier3.model-path=data/isolation_forest_model.bin \
  --fraud.tier3.pay-sim-max-rows=150000

# Sau pe Windows PowerShell:
java -jar target\fraud-service-0.0.1-SNAPSHOT.jar `
  --fraud.tier3.trainer-mode=true
```

Aplicația va:
1. Citi primele 150.000 de rânduri din CSV
2. Aplica feature engineering (`PaySimFeatureMapper`)
3. Antrena Isolation Forest pe 80% din date
4. Calibra threshold-ul optim pe 20% test (maximizare F1)
5. Afișa metrici: Precision, Recall, F1
6. Salva modelul la `data/isolation_forest_model.bin`
7. Se opri automat (`System.exit(0)`)

---

## Pasul 3: Pornire normală (producție)

```bash
java -jar target/fraud-service-0.0.1-SNAPSHOT.jar
# SAU Docker normal
```

La pornire, `Tier3MlService` va:
- Detecta că `data/isolation_forest_model.bin` există
- Deserializa modelul în ~100ms
- Loga: `✅ Tier3-ML model încărcat: version=paysim-v1.0 threshold=0.XX`

---

## Ce se întâmplă dacă modelul LIPSEȘTE?

Dacă `isolation_forest_model.bin` nu există (ex: mediu nou, CI/CD):

```
⚠️  Modelul Tier3 NU a fost găsit la: data/isolation_forest_model.bin
⚠️  Rulează antrenamentul offline: java -jar fraud-service.jar --fraud.tier3.trainer.mode=true
⚠️  Tier3 pornit în mod DEGRADAT (toate tranzacțiile → ALLOW)
```

Aplicația pornește normal. Tier 1 și Tier 2 funcționează. Tier 3 returnează ALLOW pentru toate tranzacțiile până când modelul este furnizat.

---

## Vectorul de features — explicație academică

Ambele căi (antrenament PaySim și inferență live) produc același vector `double[6]`:

```
[0] amountRatio      = min(1.0, amount / cap)
[1] balanceDeltaOrg  = (oldBal - newBal) / oldBal   [0.5 în live — necunoscut]
[2] balanceDeltaDest = newDest / (oldDest + amount)  [0.5 în live — necunoscut]
[3] typeRisk         = 1.0 dacă TRANSFER/CASH_OUT, altfel 0.0-0.5
[4] hourSuspicion    = 1.0 dacă oră în [0, 6), altfel 0.0
[5] newAccountFlag   = 1.0 dacă cont nou / balanță zero la sursă
```

> **Train/Serving Skew:** cel mai mare risc în ML producție. Dacă formula
> pentru antrenament diferă de cea pentru inferență → predicții greșite.
> `PaySimFeatureMapper` rezolvă asta: o singură clasă, două metode, aceleași formule.

---

## Verificare endpoint actuator

```bash
GET http://localhost:8086/actuator/fraud-model
```

Răspuns când modelul e încărcat:
```json
{
  "status": "ready",
  "enabled": true,
  "model_type": "isolation_forest",
  "threshold": 0.68,
  "model_version": "paysim-v1.0",
  "trained_at_epoch": 1748123456789,
  "dataset": "PaySim (Kaggle) — sub-sampled 150k rows",
  "details": "ML model is trained and ready for inference on anomaly detection"
}
```

---

## Când trebuie re-antrenat modelul?

| Situație | Acțiune |
|----------|---------|
| Schimbare în `PaySimFeatureMapper` | Re-antrenare obligatorie |
| Schimbare în numărul de features (nu 6) | Re-antrenare + bump versiune |
| Drift de date detectat în producție | Re-antrenare recomandată |
| Hyperparametri schimbați (trees, subsample) | Re-antrenare |
| Simplu restart aplicație | **NU** — modelul se încarcă din disc |

---

## Fișiere de configurare

`application.properties` — secțiunea Tier 3:

```properties
# Calea modelului pre-antrenat
fraud.tier3.model-path=data/isolation_forest_model.bin

# Calea CSV-ului PaySim (necesar doar la antrenament)
fraud.tier3.pay-sim-csv-path=data/PS_20174392719_1491204439457_log.csv

# Sub-sampling (150k = optim)
fraud.tier3.pay-sim-max-rows=150000

# Mod trainer — false în producție, true doar pentru antrenament
fraud.tier3.trainer-mode=false
```
