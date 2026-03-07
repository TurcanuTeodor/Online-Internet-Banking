package ro.app.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import ro.app.gateway.security.JwtService;

@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService){
        super(Config.class);
        this.jwtService = jwtService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            // lipseste headerul de autorizare
            if(authHeader == null || !authHeader.startsWith("Bearer ")){
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7);

            // token invalid
            if(!jwtService.isValid(token)){
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // token valid -> continua catre serviciu
            return chain.filter(exchange);
        };
    }

    // Clasa de configurare necesara pt AbstractGatewayFilterFactory
    public static class Config {
        // poate fi extinsa cu parametri (ex: roles)  pe viitor
    }
}
