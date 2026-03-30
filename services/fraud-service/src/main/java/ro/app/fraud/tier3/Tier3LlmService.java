package ro.app.fraud.tier3;

import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ro.app.fraud.dto.FraudEvaluationRequest;
import ro.app.fraud.tier2.ScoringResult;

@Service
public class Tier3LlmService {

    private static final Logger log = LoggerFactory.getLogger(Tier3LlmService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final LlmClient llmClient;

    public Tier3LlmService(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    private static final String SYSTEM_PROMPT = """
            You are a senior banking fraud analyst at a European digital bank.
            You receive transaction data and a behavioral risk scoring breakdown.
            Your task is to decide whether the transaction should be ALLOWED or FLAGGED for manual review.

            Rules:
            - Respond ONLY with a valid JSON object, no markdown, no extra text.
            - JSON schema: {"verdict": "ALLOW" or "FLAG", "confidence": 0.0 to 1.0, "reasoning": "2-3 sentence explanation"}
            - Be conservative: if in doubt, FLAG the transaction.
            - Consider the scoring breakdown, transaction amount, recipient familiarity, time of day, and account age.
            - Do not hallucinate facts. Base your reasoning strictly on the provided data.
            """;

    public LlmVerdict analyze(FraudEvaluationRequest req, ScoringResult scoring) {
        try {
            String userMessage = buildUserMessage(req, scoring);
            log.info("Tier3 LLM call: client={} amount={} score={}", req.getClientId(), req.getAmount(), scoring.totalScore());

            String rawResponse = llmClient.chat(SYSTEM_PROMPT, userMessage);
            log.info("Tier3 raw LLM response: {}", rawResponse);

            return parseVerdict(rawResponse);
        } catch (Exception e) {
            log.error("Tier3 LLM failed, defaulting to ALLOW: {}", e.getMessage());
            return new LlmVerdict("ALLOW", 0.0, "Tier3 LLM unavailable: " + e.getMessage());
        }
    }

    private String buildUserMessage(FraudEvaluationRequest req, ScoringResult scoring) {
        String components = scoring.componentScores().entrySet().stream()
                .map(e -> "  - " + e.getKey() + ": " + String.format("%.1f", e.getValue()) + "/100")
                .collect(Collectors.joining("\n"));

        return String.format("""
                TRANSACTION UNDER REVIEW:
                - Amount: %.2f %s
                - Type: %s
                - Sender IBAN: %s
                - Receiver IBAN: %s
                - Self-transfer: %s
                - Account age: %d days

                TIER 2 BEHAVIORAL SCORING (0-100 scale, higher = riskier):
                - Total risk score: %.1f / 100
                %s

                TIER 2 SUMMARY: %s

                Based on this data, should this transaction be ALLOWED or FLAGGED?
                """,
                req.getAmount(),
                req.getCurrency() != null ? req.getCurrency() : "EUR",
                req.getTransactionType(),
                req.getSenderIban() != null ? req.getSenderIban() : "N/A",
                req.getReceiverIban() != null ? req.getReceiverIban() : "N/A",
                req.isSelfTransfer() ? "yes" : "no",
                req.getAccountAgeDays(),
                scoring.totalScore(),
                components,
                scoring.summary());
    }

    private LlmVerdict parseVerdict(String raw) {
        try {
            String cleaned = raw.strip();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```\\w*\\n?", "").replaceAll("\\n?```$", "").strip();
            }

            JsonNode node = mapper.readTree(cleaned);

            String verdict = node.has("verdict") ? node.get("verdict").asText("ALLOW") : "ALLOW";
            double confidence = node.has("confidence") ? node.get("confidence").asDouble(0.5) : 0.5;
            String reasoning = node.has("reasoning") ? node.get("reasoning").asText("No reasoning provided") : "No reasoning provided";

            if (!"ALLOW".equalsIgnoreCase(verdict) && !"FLAG".equalsIgnoreCase(verdict) && !"BLOCK".equalsIgnoreCase(verdict)) {
                log.warn("Unexpected LLM verdict '{}', defaulting to ALLOW", verdict);
                verdict = "ALLOW";
            }

            return new LlmVerdict(verdict.toUpperCase(), confidence, reasoning);
        } catch (Exception e) {
            log.error("Failed to parse LLM JSON: {}", e.getMessage());
            return new LlmVerdict("ALLOW", 0.0, "Parse error: " + e.getMessage());
        }
    }
}
