package ro.app.fraud.tier3;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Teste unitare pentru FraudFeatureEngine — fiecare feature in parte.
 * Verifică atât scalara PSD2 live cât și mapping-ul PaySim pentru antrenament.
 */
class FraudFeatureEngineTest {

    // ── [0] amountRatio ─────────────────────────────────────────────────────

    @Test
    void amountRatio_normalCase() {
        assertEquals(0.5, FraudFeatureEngine.computeAmountRatio(25_000, 50_000), 1e-9);
    }

    @Test
    void amountRatio_cappedAtOne() {
        assertEquals(1.0, FraudFeatureEngine.computeAmountRatio(100_000, 50_000), 1e-9);
    }

    @Test
    void amountRatio_zeroCap_returnsZero() {
        assertEquals(0.0, FraudFeatureEngine.computeAmountRatio(1000, 0), 1e-9);
    }

    @Test
    void amountRatio_zeroAmount_returnsZero() {
        assertEquals(0.0, FraudFeatureEngine.computeAmountRatio(0, 50_000), 1e-9);
    }

    // ── [1] typeRisk (Live PSD2) ────────────────────────────────────────────

    @ParameterizedTest(name = "typeRiskLive(\"{0}\") = {1}")
    @CsvSource({
        "POS_PAYMENT,      0.0",
        "TRANSFER_INTERNAL, 1.0",
        "TRANSFER_EXTERNAL, 3.0",
        "TRANSFER_INSTANT,  3.0"
    })
    void typeRiskLive_knownTypes(String type, double expected) {
        assertEquals(expected, FraudFeatureEngine.computeTypeRiskLive(type), 1e-9);
    }

    @Test
    void typeRiskLive_unknown_returnsDefault() {
        assertEquals(1.0, FraudFeatureEngine.computeTypeRiskLive("CRYPTO_WITHDRAW"), 1e-9);
    }

    @Test
    void typeRiskLive_null_returnsDefault() {
        assertEquals(1.0, FraudFeatureEngine.computeTypeRiskLive(null), 1e-9);
    }

    @Test
    void typeRiskLive_caseInsensitive() {
        assertEquals(3.0, FraudFeatureEngine.computeTypeRiskLive("transfer_instant"), 1e-9);
        assertEquals(0.0, FraudFeatureEngine.computeTypeRiskLive("pos_payment"), 1e-9);
    }

    // ── [1] typeRisk (PaySim) ───────────────────────────────────────────────

    @ParameterizedTest(name = "typeRiskPaySim(\"{0}\") = {1}")
    @CsvSource({
        "CASH_IN,   0.0",
        "PAYMENT,   1.0",
        "DEBIT,     1.0",
        "TRANSFER,  3.0",
        "CASH_OUT,  3.0"
    })
    void typeRiskPaySim_knownTypes(String type, double expected) {
        assertEquals(expected, FraudFeatureEngine.computeTypeRiskPaySim(type), 1e-9);
    }

    @Test
    void typeRiskPaySim_null_returnsDefault() {
        assertEquals(1.0, FraudFeatureEngine.computeTypeRiskPaySim(null), 1e-9);
    }

    // ── [2] hourSuspicion ───────────────────────────────────────────────────

    @ParameterizedTest(name = "hourSuspicion({0}) = {1}")
    @CsvSource({
        "1,  3.0",   // noapte tarziu → risc maxim
        "2,  3.0",
        "3,  3.0",
        "4,  3.0",
        "5,  3.0",
        "0,  2.0",   // tranzitie
        "6,  2.0",
        "7,  2.0",
        "23, 2.0",
        "8,  1.0",   // zi → risc normal
        "12, 1.0",
        "18, 1.0",
        "22, 1.0"
    })
    void hourSuspicion_allBands(int hour, double expected) {
        assertEquals(expected, FraudFeatureEngine.computeHourSuspicionFromClock(hour), 1e-9);
    }

    // ── [3] newAccountFlag ──────────────────────────────────────────────────

    @Test
    void newAccountFlag_newAccount() {
        assertEquals(1.0, FraudFeatureEngine.computeNewAccountFlagFromAge(0), 1e-9);
        assertEquals(1.0, FraudFeatureEngine.computeNewAccountFlagFromAge(29), 1e-9);
    }

    @Test
    void newAccountFlag_oldAccount() {
        assertEquals(0.0, FraudFeatureEngine.computeNewAccountFlagFromAge(30), 1e-9);
        assertEquals(0.0, FraudFeatureEngine.computeNewAccountFlagFromAge(365), 1e-9);
    }

    @Test
    void newAccountFlag_exactBoundary() {
        // 30 zile = nu mai e "nou" (>= threshold)
        assertEquals(0.0, FraudFeatureEngine.computeNewAccountFlagFromAge(30), 1e-9);
    }

    // ── [4] senderDepletionRatio ────────────────────────────────────────────

    @Test
    void senderDepletion_halfBalance() {
        assertEquals(0.5, FraudFeatureEngine.computeSenderDepletionRatio(500, 1000.0), 1e-9);
    }

    @Test
    void senderDepletion_fullDepletion() {
        assertEquals(1.0, FraudFeatureEngine.computeSenderDepletionRatio(1000, 1000.0), 1e-9);
    }

    @Test
    void senderDepletion_overDepletion_cappedAtOne() {
        // Suma mai mare ca soldul → capped la 1.0
        assertEquals(1.0, FraudFeatureEngine.computeSenderDepletionRatio(2000, 1000.0), 1e-9);
    }

    @Test
    void senderDepletion_nullBalance_returnsZero() {
        assertEquals(0.0, FraudFeatureEngine.computeSenderDepletionRatio(500, null), 1e-9);
    }

    @Test
    void senderDepletion_zeroBalance_returnsZero() {
        assertEquals(0.0, FraudFeatureEngine.computeSenderDepletionRatio(500, 0.0), 1e-9);
    }

    @Test
    void senderDepletion_negativeBalance_returnsZero() {
        assertEquals(0.0, FraudFeatureEngine.computeSenderDepletionRatio(500, -100.0), 1e-9);
    }

    // ── [5] isRoundAmount ───────────────────────────────────────────────────

    @Test
    void roundAmount_exact100_isRound() {
        assertEquals(1.0, FraudFeatureEngine.computeRoundAmountFlag(100.0), 1e-9);
    }

    @Test
    void roundAmount_exact500_isRound() {
        assertEquals(1.0, FraudFeatureEngine.computeRoundAmountFlag(500.0), 1e-9);
    }

    @Test
    void roundAmount_exact1000_isRound() {
        assertEquals(1.0, FraudFeatureEngine.computeRoundAmountFlag(1000.0), 1e-9);
    }

    @Test
    void roundAmount_notRound() {
        assertEquals(0.0, FraudFeatureEngine.computeRoundAmountFlag(537.50), 1e-9);
    }

    @Test
    void roundAmount_floatingPointEpsilon() {
        // FIX #10: toleranta epsilon
        assertEquals(1.0, FraudFeatureEngine.computeRoundAmountFlag(500.0000000001), 1e-9);
    }

    @Test
    void roundAmount_zero_isRound() {
        assertEquals(1.0, FraudFeatureEngine.computeRoundAmountFlag(0.0), 1e-9);
    }

    // ── LEGAL_AMOUNT_CAP ────────────────────────────────────────────────────

    @Test
    void legalAmountCap_isCorrectValue() {
        assertEquals(50_000.0, FraudFeatureEngine.LEGAL_AMOUNT_CAP, 1e-9);
    }

    @Test
    void newAccountThreshold_is30Days() {
        assertEquals(30, FraudFeatureEngine.NEW_ACCOUNT_THRESHOLD_DAYS);
    }
}
