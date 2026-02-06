package com.jchacon.banking.frauddetection.repository;

import com.jchacon.banking.frauddetection.model.TransactionEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface TransactionRepository extends ReactiveCrudRepository<TransactionEntity, UUID> {

    // Retrieves the customer's transaction history in a non-blocking manner.
    Flux<TransactionEntity> findAllByCustomerId(String customerId);
}
