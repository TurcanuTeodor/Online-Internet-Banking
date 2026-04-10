package ro.app.fraud.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import ro.app.fraud.security.jwt.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final InternalApiAuthFilter internalApiAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter, InternalApiAuthFilter internalApiAuthFilter) {
        this.jwtFilter = jwtFilter;
        this.internalApiAuthFilter = internalApiAuthFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/internal/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/fraud/health").permitAll()
                // Admin-only: dashboard data, alerts, decisions
                .requestMatchers(HttpMethod.GET, "/api/fraud/decisions/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/fraud/alerts/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/fraud/decisions/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(internalApiAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
