package com.jchacon.banking.frauddetection.config;

import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

@Configuration
public class ObservationConfig {

    @PostConstruct
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
    }
}
