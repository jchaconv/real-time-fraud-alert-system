package com.jchacon.banking.frauddetection.producer;

//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.jchacon.banking.frauddetection.entity.OutboxEventEntity;
import com.jchacon.banking.frauddetection.event.TransactionEvent;
//import com.jchacon.banking.frauddetection.model.enums.OutboxEventStatus;
//import com.jchacon.banking.frauddetection.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.NetworkException;
import org.apache.kafka.common.errors.TimeoutException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
//import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudEventProducer {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    /*private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;*/

    private static final String TOPIC = "fraud-detection-events";

    /**
     * The Producer is now just a worker for the Scheduler.
     */
    public Mono<Void> sendToKafka(TransactionEvent event) {
        return Mono.fromFuture(() -> kafkaTemplate.send(TOPIC, event.getTransactionId(), event))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(200)) // Light retries
                        .filter(this::isRecoverable))
                .doOnSuccess(result -> log.info("Event sent to Kafka: {}", event.getTransactionId()))
                .then();
    }

    /**
     * Entry point for the main flow. If Kafka fails, it triggers the initial Outbox save.
     */
    /*public Mono<Void> sendTransactionEvent(TransactionEvent event) {
        return executeKafkaSend(event)
                .then() // Convert to Mono<Void> to match the error resume type
                .onErrorResume(e -> {
                    log.error("CRITICAL: Kafka failed after retries for TXN {}: {}",
                            event.getTransactionId(), e.getMessage());
                    return saveToOutbox(event, e.getMessage());
                });
    }*/

    /**
     * Entry point for the Scheduler. It ONLY tries to send to Kafka.
     * It does NOT save to outbox on failure (the scheduler handles the update).
     */
    public Mono<Void> retryFromOutbox(TransactionEvent event) {
        return executeKafkaSend(event).then();
    }

    /**
     * Core Kafka logic with retries and jitter.
     */
    private Mono<SendResult<String, TransactionEvent>> executeKafkaSend(TransactionEvent event) {
        return Mono.fromFuture(() -> kafkaTemplate.send(TOPIC, event.getTransactionId(), event))
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500))
                        .jitter(0.75)
                        .filter(this::isRecoverable)
                        .doBeforeRetry(retrySignal -> log.warn("Retrying Kafka send... Attempt: {}",
                                retrySignal.totalRetries() + 1))
                )
                .doOnSuccess(result -> log.info("Event sent to Kafka: {} | Topic: {} | Partition: {}",
                        event.getTransactionId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition()));
    }

    /**
     * Persists the failed event into the outbox_events table.
     */
    /*private Mono<Void> saveToOutbox(TransactionEvent event, String errorMessage) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(event))
                .flatMap(payload -> {
                    OutboxEventEntity outboxEvent = OutboxEventEntity.builder()
                            .transactionId(event.getTransactionId())
                            .payload(payload)
                            .status(OutboxEventStatus.FAILED.name())
                            .errorMessage(errorMessage)
                            .retryCount(0)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    return outboxRepository.save(outboxEvent)
                            .doOnSuccess(saved -> log.info("Event successfully saved to Outbox Table for TXN: {}",
                                    event.getTransactionId()))
                            .doOnError(err -> log.error("FATAL: Could not persist event to Outbox Table!", err));
                })
                .onErrorResume(JsonProcessingException.class, e -> {
                    log.error("Serialization error while saving to Outbox: {}", e.getMessage());
                    return Mono.empty();
                })
                .then();
    }*/

    private boolean isRecoverable(Throwable e) {
        return e instanceof TimeoutException || e instanceof NetworkException;
    }
}