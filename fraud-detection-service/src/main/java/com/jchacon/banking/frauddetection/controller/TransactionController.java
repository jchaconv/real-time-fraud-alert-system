package com.jchacon.banking.frauddetection.controller;

import com.jchacon.banking.frauddetection.model.TransactionEntity;
import com.jchacon.banking.frauddetection.service.FraudService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final FraudService fraudService;

    /**
     * Receives a new transaction, processes it through the fraud engine, and persists the result.
     * * @param transaction The transaction data provided in the request body.
     * @return A Mono emitting the processed TransactionEntity.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TransactionEntity> createTransaction(@Valid @RequestBody TransactionEntity transaction) {
        return fraudService.processTransaction(transaction);
    }

    /**
     * Simple health check endpoint for the reactive service.
     */
    @GetMapping("/health")
    public Mono<String> checkHealth() {
        return Mono.just("Service is up and running in reactive mode");
    }


}
