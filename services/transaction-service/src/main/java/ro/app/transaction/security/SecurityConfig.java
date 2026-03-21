package ro.app.transaction.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import ro.app.transaction.security.jwt.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                // Internal deposit from account-service (shared secret header)
                .requestMatchers("/api/internal/**").permitAll()
                // ADMIN only endpoints
                .requestMatchers(HttpMethod.GET, "/api/transactions/view-all").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/transactions/flagged").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/transactions/daily-totals").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/transactions/by-type/*").hasRole("ADMIN")
                // ADMIN & USER endpoints
                .requestMatchers(HttpMethod.GET, "/api/transactions/by-account/*").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.GET, "/api/transactions/by-accounts").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.GET, "/api/transactions/by-client/*").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.GET, "/api/transactions/by-iban/*").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.GET, "/api/transactions/between").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.GET, "/api/transactions/*").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.POST, "/api/transactions").hasAnyRole("ADMIN", "USER")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // No PasswordEncoder — transaction-service doesn't handle login
    // No AuthenticationManager — JWT validation only
}
