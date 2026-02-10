package com.jchacon.banking.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
public class KafkaConsumerConfig {

    /**
     * Configures the error handling for Kafka consumers.
     * If a message fails, it will retry 3 times every 2 seconds.
     * After exhaustion, it will be sent to a .DLT (Dead Letter Topic).
     */
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> template) {
        // Retry policy: 3 attempts, 2000ms interval
        var backOff = new FixedBackOff(2000L, 3);

        // Recoverer that sends the failed message to a topic named: originalTopic-dlt
        var recoverer = new DeadLetterPublishingRecoverer(template);

        return new DefaultErrorHandler(recoverer, backOff);
    }
}