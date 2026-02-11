package com.jchacon.banking.frauddetection.service;

import com.jchacon.banking.frauddetection.model.ProcessTransactionResponseDTO;
import reactor.core.publisher.Mono;

public interface IdempotencyService {
    /**
     * Checks if the transaction ID is already in Redis.
     * @param key The transactionId
     * @return Mono<Boolean> true if exists, false otherwise
     */
    Mono<Boolean> isDuplicate(String key);

    /**
     * Retrieves the cached response from Redis if it exists.
     */
    Mono<ProcessTransactionResponseDTO> getCachedResponse(String key);

    /**
     * Saves the full response DTO into Redis with a TTL.
     * @param key The transactionId
     * @return Mono<Void>
     */
    Mono<Void> markAsProcessed(String key, ProcessTransactionResponseDTO response);
}