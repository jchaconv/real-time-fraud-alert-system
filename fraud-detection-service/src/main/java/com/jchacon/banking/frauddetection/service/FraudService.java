package com.jchacon.banking.frauddetection.service;

import com.jchacon.banking.frauddetection.model.TransactionEntity;
import com.jchacon.banking.frauddetection.repository.CustomerLimitRepository;
import com.jchacon.banking.frauddetection.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@RequiredArgsConstructor
@Service
public class FraudService {

    private final TransactionRepository transactionRepository;
    private final CustomerLimitRepository customerLimitRepository;

    /**
     * Processes a transaction by validating it against the customer's daily limit.
     * * @param transaction The transaction to be evaluated.
     * @return A Mono containing the updated and persisted TransactionEntity.
     */
    public Mono<TransactionEntity> processTransaction(TransactionEntity transaction) {

        log.info("Analyzing transaction: {}", transaction.getTransactionId());

        return customerLimitRepository.findById(transaction.getCustomerId())
                .flatMap(limit -> {
                    // Business Logic: Check if (current transaction amount + daily spent) exceeds max limit
                    BigDecimal totalSpent = limit.getCurrentDailySpent().add(transaction.getAmount());
                    if (totalSpent.compareTo(limit.getDailyMaxAmount()) > 0) {
                        log.warn("Potential Fraud Detected! Customer {} exceeded daily limit. Total attempt: {}",
                                transaction.getCustomerId(), totalSpent);
                        transaction.setStatus("REJECTED");
                        transaction.setResponseCode("51"); // Insufficient funds / Limit exceeded code
                        transaction.setDescription("Daily transaction limit exceeded");
                    } else {
                        log.info("Transaction approved for customer: {}", transaction.getCustomerId());
                        transaction.setStatus("APPROVED");
                        transaction.setResponseCode("00"); // Success code
                        transaction.setDescription("Transaction verified successfully");
                    }
                    // Persist the transaction with its final status
                    return transactionRepository.save(transaction);
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Customer not found or limits not configured")));
    }
}
