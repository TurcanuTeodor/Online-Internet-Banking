package ro.app.banking.security.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ro.app.banking.security.CustomUserDetailsService;
import ro.app.banking.security.UserPrincipal;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override //method that is automatically exe for each http
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader= request.getHeader("Authorization"); //get auth header bc JWT is sent there
        if(authHeader == null || !authHeader.startsWith("Bearer ")){ //check for a valid JWT if header exists
            filterChain.doFilter(request, response); //not all req need auth; let the request move on
            return;
        }

        String token = authHeader.substring(7); //cut the Bearer string

        if(jwtService.isValid(token) && SecurityContextHolder.getContext().getAuthentication() == null){ //SecurityContextHolder= where Spring Secur keeps the current user so it needs to be null/no auth
            Claims claims= jwtService.parseClaims(token); //extract info from token

            Object twofa = claims.get("2fa");
            if (twofa == null || !"ok".equals(twofa)) {
                //temp or non-2FA-validated tokens must not authenticate requests
                filterChain.doFilter(request, response);
                return;
            }

            String subject = claims.getSubject(); //username/email

            UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(subject); //find user +details in db using subject
            
            UsernamePasswordAuthenticationToken authToken= new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()); //proof for user auth; password is null bc JWT is valid already
                
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); //add request details (IP, session id etc)

            SecurityContextHolder.getContext().setAuthentication(authToken); //Spring Secur is notified about auth req and principal user, later needed for access to other methods
        }

        filterChain.doFilter(request, response); //req needs to go to controller, endpoint and other
    }
     
}
