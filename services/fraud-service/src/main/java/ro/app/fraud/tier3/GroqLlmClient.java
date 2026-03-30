package ro.app.fraud.tier3;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "groq")
public class GroqLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GroqLlmClient.class);
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;

    public GroqLlmClient(
            @Value("${app.llm.groq.api-key}") String apiKey,
            @Value("${app.llm.groq.model:llama-3.3-70b-versatile}") String model,
            @Value("${app.llm.timeout-seconds:5}") int timeoutSeconds) {
        this.apiKey = apiKey;
        this.model = model;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutSeconds * 1000);
        factory.setReadTimeout(timeoutSeconds * 1000);
        this.restTemplate = new RestTemplate(factory);
        log.info("GroqLlmClient initialized: model={} timeout={}s", model, timeoutSeconds);
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "temperature", 0.1,
                "max_tokens", 512,
                "response_format", Map.of("type", "json_object")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        ResponseEntity<Map> resp = restTemplate.exchange(
                GROQ_URL, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        if (resp.getBody() == null) {
            throw new RuntimeException("Empty Groq response");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.getBody().get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("No choices in Groq response");
        }

        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");

        log.debug("Groq raw response: {}", content);
        return content;
    }
}
