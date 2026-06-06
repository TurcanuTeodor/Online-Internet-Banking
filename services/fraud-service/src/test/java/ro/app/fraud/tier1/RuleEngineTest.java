package ro.app.fraud.tier1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.MockitoJUnitRunner;

import ro.app.fraud.config.FraudProperties;
import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.model.enums.FraudDecisionStatus;
import ro.app.fraud.repository.FraudDecisionRepository;

/**
 * Teste unitare pentru RuleEngine (JUnit 4).
 *
 * Pattern testat: Strategy (Behavioral) — motorul de reguli selecteaza actiunea
 * (ALLOW, STEP_UP_REQUIRED, BLOCK) in functie de caracteristicile tranzactiei.
 *
 * Tehnici aplicate:
 * - Echivalenta de clase: tranzactie normala, suma mare, auto-transfer
 * - Analiza valorilor limita: prag de suma (10000 EUR)
 */
@RunWith(MockitoJUnitRunner.class)
public class RuleEngineTest {

    @Mock
    FraudDecisionRepository decisionRepo;

    FraudProperties fraudProperties;
    RuleEngine ruleEngine;

    @Before
    public void setUp() {
        fraudProperties = new FraudProperties();
        FraudProperties.Tier1 tier1 = new FraudProperties.Tier1();
        tier1.setLargeAmountThreshold(10_000.0);
        tier1.setBurstLimit(5);
        tier1.setNewAccountAgeDays(30);
        fraudProperties.setTier1(tier1);

        ruleEngine = new RuleEngine(decisionRepo, fraudProperties);
    }

    @Test
    public void normalTransaction_returnsAllow() {
        // Arrange — suma mica, cont vechi, fara burst
        FraudEvaluationRequest req = buildRequest(500.0, false, 60);
        when(decisionRepo.countByAccountIdAndCreatedAtAfter(any(), any())).thenReturn(0L);

        // Act
        RuleResult result = ruleEngine.evaluate(req);

        // Assert
        assertEquals(FraudDecisionStatus.ALLOW, result.status());
    }

    @Test
    public void largeAmount_triggersStepUp() {
        // Arrange — suma mare (15000 > prag 10000)
        FraudEvaluationRequest req = buildRequest(15_000.0, false, 60);
        when(decisionRepo.countByAccountIdAndCreatedAtAfter(any(), any())).thenReturn(0L);

        // Act
        RuleResult result = ruleEngine.evaluate(req);

        // Assert
        assertEquals(FraudDecisionStatus.STEP_UP_REQUIRED, result.status());
        assertTrue("LARGE_AMOUNT rule must be in rule hits",
                result.ruleHits().contains("LARGE_AMOUNT"));
    }

    @Test
    public void selfTransfer_alwaysAllow_regardlessOfAmount() {
        // Arrange — auto-transfer: indiferent de suma, trebuie sa fie ALLOW
        FraudEvaluationRequest req = buildRequest(50_000.0, true, 1);

        // Act — fara stub pentru decisionRepo (nu se ajunge la reguli burst/amount pentru self-transfer)
        RuleResult result = ruleEngine.evaluate(req);

        // Assert
        assertEquals("Self-transfer must always be ALLOW",
                FraudDecisionStatus.ALLOW, result.status());
    }

    @Test
    public void amountExactlyAtThreshold_returnsAllow() {
        // Arrange — Boundary: suma exact la prag (10000 == threshold)
        FraudEvaluationRequest req = buildRequest(10_000.0, false, 60);
        when(decisionRepo.countByAccountIdAndCreatedAtAfter(any(), any())).thenReturn(0L);

        // Act
        RuleResult result = ruleEngine.evaluate(req);

        // Assert — la prag (nu strict mai mare), nu se aplica step-up
        assertEquals(FraudDecisionStatus.ALLOW, result.status());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private FraudEvaluationRequest buildRequest(double amount, boolean selfTransfer, int accountAgeDays) {
        FraudEvaluationRequest req = new FraudEvaluationRequest();
        req.setAccountId(1L);
        req.setClientId(1L);
        req.setAmount(amount);
        req.setSelfTransfer(selfTransfer);
        req.setAccountAgeDays(accountAgeDays);
        req.setTransactionType("TRANSFER_INTERNAL");
        return req;
    }
}
