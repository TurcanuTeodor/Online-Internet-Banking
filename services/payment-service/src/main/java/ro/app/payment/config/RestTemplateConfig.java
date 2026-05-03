package ro.app.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClient;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Modern RestClient for Spring Boot 3.2+ (replaces RestTemplate)
     * Provides fluent, type-safe HTTP client API with built-in observation support
     */
    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }
}
