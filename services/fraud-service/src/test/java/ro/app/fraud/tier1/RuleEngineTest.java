package ro.app.fraud.tier1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.model.enums.FraudDecisionStatus;
import ro.app.fraud.repository.FraudDecisionRepository;

@ExtendWith(MockitoExtension.class)
class RuleEngineTest {

    @Mock
    FraudDecisionRepository decisionRepo;
    
    @InjectMocks
    RuleEngine ruleEngine;

    @Test
    void normalTransaction_returnsAllow() {
        FraudEvaluationRequest req = buildRequest(500.0, false, 60);
        when(decisionRepo.countByAccountIdAndCreatedAtAfter(any(), any())).thenReturn(0L);
        
        RuleResult result = ruleEngine.evaluate(req);
        
        assertEquals(FraudDecisionStatus.ALLOW, result.status());
    }

    @Test
    void largeAmount_triggersStepUp() {
        FraudEvaluationRequest req = buildRequest(15000.0, false, 60);
        when(decisionRepo.countByAccountIdAndCreatedAtAfter(any(), any())).thenReturn(0L);
        
        RuleResult result = ruleEngine.evaluate(req);
        
        assertEquals(FraudDecisionStatus.STEP_UP_REQUIRED, result.status());
        assertTrue(result.ruleHits().contains("LARGE_AMOUNT"));
    }

    @Test
    void selfTransfer_alwaysAllow() {
        FraudEvaluationRequest req = buildRequest(50000.0, true, 1);
        
        RuleResult result = ruleEngine.evaluate(req);
        
        assertEquals(FraudDecisionStatus.ALLOW, result.status());
    }
    
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
