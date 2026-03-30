package ro.app.fraud.tier3;

/**
 * Abstraction over any LLM provider (Gemini, OpenAI, Claude, Ollama, stub).
 * Implementations are selected via Spring @ConditionalOnProperty.
 */
public interface LlmClient {

    String chat(String systemPrompt, String userMessage);
}
