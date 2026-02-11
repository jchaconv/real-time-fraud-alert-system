package com.jchacon.banking.frauddetection.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jchacon.banking.frauddetection.model.ErrorResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class SecurityExceptionHandler implements ServerAuthenticationEntryPoint, ServerAccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Handles 401 Unauthorized
    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        return writeResponse(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid or missing token");
    }

    // Handles 403 Forbidden
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException ex) {
        return writeResponse(exchange, HttpStatus.FORBIDDEN, "FORBIDDEN", "Insufficient permissions for this operation");
    }

    /**
     * Writes a standardized ErrorResponse to the output stream.
     * Ensures security errors match the global business error format.
     */
    private Mono<Void> writeResponse(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Create the same DTO used in your GlobalExceptionHandler
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(code)
                .message(message)
                .responseCode("99") // Banking standard for non-business failures
                .build();

        try {
            // Serialize the DTO using the injected ObjectMapper
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);

            // Write the buffer to the reactive response stream
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            // If serialization fails, escalate to the reactive stream
            return Mono.error(new RuntimeException("Error serializing security response", e));
        }
    }
}