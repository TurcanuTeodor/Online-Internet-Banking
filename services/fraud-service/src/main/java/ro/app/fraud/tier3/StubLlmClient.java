package ro.app.fraud.tier3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Fallback LLM client — no external API call.
 * Returns a template-based verdict for development/testing.
 */
@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "stub", matchIfMissing = true)
public class StubLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(StubLlmClient.class);

    @Override
    public String chat(String systemPrompt, String userMessage) {
        log.info("StubLlmClient invoked (no real LLM call)");
        return """
                {
                  "verdict": "ALLOW",
                  "confidence": 0.5,
                  "reasoning": "Stub LLM: no real analysis performed. Transaction forwarded for manual review."
                }
                """;
    }
}
