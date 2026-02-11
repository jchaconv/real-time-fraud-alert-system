package com.jchacon.banking.frauddetection.service.impl;

import io.micrometer.tracing.Tracer;

import com.jchacon.banking.frauddetection.event.TransactionEvent;
import com.jchacon.banking.frauddetection.exception.BusinessException;
import com.jchacon.banking.frauddetection.exception.TechnicalException;
import com.jchacon.banking.frauddetection.entity.CustomerLimitEntity;
import com.jchacon.banking.frauddetection.entity.TransactionEntity;
import com.jchacon.banking.frauddetection.model.ProcessTransactionRequestDTO;
import com.jchacon.banking.frauddetection.model.ProcessTransactionResponseDTO;
import com.jchacon.banking.frauddetection.model.enums.OperationType;
import com.jchacon.banking.frauddetection.model.enums.TransactionStatus;
import com.jchacon.banking.frauddetection.producer.FraudEventProducer;
import com.jchacon.banking.frauddetection.repository.CustomerLimitRepository;
import com.jchacon.banking.frauddetection.repository.TransactionRepository;
import com.jchacon.banking.frauddetection.service.FraudService;
import com.jchacon.banking.frauddetection.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class FraudServiceImpl implements FraudService {

    private final TransactionRepository transactionRepository;
    private final CustomerLimitRepository customerLimitRepository;
    private final FraudEventProducer eventProducer;
    private final IdempotencyService idempotencyService;

    private final Tracer tracer;

    /**
     * Entry point for transaction processing.
     * Implements Idempotency to prevent double-spending or duplicate records.
     */
    @Transactional // Ensures both database operations complete or fail together
    public Mono<ProcessTransactionResponseDTO> processTransaction(ProcessTransactionRequestDTO request) {
        String txnId = request.getTransactionId();

        // Get the current technical Trace ID
        String activeTraceId = tracer.currentSpan() != null ?
                tracer.currentSpan().context().traceId() : "N/A";

        // Try to get from Redis Cache (Full JSON)
        return idempotencyService.getCachedResponse(txnId)
                .doOnNext(res -> log.warn("Idempotency Triggered (Redis): Returning full cached response for {}", txnId))
                // If NOT in Redis, check DB (Double check for safety)
                .switchIfEmpty(Mono.defer(() -> fetchFromDbAndMap(txnId)))
                // If NOT in DB, process normally
                .switchIfEmpty(Mono.defer(() -> executeProcessing(request, activeTraceId)));
    }

    /**
     * Helper method to fetch from DB and map to DTO to keep processTransaction clean.
     */
    private Mono<ProcessTransactionResponseDTO> fetchFromDbAndMap(String transactionId) {
        // Idempotency Check: First, look for the transactionId in our records
        return transactionRepository.findByTransactionId(transactionId)
                .map(existingEntity -> {
                    log.warn("Idempotency Triggered (DB): Transaction {} found in records.", transactionId);
                    return mapToResponseDTO(existingEntity);
                });
    }

    /**
     * Internal logic for a new transaction evaluation.
     * Evaluates the transaction and updates the customer's daily spent balance if approved.
     */
    private Mono<ProcessTransactionResponseDTO> executeProcessing(ProcessTransactionRequestDTO request, String traceId) {
        // Map using the traceId as the correlationId for the Entity/Database
        TransactionEntity transaction = mapRequestToEntity(request, traceId);
        log.info("Processing new transaction: {} | Type: {} | Correlation: {}", transaction.getTransactionId(), transaction.getOperationType(), traceId);
        return customerLimitRepository.findById(transaction.getCustomerId())
                .timeout(Duration.ofSeconds(2)) // Critical: Protects against slow DB lookups
                .flatMap(limit -> {
                    // Business Rule: Determine if this operation should impact the daily limit
                    BigDecimal impactValue = calculateLimitImpact(transaction);
                    BigDecimal projectedSpent = limit.getCurrentDailySpent().add(impactValue);
                    // Validation against daily threshold
                    if (projectedSpent.compareTo(limit.getDailyMaxAmount()) > 0) {
                        return handleRejectedTransaction(transaction, projectedSpent);
                    } else {
                        return handleApprovedTransaction(transaction, limit, projectedSpent);
                    }
                })
                // AFTER DB SAVE: Mark as processed in Redis AND send to Kafka
                .flatMap(savedEntity -> {
                    ProcessTransactionResponseDTO response = mapToResponseDTO(savedEntity);
                    // Save in Redis AND publish to Kafka in parallel (Reactive style)
                    return Mono.when(
                            idempotencyService.markAsProcessed(savedEntity.getTransactionId(), response)
                                    .doOnError(e -> log.error("Redis Cache Failed: {}", e.getMessage())),
                            eventProducer.sendTransactionEvent(mapToEvent(savedEntity))
                                    .doOnError(e -> log.error("Kafka Publish Failed: {}", e.getMessage()))
                    ).thenReturn(response); // Ignoring the Kafka error to have no effect on the client's response
                })
                // Applying timeout to the entire flow or individual DB saves
                .timeout(Duration.ofSeconds(5))
                .switchIfEmpty(Mono.error(() -> {
                    log.error("Validation failed: Customer {} not found", transaction.getCustomerId());
                    return new BusinessException(TransactionStatus.CUSTOMER_NOT_FOUND, "Customer not found in system");
                }))
                // Transforming infrastructure errors into TechnicalException
                .onErrorResume(e -> !(e instanceof BusinessException), e -> {
                    log.error("Technical error during transaction processing: {}", e.getMessage());
                    return Mono.error(new TechnicalException("Service temporarily unavailable due to System issues", e));
                });
    }

    /**
     * Strategy pattern-like logic to determine how much the transaction impacts the limit.
     */
    private BigDecimal calculateLimitImpact(TransactionEntity transaction) {
        OperationType type = OperationType.valueOf(transaction.getOperationType().toUpperCase());

        return switch (type) {
            case DEBIT, CASH_WITHDRAWAL, TRANSFER -> transaction.getAmount();
            case CREDIT -> {
                // Logic: If it's a credit purchase it consumes limit,
                // but we could implement logic for refunds here (returning negative amount).
                log.info("Credit operation detected for ID: {}", transaction.getTransactionId());
                yield transaction.getAmount(); //Produce the final value of this block
            }
            default -> transaction.getAmount();
        };
    }

    private Mono<TransactionEntity> handleApprovedTransaction(TransactionEntity transaction,
                                                              CustomerLimitEntity limit,
                                                              BigDecimal totalSpent) {

        log.info("Transaction approved for customer: {}", transaction.getCustomerId());

        transaction.setStatus(TransactionStatus.APPROVED.getDescription());
        transaction.setResponseCode(TransactionStatus.APPROVED.getResponseCode());
        transaction.setDescription("Transaction verified successfully");

        // Update the limit object with the new total spent
        limit.setCurrentDailySpent(totalSpent);

        // Chain both saves: Update limit AND save transaction
        return customerLimitRepository.save(limit)
                .doOnSuccess(l -> log.info("STEP 1 SUCCESS: Customer balance updated to {}", l.getCurrentDailySpent()))
                .doOnError(e -> log.error("STEP 1 FAILED: Could not update customer balance: {}", e.getMessage()))
                .then(transactionRepository.save(transaction))
                .doOnSuccess(t -> log.info("STEP 2 SUCCESS: Transaction recorded with ID: {}", t.getId()))
                .doOnError(e -> log.error("STEP 2 FAILED: Could not save transaction record: {}", e.getMessage()));
    }

    private Mono<TransactionEntity> handleRejectedTransaction(TransactionEntity transaction, BigDecimal totalSpent) {
        log.warn("Fraud alert! Limit exceeded for customer: {}. Total: {}", transaction.getCustomerId(), totalSpent);

        transaction.setStatus(TransactionStatus.REJECTED_LIMIT.getDescription());
        transaction.setResponseCode(TransactionStatus.REJECTED_LIMIT.getResponseCode());
        transaction.setDescription("Daily transaction limit exceeded");

        return transactionRepository.save(transaction);
    }

    /**
     * Maps the Request DTO to a Transaction Entity.
     */
    private TransactionEntity mapRequestToEntity(ProcessTransactionRequestDTO request, String correlationId) {

        return TransactionEntity.builder()
                .transactionId(request.getTransactionId())
                .correlationId(correlationId)
                .accountId(request.getAccountId())
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .operationType(request.getOperationType())
                .merchantId(request.getMerchantId())
                .merchantName(request.getMerchantName())
                .mcc(request.getMcc())
                .terminalId(request.getTerminalId())
                .ipAddress(request.getIpAddress())
                .channel(request.getChannel())
                .build();
    }

    /**
     * Maps the persisted Entity to a Response DTO.
     */
    private ProcessTransactionResponseDTO mapToResponseDTO(TransactionEntity entity) {
        return ProcessTransactionResponseDTO.builder()
                .id(entity.getId() != null ? entity.getId().toString() : null)
                .transactionId(entity.getTransactionId())
                .status(entity.getStatus())
                .responseCode(entity.getResponseCode())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private TransactionEvent mapToEvent(TransactionEntity entity) {
        return TransactionEvent.builder()
                .transactionId(entity.getTransactionId())
                .customerId(entity.getCustomerId())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .responseCode(entity.getResponseCode())
                .timestamp(entity.getCreatedAt() != null ? entity.getCreatedAt() : LocalDateTime.now())
                .correlationId(entity.getCorrelationId())
                .build();
    }
}
