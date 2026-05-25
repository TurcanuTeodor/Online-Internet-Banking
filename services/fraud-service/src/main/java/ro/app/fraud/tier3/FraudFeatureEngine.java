package ro.app.fraud.tier3;

/**
 * =====================================================================
 * — Single Source of Truth pentru Feature Engineering
 * =====================================================================
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
 * -----------------------------------------------------------------------
 *
 * [0] amountRatio — suma normalizata prin raportare la plafonul legal Transfond.
 *     Range: [0.0, 1.0] (capped la 1.0 — orice suma >= 50.000 RON = alerta maxima)
 *     Calcul: min(amount / LEGAL_AMOUNT_CAP, 1.0)
 *     Motiv: atacatorii maximizeaza profitul; normalizarea protejeaza de scale invariance in ML.
 *
 * [1] typeRisk — riscul asociat tipului de tranzactie (scara PSD2).
 *     Range: {0.0, 1.0, 2.0, 3.0}
 *     Motiv: transferurile instant (SEPA Instant) nu permit recall — risc maxim.
 *
 * [2] hourSuspicion — grupa de risc ciclica pe ora zilei.
 *     Range: {1.0, 2.0, 3.0} (nu binar — evita functia liniara 0-23)
 *     Motiv: atacurile au loc noaptea pentru a intarzia notificarile.
 *
 * [3] newAccountFlag — semnal de cont nou la sender.
 *     Range: {0.0, 1.0} (feature binar)
 *     Motiv: burner accounts (< 30 zile) = vectori de money laundering.
 *
 * [4] senderDepletionRatio — ce procent din contul senderului paraseste contul.
 *     Range: [0.0, 1.0] (capped la 1.0)
 *     Calcul: min(amount / oldBalanceOrg, 1.0)
 *     Motiv: golirea completa a contului = semnatura Account Takeover (ATO).
 *
 * [5] isRoundAmount — flag comportamental pentru sume rotunde.
 *     Range: {0.0, 1.0} (feature binar)
 *     Motiv: atacatorii Cash-Out folosesc sume rotunde (100, 500, 1000).
 */
public final class FraudFeatureEngine {

    // Plafonul legal unificat pentru normalizarea sumei.
    // Reprezinta limita maxima pentru plati instant Transfond (Romania),
    // echivalentul directivei AML (~10.000 EUR). Sursa: BNR / Transfond 2024.
    // Utilizat IDENTIC in antrenament (PaySimFeatureMapper) si in inferenta live (FeatureVectorBuilder).
    public static final double LEGAL_AMOUNT_CAP = 50_000.0;

    public static final int NEW_ACCOUNT_THRESHOLD_DAYS = 30;

    private FraudFeatureEngine() {
        // util class
    }

    // -----------------------------------------------------------------------
    // [0] amountRatio
    // -----------------------------------------------------------------------

    /**
     * Normalizeaza suma la plafonul legal. Capped la 1.0.
     * Exemplu: computeAmountRatio(75_000, LEGAL_AMOUNT_CAP) → 1.0
     * Exemplu: computeAmountRatio(25_000, LEGAL_AMOUNT_CAP) → 0.5
     */
    public static double computeAmountRatio(double amount, double cap) {
        if (cap <= 0) return 0.0;
        return Math.min(amount / cap, 1.0);
    }

    // -----------------------------------------------------------------------
    // [1] typeRisk — scara PSD2
    // -----------------------------------------------------------------------

    /**
     * Risc bazat pe tipul tranzactiei in contextul PSD2 / Open Payments.
     * Scara: POS_PAYMENT=0 (biometric/chip) → TRANSFER_INSTANT=3 (no recall).
     * Tipurile PaySim sunt mapate pe aceeasi scara pentru consistenta la antrenament.
     */
    public static double computeTypeRiskLive(String type) {
        if (type == null) return 1.0;
        return switch (type.toUpperCase().trim()) {
            case "POS_PAYMENT"       -> 0.0; // Sigur: card fizic / biometrie
            case "TRANSFER_INTERNAL" -> 1.0; // Risc normal: intern, reversibil
            case "TRANSFER_EXTERNAL" -> 2.0; // Risc ridicat: interbancar
            case "TRANSFER_INSTANT"  -> 3.0; // Risc maxim: SEPA Instant, no recall
            default                  -> 1.0; // Default: risc normal
        };
    }

    /**
     * Mapeaza tipurile PaySim pe scara PSD2 pentru antrenament consistent.
     * TRANSFER/CASH_OUT → risc maxim; PAYMENT/DEBIT → risc normal; CASH_IN → sigur.
     */
    public static double computeTypeRiskPaySim(String paySimType) {
        if (paySimType == null) return 1.0;
        return switch (paySimType.toUpperCase().trim()) {
            case "TRANSFER", "CASH_OUT" -> 3.0; // Echivalent TRANSFER_INSTANT (fraud predominant)
            case "PAYMENT", "DEBIT"     -> 1.0; // Echivalent TRANSFER_INTERNAL
            case "CASH_IN"              -> 0.0; // Echivalent POS_PAYMENT (intrare fonduri)
            default                     -> 1.0;
        };
    }

    // -----------------------------------------------------------------------
    // [2] hourSuspicion — grupe de risc ciclice
    // -----------------------------------------------------------------------

    /**
     * Grupa de risc ciclica pentru ora zilei. Evita functia liniara 0-23.
     * Grupe:
     *   3.0 = Risc Maxim  → [1, 5]       (noapte tarziu: atacuri active)
     *   2.0 = Risc Mediu  → {0, 6, 7, 23} (tranzitie zi/noapte: marginal)
     *   1.0 = Risc Normal → restul orelor  (ziua)
     *
     * @param hour ora zilei [0, 23]
     */
    public static double computeHourSuspicionFromClock(int hour) {
        if (hour >= 1 && hour <= 5)                              return 3.0;
        if (hour == 23 || hour == 0 || hour == 6 || hour == 7)  return 2.0;
        return 1.0;
    }

    // -----------------------------------------------------------------------
    // [3] newAccountFlag
    // -----------------------------------------------------------------------

    public static double computeNewAccountFlagFromAge(int accountAgeDays) {
        return accountAgeDays < NEW_ACCOUNT_THRESHOLD_DAYS ? 1.0 : 0.0;
    }

    // -----------------------------------------------------------------------
    // [4] senderDepletionRatio
    // -----------------------------------------------------------------------

    /**
     * Procentul din contul senderului care paraseste contul intr-o singura tranzactie.
     * Valoare 1.0 = contul este golit complet = semnatura Account Takeover (ATO).
     *
     * @param amount        suma tranzactiei
     * @param oldBalanceOrg soldul sender INAINTE de tranzactie (nullable → 0.0)
     */
    public static double computeSenderDepletionRatio(double amount, Double oldBalanceOrg) {
        if (oldBalanceOrg == null || oldBalanceOrg <= 0) return 0.0;
        return Math.min(amount / oldBalanceOrg, 1.0);
    }

    // -----------------------------------------------------------------------
    // [5] isRoundAmount
    // -----------------------------------------------------------------------

    /**
     * Flag comportamental: suma rotunda la 100 = specific atacurilor Cash-Out.
     * Exemplu: 500.0 → 1.0 | 537.50 → 0.0
     */
    public static double computeRoundAmountFlag(double amount) {
        return (amount % 100.0 == 0) ? 1.0 : 0.0;
    }


}
