package com.jchacon.banking.frauddetection.producer;

import com.jchacon.banking.frauddetection.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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
                .doOnSuccess(result -> log.info("Event sent to Kafka: {} | Topic: {} | Partition: {}",
                        event.getTransactionId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition()))
                .doOnError(e -> log.error("Error sending to Kafka", e))
                .then();
    }
}