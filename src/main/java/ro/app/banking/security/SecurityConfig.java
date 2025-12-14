package ro.app.banking.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import ro.app.banking.security.jwt.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }
    
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{

        http.csrf(csrf-> csrf.disable()) //disable Cross-Site Request Forgery(html form and session) bc i use jwt and rest api
            .authorizeHttpRequests(auth-> auth.requestMatchers("/api/auth/**") 
                                              .permitAll() //define who can access endpoints without token
                                              .anyRequest()
                                              .authenticated() //any other req needs auth w valid JWT
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class); //run the jwt filter before the login filter

        return http.build(); //builds the security system
    }
    
    @Bean 
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception{ 
        //identity verification only for username + password (like a security guard)
        return config.getAuthenticationManager();
    }
}
