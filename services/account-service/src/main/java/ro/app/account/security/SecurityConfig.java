package ro.app.account.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import ro.app.account.security.jwt.JwtAuthenticationFilter;

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
                //ADMIN only endpoints
                .requestMatchers(HttpMethod.GET, "/api/accounts/view").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/accounts/*/freeze").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/accounts/*/close").hasRole("ADMIN")
                //ADMIN & USER endpoints
                .requestMatchers(HttpMethod.GET, "/api/accounts/by-client/*").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.GET, "/api/accounts/by-id/*").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.GET, "/api/accounts/by-iban/*").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.GET, "/api/accounts/*/balance").hasAnyRole("ADMIN", "USER")
                // Internal Stripe settlement (payment-service → shared secret header)
                .requestMatchers(HttpMethod.POST, "/api/internal/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/accounts/open").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.POST, "/api/accounts/transfer").hasAnyRole("ADMIN", "USER")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // No PasswordEncoder — account-service doesn't handle login
    // No AuthenticationManager — JWT validation only
}
