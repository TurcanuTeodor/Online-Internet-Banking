package ro.app.fraud.tier3;

/**
 * — Single Source of Truth pentru Feature Engineering
 *
 * SCOP:
 * Contine exclusiv logica matematica de calcul a celor 6 dimensiuni
 * ale vectorului de features al modelului Isolation Forest.
 *
 * STRATEGIE: Open Payments / PSD2 — Behavioral Fraud Detector
 * Vectorul este axat exclusiv pe metadatele tranzactiei si comportamentul
 * expeditorului (Sender). Soldul destinatarului NU este folosit deoarece
 * in transferurile externe (Off-Us) acesta este inaccesibil.
 *
 * SOLUTIE — Principiul DRY (Don't Repeat Yourself):
 * Ambele clase (PaySimFeatureMapper, FeatureVectorBuilder) devin simple
 * adapters care extrag valorile brute si le paseaza metodelor de jos.
 *
 * -----------------------------------------------------------------------
 * VECTORUL DE 6 FEATURES — definitie canonica PSD2
 *
 * [0] amountRatio — suma normalizata prin raportare la plafonul legal
 * Transfond.
 * Range: [0.0, 1.0] (capped la 1.0 — orice suma >= 50.000 RON = alerta maxima)
 * Calcul: min(amount / LEGAL_AMOUNT_CAP, 1.0)
 * Motiv: atacatorii maximizeaza profitul; normalizarea protejeaza de scale
 * invariance in ML.
 *
 * [1] typeRisk — riscul asociat tipului de tranzactie (scara PSD2).
 * Range: {0.0, 1.0, 3.0} — aliniata cu PaySim ({CASH_IN=0, PAYMENT=1,
 * TRANSFER/CASH_OUT=3})
 * IMPORTANT: valoarea 2.0 a fost eliminata pentru a preveni out-of-distribution
 * la inferenta.
 * TRANSFER_EXTERNAL mapat pe 3.0 (la fel ca TRANSFER_INSTANT) — ambele sunt
 * ireversibile.
 * Motiv: transferurile instant (SEPA Instant) nu permit recall — risc maxim.
 *
 * [2] hourSuspicion — grupa de risc ciclica pe ora zilei.
 * Range: {1.0, 2.0, 3.0} (nu binar — evita functia liniara 0-23)
 * Motiv: atacurile au loc noaptea pentru a intarzia notificarile.
 *
 * [3] newAccountFlag — semnal de cont nou la sender.
 * Range: {0.0, 1.0} (feature binar)
 * Motiv: burner accounts (< 30 zile) = vectori de money laundering.
 *
 * [4] senderDepletionRatio — ce procent din contul senderului paraseste contul.
 * Range: [0.0, 1.0] (capped la 1.0)
 * Calcul: min(amount / oldBalanceOrg, 1.0)
 * Motiv: golirea completa a contului = semnatura Account Takeover (ATO).
 *
 * [5] isRoundAmount — flag comportamental pentru sume rotunde.
 * Range: {0.0, 1.0} (feature binar)
 * Motiv: atacatorii Cash-Out folosesc sume rotunde (100, 500, 1000).
 */
public final class FraudFeatureEngine {

    // Plafonul legal unificat pentru normalizarea sumei.
    // Reprezinta limita maxima pentru plati instant Transfond (Romania),
    // echivalentul directivei AML (~10.000 EUR). Sursa: BNR / Transfond 2024..
    public static final double LEGAL_AMOUNT_CAP = 50_000.0;

    public static final int NEW_ACCOUNT_THRESHOLD_DAYS = 30;

    private FraudFeatureEngine() {
        // util class
    }

    // [0] amountRatio
    public static double computeAmountRatio(double amount, double cap) {
        if (cap <= 0)
            return 0.0;
        return Math.min(amount / cap, 1.0);
    }

    // [1] typeRisk — scara PSD2
    public static double computeTypeRiskLive(String type) {
        if (type == null)
            return 1.0;
        return switch (type.toUpperCase().trim()) {
            case "POS_PAYMENT" -> 0.0; // Sigur: card fizic / biometrie
            case "TRANSFER_INTERNAL" -> 1.0; // Risc normal: intern, reversibil
            case "TRANSFER_EXTERNAL" -> 3.0; // Risc maxim: interbancar ireversibil (aliniat PaySim TRANSFER)
            case "TRANSFER_INSTANT" -> 3.0; // Risc maxim: SEPA Instant, no recall
            default -> 1.0; // Default: risc normal
        };
    }

    /**
     * Mapeaza tipurile PaySim pe scara PSD2 pentru antrenament consistent.
     * TRANSFER/CASH_OUT → risc maxim; PAYMENT/DEBIT → risc normal; CASH_IN → sigur.
     */
    public static double computeTypeRiskPaySim(String paySimType) {
        if (paySimType == null)
            return 1.0;
        return switch (paySimType.toUpperCase().trim()) {
            case "TRANSFER", "CASH_OUT" -> 3.0; // Echivalent TRANSFER_INSTANT (fraud predominant)
            case "PAYMENT", "DEBIT" -> 1.0; // Echivalent TRANSFER_INTERNAL
            case "CASH_IN" -> 0.0; // Echivalent POS_PAYMENT (intrare fonduri)
            default -> 1.0;
        };
    }

    // [2] hourSuspicion — grupe de risc ciclice
    public static double computeHourSuspicionFromClock(int hour) {
        if (hour >= 1 && hour <= 5)
            return 3.0;
        if (hour == 23 || hour == 0 || hour == 6 || hour == 7)
            return 2.0;
        return 1.0;
    }

    // [3] newAccountFlag
    public static double computeNewAccountFlagFromAge(int accountAgeDays) {
        return accountAgeDays < NEW_ACCOUNT_THRESHOLD_DAYS ? 1.0 : 0.0;
    }

    // [4] senderDepletionRatio
    public static double computeSenderDepletionRatio(double amount, Double oldBalanceOrg) {
        if (oldBalanceOrg == null || oldBalanceOrg <= 0)
            return 0.0;
        return Math.min(amount / oldBalanceOrg, 1.0);
    }

    // [5] isRoundAmount
    public static double computeRoundAmountFlag(double amount) {
        return (Math.abs(amount % 100.0) < 0.01) ? 1.0 : 0.0;
    }

}
