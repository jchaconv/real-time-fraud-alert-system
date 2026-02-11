package com.jchacon.banking.frauddetection.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security configuration for the Fraud Detection Resource Server.
 * Using WebFlux Security for reactive non-blocking support.
 */
@RequiredArgsConstructor
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final SecurityExceptionHandler securityExceptionHandler;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                // Disable CSRF as we are using stateless JWT authentication
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Permit public access to Actuator endpoints for monitoring (Kibana/Dynatrace)
                        .pathMatchers("/actuator/**").permitAll()
                        // Enforce specific Scope for transaction processing
                        // Note: Spring Security automatically prefixes scopes with 'SCOPE_'
                        .pathMatchers("/api/v1/transactions/**").hasAuthority("SCOPE_transaction:write")
                        // Any other exchange requires a valid authenticated user/client
                        .anyExchange().authenticated()
                )
                // Register custom exception handling
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(securityExceptionHandler) // 401
                        .accessDeniedHandler(securityExceptionHandler)      // 403
                )
                // Configure the application as an OAuth2 Resource Server to validate JWTs
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                );
        return http.build();
    }
}