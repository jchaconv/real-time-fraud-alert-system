package com.jchacon.banking.frauddetection.controller;

import com.jchacon.banking.frauddetection.model.ProcessTransactionRequestDTO;
import com.jchacon.banking.frauddetection.model.ProcessTransactionResponseDTO;
import com.jchacon.banking.frauddetection.service.FraudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

@Tag(name = "Fraud Detection", description = "Endpoints for real-time transaction authorization and fraud analysis")
@RequiredArgsConstructor
@RequestMapping("/api/v1/fraud")
@RestController
public class TransactionController {

    private final FraudService fraudService;

    /**
     * Receives a new transaction, processes it through the fraud engine, and persists the result.
     * * @param transaction The transaction data provided in the request body.
     * @return A Mono emitting the processed ProcessTransactionRequestDTO.
     */
    @Operation(
            summary = "Process and authorize transaction",
            description = "Analyzes the transaction based on customer limits and business rules. Supports idempotency via transactionId."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction evaluated (can be APPROVED or REJECTED)",
                    content = @Content(schema = @Schema(implementation = ProcessTransactionResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data or business validation failed"),
            @ApiResponse(responseCode = "500", description = "Technical system error")
    })
    @PostMapping("/process") // More descriptive endpoint
    @ResponseStatus(HttpStatus.OK) // In banking, 200 is preferred as even a rejection is a valid business result
    public Mono<ProcessTransactionResponseDTO> processTransaction(@Valid @RequestBody ProcessTransactionRequestDTO transaction) {
        return fraudService.processTransaction(transaction);
    }

    /**
     * Simple health check endpoint for the reactive service.
     * Liveness and Readiness check for orchestration (Kubernetes/Cloud).
     */
    @Operation(summary = "Health check")
    @GetMapping("/health")
    public Mono<String> checkHealth() {
        return Mono.just("Service is up and running in reactive mode");
    }


}
