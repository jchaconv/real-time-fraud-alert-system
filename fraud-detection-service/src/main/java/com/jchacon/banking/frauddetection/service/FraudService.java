package com.jchacon.banking.frauddetection.service;

import com.jchacon.banking.frauddetection.config.CorrelationFilter;
import com.jchacon.banking.frauddetection.exception.BusinessException;
import com.jchacon.banking.frauddetection.exception.TechnicalException;
import com.jchacon.banking.frauddetection.entity.CustomerLimitEntity;
import com.jchacon.banking.frauddetection.entity.TransactionEntity;
import com.jchacon.banking.frauddetection.model.ProcessTransactionRequestDTO;
import com.jchacon.banking.frauddetection.model.ProcessTransactionResponseDTO;
import com.jchacon.banking.frauddetection.model.enums.TransactionStatus;
import com.jchacon.banking.frauddetection.repository.CustomerLimitRepository;
import com.jchacon.banking.frauddetection.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@RequiredArgsConstructor
@Service
public class FraudService {

    private final TransactionRepository transactionRepository;
    private final CustomerLimitRepository customerLimitRepository;

    /**
     * Entry point for transaction processing.
     * Implements Idempotency to prevent double-spending or duplicate records.
     */
    @Transactional // Ensures both database operations complete or fail together
    public Mono<ProcessTransactionResponseDTO> processTransaction(ProcessTransactionRequestDTO request) {

        // Get the correlation ID from MDC (already populated by Micrometer Context Propagation)
        String activeCorrelationId = MDC.get(CorrelationFilter.CORRELATION_ID_KEY);

        // Idempotency Check: First, look for the transactionId in our records
        return transactionRepository.findByTransactionId(request.getTransactionId())
                .map(existingEntity -> {
                    log.warn("Idempotency Triggered: Transaction {} already processed. Returning cached result.",
                            request.getTransactionId());
                    return mapToResponseDTO(existingEntity);
                })
                /* * 2. If not found, proceed to process.
                 * Mono.defer is crucial here: it ensures executeProcessing() is only
                 * called if the transaction does NOT exist in the database.
                 */
                .switchIfEmpty(Mono.defer(() -> executeProcessing(request, activeCorrelationId)));
    }

    /**
     * Internal logic for a new transaction evaluation.
     * Evaluates the transaction and updates the customer's daily spent balance if approved.
     */
    private Mono<ProcessTransactionResponseDTO> executeProcessing(ProcessTransactionRequestDTO request, String correlationId) {
        TransactionEntity transaction = mapRequestToEntity(request, correlationId);
        log.info("Processing new transaction: {}", transaction.getTransactionId());
        return customerLimitRepository.findById(transaction.getCustomerId())
                .flatMap(limit -> {
                    BigDecimal totalSpent = limit.getCurrentDailySpent().add(transaction.getAmount());
                    if (totalSpent.compareTo(limit.getDailyMaxAmount()) > 0) {
                        return handleRejectedTransaction(transaction, totalSpent);
                    } else {
                        return handleApprovedTransaction(transaction, limit, totalSpent);
                    }
                })
                .map(this::mapToResponseDTO) // Mapping to Response DTO
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
}
