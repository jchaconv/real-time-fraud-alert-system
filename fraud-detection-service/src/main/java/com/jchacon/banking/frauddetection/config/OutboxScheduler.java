package com.jchacon.banking.frauddetection.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jchacon.banking.frauddetection.entity.OutboxEventEntity;
import com.jchacon.banking.frauddetection.event.TransactionEvent;
import com.jchacon.banking.frauddetection.model.enums.OutboxEventStatus;
import com.jchacon.banking.frauddetection.producer.FraudEventProducer;
import com.jchacon.banking.frauddetection.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxRepository outboxRepository;
    private final FraudEventProducer fraudEventProducer;
    private final ObjectMapper objectMapper;

    /**
     * Polls the outbox table for failed events every 5 seconds.
     * In a production environment, this interval could be adjusted via properties.
     */
    @Scheduled(fixedDelayString = "${app.scheduler.outbox-retry-ms:5000}")
    public void processFailedEvents() {
        outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.FAILED.name())
                .concatMap(this::retryEvent) // concatMap maintains the order by processing one-by-one
                .subscribe(); // WebFlux requires manual subscription for background tasks
    }

    private Mono<Void> retryEvent(OutboxEventEntity entity) {
        return Mono.fromCallable(() -> objectMapper.readValue(entity.getPayload(), TransactionEvent.class))
                .flatMap(event -> {
                    log.info("Outbox Scheduler: Retrying TXN {}", entity.getTransactionId());
                    return fraudEventProducer.retryFromOutbox(event)
                            // If successfully sent, delete from outbox to keep table clean
                            .then(outboxRepository.delete(entity))
                            .doOnSuccess(v -> log.info("Outbox Scheduler: TXN {} processed and removed", entity.getTransactionId()));
                })
                .onErrorResume(e -> {
                    log.error("Outbox Scheduler: Retry failed for ID {}: {}", entity.getId(), e.getMessage());
                    entity.setRetryCount(entity.getRetryCount() + 1);
                    entity.setUpdatedAt(LocalDateTime.now());

                    // If it fails too many times, stop retrying automatically
                    if (entity.getRetryCount() >= 10) {
                        entity.setStatus(OutboxEventStatus.FATAL_ERROR.name());
                        log.error("Outbox Scheduler: Max retries reached for TXN {}. Manual intervention required.", entity.getTransactionId());
                    }
                    return outboxRepository.save(entity).then();
                });
    }
}