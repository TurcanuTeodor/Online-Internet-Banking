package ro.app.payment.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import ro.app.payment.security.jwt.JwtAuthenticationFilter;

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
                .requestMatchers("/api/payments/webhook").permitAll() // Stripe — uses Stripe-Signature, no JWT

                // ADMIN only — vizualizare globală (nu există încă, dar e pregătit)
                .requestMatchers(HttpMethod.GET, "/api/payments/all").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/payment-methods/all").hasRole("ADMIN")

                // ADMIN & USER — operații pe propriile date (ownership check în controller)
                .requestMatchers(HttpMethod.POST, "/api/payments/top-up/intent").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.POST, "/api/payments").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.GET, "/api/payments/{id}").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.POST, "/api/payments/*/refund").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.GET, "/api/payments/by-client/*").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.POST, "/api/payment-methods").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.GET, "/api/payment-methods/by-client/*").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.DELETE, "/api/payment-methods/*").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.PUT, "/api/payment-methods/*/set-default").hasAnyRole("ADMIN", "USER")

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // No PasswordEncoder — payment-service doesn't handle login
    // No AuthenticationManager — JWT validation only
}