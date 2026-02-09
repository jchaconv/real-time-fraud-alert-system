package com.jchacon.banking.frauddetection.repository;

import com.jchacon.banking.frauddetection.entity.TransactionEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface TransactionRepository extends ReactiveCrudRepository<TransactionEntity, UUID> {

    // Retrieves the customer's transaction history in a non-blocking manner.
    Flux<TransactionEntity> findAllByCustomerId(String customerId);

    // Finds a transaction by its business ID
    Mono<TransactionEntity> findByTransactionId(String transactionId);

}
