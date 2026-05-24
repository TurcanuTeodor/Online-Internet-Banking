package ro.app.fraud.tier3;

/**
 * =====================================================================
 * — Single Source of Truth pentru Feature Engineering
 * =====================================================================
 *
 * SCOP:
 * Contine exclusiv logica matematica de calcul
 * celor 6 dimensiuni ale vectorului de features al modelului Isolation Forest.
 *
 * SOLUTIE — Principiul DRY (Don't Repeat Yourself):
 * Ambele clase (PaySimFeatureMapper, FeatureVectorBuilder) devin simple
 * adapters care extrag valorile brute și le pasează metodelor de jos.
 *
 * DE CE final class + private constructor?
 * - final → nu poate fi extinsă (nu are sens sa mosteneasca o clasă util)
 * - private constructor → nu poate fi instantiata(toate metodele sunt
 * statice)
 *
 * -----------------------------------------------------------------------
 * VECTORUL DE 6 FEATURES — definitie canonica
 * -----------------------------------------------------------------------
 *
 * [0] amountRatio — valoarea nomalizata a tranzactiei prin raportare la un 
 * plafon max acceptat.
 * Range: [0.0, 1.0] 
 * Calcul: amount / cap (cap = 10.000 pentru PaySim, 5.000 pentru live)
 * Motiv: atacatorii incearca sa maximizeze profitul efectuand tranzactii la 
 * limitele maxime permise de sistem. Raportul protejeaza de problema scale invariance in ML, 
 * unde modelul ar putea ignora complet suma daca nu este normalizata.
 *
 * [1] balanceDeltaOrg — cat din soldul initial al sender-ului a parasit contul in
 * urma tranzactiei.
 * Range: [0.0, 1.0] (0.5 = neutral/necunoscut în context live)
 * Calcul: (oldbalance - newbalance) / oldbalance
 * Motiv: cel mai puternic predictor al fraudei ACCOUNT TAKEOVER(ATO) 
 * din bataset-ul PaySim = CASH_OUT care goleste complet contul
 * (delta → 1.0). Delta mica = tranzactie normala.
 *
 * [2] balanceDeltaDest — cat din suma expediata a ajuns efectiv in soldul destinatarului.
 * Range: [0.0, 1.0] (0.5 = neutral/necunoscut în context live)
 * Calcul: newbalanceDest / (oldbalanceDest + amount)
 * Motiv: daca suma nu a ajuns la destinatar conform asteptarilor,
 * poate indica deturnare (man-in-the-middle).
 *
 * [3] typeRisk — riscul asociat tipului de tranzactie 
 * Range: {0.0, 0.2, 0.3, 0.5, 1.0} (valori discrete)
 * Motiv: in dataset 100% din fraude sunt TRANSFER sau CASH_OUT.
 * PAYMENT si DEBIT nu sunt niciodata fraudate.
 *
 * [4] hourSuspicion — daca tranzactia are loc in intervalul nocturn suspect [0, 6)?
 * Range: {0.0, 1.0} (feature binar)
 * Motiv: atacurile au loc noaptea pt a intarzia notificarile si a max fereastra de timp
 * pentru deturnarea fondurilor, inainte de blocarea contului/cardului.
 *
 * [5] newAccountFlag — semnal de cont nou / cont cu sold zero la sender
 * Range: {0.0, 1.0} (feature binar)
 * Motiv: conturile noi/burner accounts (<30 zile) sunt vectori de money laundering.
 */
public final class FraudFeatureEngine {

    // Praguri de normalizare pentru sume 
    public static final double PAYSIM_AMOUNT_CAP = 10_000.0;
    public static final double LIVE_AMOUNT_CAP = 5_000.0;

    public static final int NEW_ACCOUNT_THRESHOLD_DAYS = 30;

    public static final int NIGHT_HOUR_END = 6;

    public static final double NEUTRAL = 0.5;

    private FraudFeatureEngine() {
        //util class
    }

    /**
     * Exemplu PaySim: computeAmountRatio(8000.0, PAYSIM_AMOUNT_CAP) → 0.80
     * Exemplu live: computeAmountRatio(6000.0, LIVE_AMOUNT_CAP) → 1.00 (capped)
     */
    public static double computeAmountRatio(double amount, double cap) {
        if (cap <= 0)
            return 0.0; 
        return Math.min(1.0, amount / cap);
    }

    public static double computeBalanceDeltaOrg(double oldBal, double newBal) {
        if (oldBal <= 0)
            return 0.0;
        double delta = (oldBal - newBal) / oldBal;
        return Math.max(0.0, Math.min(1.0, delta));
    }

    public static double computeBalanceDeltaDest(double oldDest, double newDest, double amount) {
        double expected = oldDest + amount;
        if (expected <= 0)
            return NEUTRAL;
        return Math.max(0.0, Math.min(1.0, newDest / expected));
    }

    public static double computeTypeRiskPaySim(String paySimType) {
        if (paySimType == null)
            return NEUTRAL;
        return switch (paySimType.toUpperCase().trim()) {
            case "TRANSFER", "CASH_OUT" -> 1.0;
            case "PAYMENT", "DEBIT" -> 0.2;
            case "CASH_IN" -> 0.0;
            default -> NEUTRAL;
        };
    }

    public static double computeTypeRiskLive(String liveType) {
        if (liveType == null)
            return NEUTRAL;
        return switch (liveType.toUpperCase().trim()) {
            case "TRANSFER_EXTERNAL", "WITHDRAWAL" -> 1.0;
            case "TRANSFER_INTERNAL" -> 0.3;
            case "DEPOSIT" -> 0.0;
            default -> NEUTRAL;
        };
    }
    
    public static double computeHourSuspicionFromStep(int step) {
        int hour = step % 24;
        return hour < NIGHT_HOUR_END ? 1.0 : 0.0;
    }

    public static double computeHourSuspicionFromClock() {
        int hour = java.time.LocalTime.now().getHour();
        return hour < NIGHT_HOUR_END ? 1.0 : 0.0;
    }

    public static double computeNewAccountFlagFromBalance(double oldbalanceOrg, double amount) {
        return (oldbalanceOrg == 0.0 && amount > 0.0) ? 1.0 : 0.0;
    }

    public static double computeNewAccountFlagFromAge(int accountAgeDays) {
        return accountAgeDays < NEW_ACCOUNT_THRESHOLD_DAYS ? 1.0 : 0.0;
    }
}
