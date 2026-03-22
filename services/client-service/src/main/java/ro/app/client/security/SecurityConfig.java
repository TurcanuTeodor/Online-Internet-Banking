package ro.app.client.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import ro.app.client.security.jwt.JwtAuthenticationFilter;

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
                    // Public self-service: create client profile before auth register (links clientId)
                    .requestMatchers(HttpMethod.POST, "/api/clients/sign-up").permitAll()
                    // ADMIN only
                    .requestMatchers(HttpMethod.POST, "/api/clients").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/clients/*").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/clients/*/suspend").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/clients/view").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/search").hasRole("ADMIN")
                    // ADMIN & USER
                    .requestMatchers(HttpMethod.GET, "/api/clients/*/summary").hasAnyRole("ADMIN", "USER")
                    .requestMatchers(HttpMethod.PUT, "/api/clients/*/contact").hasAnyRole("ADMIN", "USER")
                    // GDPR — ownership check în controller
                    .requestMatchers("/api/gdpr/**").hasAnyRole("ADMIN", "USER")
                    .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // No PasswordEncoder — user-service doesn't handle login
    // No AuthenticationManager — JWT validation only
}
