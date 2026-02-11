package com.jchacon.banking.frauddetection.producer;

import com.jchacon.banking.frauddetection.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.NetworkException;
import org.apache.kafka.common.errors.TimeoutException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudEventProducer {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    private static final String TOPIC = "fraud-detection-events";

    /**
     * Publishes the transaction event to Kafka.
     * Micrometer Tracing will automatically inject the 'traceparent' header
     * if the ObservationRegistry is properly configured.
     */
    public Mono<Void> sendTransactionEvent(TransactionEvent event) {
        return Mono.fromFuture(() -> kafkaTemplate.send(TOPIC, event.getTransactionId(), event))
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500)) // 3 retries, starting at 500ms
                        .jitter(0.75) // Avoid "thundering herd" problem
                        .filter(this::isRecoverable) // Only retry on technical errors
                        .doBeforeRetry(retrySignal -> log.warn("Retrying Kafka send... Attempt: {}", retrySignal.totalRetries() + 1))
                )
                .doOnSuccess(result -> log.info("Event sent to Kafka: {} | Topic: {} | Partition: {}",
                        event.getTransactionId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition()))
                .onErrorResume(e -> {
                    log.error("CRITICAL: Kafka failed after retries for TXN {}: {}",
                            event.getTransactionId(), e.getMessage());
                    // In banking, here you might save to a "Dead Letter Table" in DB
                    return Mono.empty();
                })
                .then();
    }

    private boolean isRecoverable(Throwable e) {
        // Only retry on timeouts or connection issues, not on business or serialization errors
        return e instanceof TimeoutException ||
                e instanceof NetworkException;
    }
}