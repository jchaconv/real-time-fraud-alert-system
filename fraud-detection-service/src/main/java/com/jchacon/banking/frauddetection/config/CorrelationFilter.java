package com.jchacon.banking.frauddetection.config;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

@Component
public class CorrelationFilter implements WebFilter {

    public static final String CORRELATION_ID_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders()
                .getFirst("X-Correlation-ID");

        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        exchange.getResponse().getHeaders().add("X-Correlation-ID", correlationId);

        // This writes the ID into the Reactor Context
        return chain.filter(exchange)
                .contextWrite(Context.of(CORRELATION_ID_KEY, correlationId));
    }
}
