package ro.app.fraud.tier3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Teste unitare pentru FraudFeatureEngine (JUnit 4).
 *
 * Fiecare feature este testat cu metode @Test individuale (fara @ParameterizedTest).
 * Aceasta abordare este mai clara si mai usor de inteles la prezentari.
 *
 * Pattern testat: Strategy (Behavioral) — fiecare compute*() este o strategie
 * de calcul a unui feature din vectorul ML.
 *
 * Acoperire:
 *   [0] amountRatio — normalizare, cap, zero
 *   [1] typeRisk (Live PSD2) — toate tipurile cunoscute + unknown + null + case-insensitive
 *   [1] typeRisk (PaySim) — toate tipurile cunoscute + null
 *   [2] hourSuspicion — toate intervalele orare
 *   [3] newAccountFlag — cont nou vs. vechi, valoare limita
 *   [4] senderDepletionRatio — jumatate, total, depasire, null, zero, negativ
 *   [5] isRoundAmount — rotund, nerotund, epsilon, zero
 *   Constante publice
 */
@RunWith(JUnit4.class)
public class FraudFeatureEngineTest {

    // ── [0] amountRatio ─────────────────────────────────────────────────────

    @Test
    public void amountRatio_normalCase_returnsHalf() {
        assertEquals(0.5, FraudFeatureEngine.computeAmountRatio(25_000, 50_000), 1e-9);
    }

    @Test
    public void amountRatio_exceedsCap_cappedAtOne() {
        assertEquals(1.0, FraudFeatureEngine.computeAmountRatio(100_000, 50_000), 1e-9);
    }

    @Test
    public void amountRatio_zeroCapAmount_returnsZero() {
        // Boundary: impartire la zero => 0.0
        assertEquals(0.0, FraudFeatureEngine.computeAmountRatio(1000, 0), 1e-9);
    }

    @Test
    public void amountRatio_zeroTransactionAmount_returnsZero() {
        assertEquals(0.0, FraudFeatureEngine.computeAmountRatio(0, 50_000), 1e-9);
    }

    // ── [1] typeRiskLive — tipuri cunoscute ──────────────────────────────────

    @Test
    public void typeRiskLive_posPayment_returnsZero() {
        assertEquals(0.0, FraudFeatureEngine.computeTypeRiskLive("POS_PAYMENT"), 1e-9);
    }

    @Test
    public void typeRiskLive_transferInternal_returnsOne() {
        assertEquals(1.0, FraudFeatureEngine.computeTypeRiskLive("TR_INT"), 1e-9);
    }

    @Test
    public void typeRiskLive_transferExternal_returnsThree() {
        // FIX #1a: TRANSFER_EXTERNAL trebuie sa returneze 3.0 (nu 2.0)
        assertEquals(3.0, FraudFeatureEngine.computeTypeRiskLive("TR_EXT"), 1e-9);
    }

    @Test
    public void typeRiskLive_transferInstant_returnsThree() {
        assertEquals(3.0, FraudFeatureEngine.computeTypeRiskLive("TRANSFER_INSTANT"), 1e-9);
    }

    @Test
    public void typeRiskLive_unknownType_returnsDefault() {
        assertEquals(1.0, FraudFeatureEngine.computeTypeRiskLive("CRYPTO_WITHDRAW"), 1e-9);
    }

    @Test
    public void typeRiskLive_nullType_returnsDefault() {
        assertEquals(1.0, FraudFeatureEngine.computeTypeRiskLive(null), 1e-9);
    }

    @Test
    public void typeRiskLive_lowercaseTransferInstant_returnsThree() {
        // Test case-insensitive
        assertEquals(3.0, FraudFeatureEngine.computeTypeRiskLive("transfer_instant"), 1e-9);
    }

    @Test
    public void typeRiskLive_lowercasePosPayment_returnsZero() {
        assertEquals(0.0, FraudFeatureEngine.computeTypeRiskLive("pos_payment"), 1e-9);
    }

    // ── [1] typeRiskPaySim — tipuri PaySim ──────────────────────────────────

    @Test
    public void typeRiskPaySim_cashIn_returnsZero() {
        assertEquals(0.0, FraudFeatureEngine.computeTypeRiskPaySim("CASH_IN"), 1e-9);
    }

    @Test
    public void typeRiskPaySim_payment_returnsOne() {
        assertEquals(1.0, FraudFeatureEngine.computeTypeRiskPaySim("PAYMENT"), 1e-9);
    }

    @Test
    public void typeRiskPaySim_debit_returnsOne() {
        assertEquals(1.0, FraudFeatureEngine.computeTypeRiskPaySim("DEBIT"), 1e-9);
    }

    @Test
    public void typeRiskPaySim_transfer_returnsThree() {
        assertEquals(3.0, FraudFeatureEngine.computeTypeRiskPaySim("TRANSFER"), 1e-9);
    }

    @Test
    public void typeRiskPaySim_cashOut_returnsThree() {
        assertEquals(3.0, FraudFeatureEngine.computeTypeRiskPaySim("CASH_OUT"), 1e-9);
    }

    @Test
    public void typeRiskPaySim_nullType_returnsDefault() {
        assertEquals(1.0, FraudFeatureEngine.computeTypeRiskPaySim(null), 1e-9);
    }

    // ── [2] hourSuspicion — intervale orare ──────────────────────────────────

    @Test
    public void hourSuspicion_hour1_returnsThree() {
        // Noapte tarziu (1-5) = risc maxim 3.0
        assertEquals(3.0, FraudFeatureEngine.computeHourSuspicionFromClock(1), 1e-9);
    }

    @Test
    public void hourSuspicion_hour2_returnsThree() {
        assertEquals(3.0, FraudFeatureEngine.computeHourSuspicionFromClock(2), 1e-9);
    }

    @Test
    public void hourSuspicion_hour3_returnsThree() {
        assertEquals(3.0, FraudFeatureEngine.computeHourSuspicionFromClock(3), 1e-9);
    }

    @Test
    public void hourSuspicion_hour4_returnsThree() {
        assertEquals(3.0, FraudFeatureEngine.computeHourSuspicionFromClock(4), 1e-9);
    }

    @Test
    public void hourSuspicion_hour5_returnsThree() {
        assertEquals(3.0, FraudFeatureEngine.computeHourSuspicionFromClock(5), 1e-9);
    }

    @Test
    public void hourSuspicion_hour0_returnsTwo() {
        // Tranzitie (0, 6-7, 23) = risc mediu 2.0
        assertEquals(2.0, FraudFeatureEngine.computeHourSuspicionFromClock(0), 1e-9);
    }

    @Test
    public void hourSuspicion_hour6_returnsTwo() {
        assertEquals(2.0, FraudFeatureEngine.computeHourSuspicionFromClock(6), 1e-9);
    }

    @Test
    public void hourSuspicion_hour7_returnsTwo() {
        assertEquals(2.0, FraudFeatureEngine.computeHourSuspicionFromClock(7), 1e-9);
    }

    @Test
    public void hourSuspicion_hour23_returnsTwo() {
        assertEquals(2.0, FraudFeatureEngine.computeHourSuspicionFromClock(23), 1e-9);
    }

    @Test
    public void hourSuspicion_hour8_returnsOne() {
        // Zi (8-22) = risc normal 1.0
        assertEquals(1.0, FraudFeatureEngine.computeHourSuspicionFromClock(8), 1e-9);
    }

    @Test
    public void hourSuspicion_hour12_returnsOne() {
        assertEquals(1.0, FraudFeatureEngine.computeHourSuspicionFromClock(12), 1e-9);
    }

    @Test
    public void hourSuspicion_hour18_returnsOne() {
        assertEquals(1.0, FraudFeatureEngine.computeHourSuspicionFromClock(18), 1e-9);
    }

    @Test
    public void hourSuspicion_hour22_returnsOne() {
        assertEquals(1.0, FraudFeatureEngine.computeHourSuspicionFromClock(22), 1e-9);
    }

    // ── [3] newAccountFlag ──────────────────────────────────────────────────

    @Test
    public void newAccountFlag_day0_isNew() {
        assertEquals(1.0, FraudFeatureEngine.computeNewAccountFlagFromAge(0), 1e-9);
    }

    @Test
    public void newAccountFlag_day29_isNew() {
        // Boundary: 29 < 30 => nou
        assertEquals(1.0, FraudFeatureEngine.computeNewAccountFlagFromAge(29), 1e-9);
    }

    @Test
    public void newAccountFlag_day30_isOld() {
        // Boundary: 30 >= 30 => nu mai e nou
        assertEquals(0.0, FraudFeatureEngine.computeNewAccountFlagFromAge(30), 1e-9);
    }

    @Test
    public void newAccountFlag_day365_isOld() {
        assertEquals(0.0, FraudFeatureEngine.computeNewAccountFlagFromAge(365), 1e-9);
    }

    // ── [4] senderDepletionRatio ─────────────────────────────────────────────

    @Test
    public void senderDepletion_halfBalance_returnsHalf() {
        assertEquals(0.5, FraudFeatureEngine.computeSenderDepletionRatio(500, 1000.0), 1e-9);
    }

    @Test
    public void senderDepletion_fullDepletion_returnsOne() {
        assertEquals(1.0, FraudFeatureEngine.computeSenderDepletionRatio(1000, 1000.0), 1e-9);
    }

    @Test
    public void senderDepletion_overDepletion_cappedAtOne() {
        // Boundary: suma > sold => capped la 1.0
        assertEquals(1.0, FraudFeatureEngine.computeSenderDepletionRatio(2000, 1000.0), 1e-9);
    }

    @Test
    public void senderDepletion_nullBalance_returnsZero() {
        assertEquals(0.0, FraudFeatureEngine.computeSenderDepletionRatio(500, null), 1e-9);
    }

    @Test
    public void senderDepletion_zeroBalance_returnsZero() {
        // Boundary: sold zero => impartire la zero => 0.0
        assertEquals(0.0, FraudFeatureEngine.computeSenderDepletionRatio(500, 0.0), 1e-9);
    }

    @Test
    public void senderDepletion_negativeBalance_returnsZero() {
        assertEquals(0.0, FraudFeatureEngine.computeSenderDepletionRatio(500, -100.0), 1e-9);
    }

    // ── [5] isRoundAmount ────────────────────────────────────────────────────

    @Test
    public void roundAmount_100_isRound() {
        assertEquals(1.0, FraudFeatureEngine.computeRoundAmountFlag(100.0), 1e-9);
    }

    @Test
    public void roundAmount_500_isRound() {
        assertEquals(1.0, FraudFeatureEngine.computeRoundAmountFlag(500.0), 1e-9);
    }

    @Test
    public void roundAmount_1000_isRound() {
        assertEquals(1.0, FraudFeatureEngine.computeRoundAmountFlag(1000.0), 1e-9);
    }

    @Test
    public void roundAmount_53750_isNotRound() {
        assertEquals(0.0, FraudFeatureEngine.computeRoundAmountFlag(537.50), 1e-9);
    }

    @Test
    public void roundAmount_floatingPointEpsilon_detectedAsRound() {
        // FIX #10: toleranta epsilon pentru erori de conversie valutara
        assertEquals(1.0, FraudFeatureEngine.computeRoundAmountFlag(500.0000000001), 1e-9);
    }

    @Test
    public void roundAmount_zero_isRound() {
        assertEquals(1.0, FraudFeatureEngine.computeRoundAmountFlag(0.0), 1e-9);
    }

    // ── Constante publice ────────────────────────────────────────────────────

    @Test
    public void legalAmountCap_is50000() {
        assertEquals(50_000.0, FraudFeatureEngine.LEGAL_AMOUNT_CAP, 1e-9);
    }

    @Test
    public void newAccountThreshold_is30Days() {
        assertEquals(30, FraudFeatureEngine.NEW_ACCOUNT_THRESHOLD_DAYS);
    }
}
