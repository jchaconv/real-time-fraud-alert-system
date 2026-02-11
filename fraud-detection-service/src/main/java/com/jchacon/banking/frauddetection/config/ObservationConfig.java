package com.jchacon.banking.frauddetection.config;

import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

/**
 * Modern Observation Configuration for Spring Boot 3.4+.
 * Ensures Trace IDs propagate through Reactive Streams (Flux/Mono).
 */
@RequiredArgsConstructor
@Configuration
public class ObservationConfig {

    private final ObservationRegistry observationRegistry;

    @PostConstruct
    public void init() {
        // Essential: Enables automatic context propagation for Project Reactor
        // This is what bridges Micrometer Trace IDs with MDC in WebFlux
        Hooks.enableAutomaticContextPropagation();
    }

    /*@PostConstruct
    public void init() {
        // Map Reactor Context key to MDC key
        ContextRegistry.getInstance().registerThreadLocalAccessor(
                CorrelationFilter.CORRELATION_ID_KEY,
                () -> MDC.get(CorrelationFilter.CORRELATION_ID_KEY),
                value -> MDC.put(CorrelationFilter.CORRELATION_ID_KEY, value),
                () -> MDC.remove(CorrelationFilter.CORRELATION_ID_KEY)
        );

        // Enable automatic propagation through all operators
        Hooks.enableAutomaticContextPropagation();
    }*/
}
