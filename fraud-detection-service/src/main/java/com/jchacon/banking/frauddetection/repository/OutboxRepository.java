package com.jchacon.banking.frauddetection.repository;

import com.jchacon.banking.frauddetection.entity.OutboxEventEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface OutboxRepository extends ReactiveCrudRepository<OutboxEventEntity, Long> {

    /**
     * Search for events by state (e.g., 'FAILED') to be processed by the Scheduler.
     * Spring Data R2DBC will handle the "ORDER BY created_at ASC" logic
     */
    Flux<OutboxEventEntity> findByStatusOrderByCreatedAtAsc(String status);
}