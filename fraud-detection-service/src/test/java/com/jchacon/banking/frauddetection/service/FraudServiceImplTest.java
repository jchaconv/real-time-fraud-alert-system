package com.jchacon.banking.frauddetection.service;

import com.jchacon.banking.frauddetection.entity.CustomerLimitEntity;
import com.jchacon.banking.frauddetection.entity.TransactionEntity;
import com.jchacon.banking.frauddetection.exception.TechnicalException;
import com.jchacon.banking.frauddetection.model.ProcessTransactionRequestDTO;
import com.jchacon.banking.frauddetection.model.enums.OperationType;
import com.jchacon.banking.frauddetection.producer.FraudEventProducer;
import com.jchacon.banking.frauddetection.repository.CustomerLimitRepository;
import com.jchacon.banking.frauddetection.repository.TransactionRepository;
import com.jchacon.banking.frauddetection.service.impl.FraudServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CustomerLimitRepository customerLimitRepository;

    @Mock
    private FraudEventProducer eventProducer;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private FraudServiceImpl fraudService;

    private ProcessTransactionRequestDTO request;
    private CustomerLimitEntity limit;

    private static final String TXN_ID = "TXN-100";

    @BeforeEach
    void setUp() {
        request = ProcessTransactionRequestDTO.builder()
                .transactionId("TXN-100")
                .customerId("CUST-777")
                .amount(new BigDecimal("100.00"))
                .operationType(OperationType.DEBIT.name())
                .build();

        limit = CustomerLimitEntity.builder()
                .customerId("CUST-777")
                .dailyMaxAmount(new BigDecimal("500.00"))
                .currentDailySpent(new BigDecimal("0.00"))
                .build();
    }

    @Test
    @DisplayName("Should APPROVE transaction and update limit")
    void shouldApproveTransaction() {
        // Arrange
        when(idempotencyService.getCachedResponse(TXN_ID)).thenReturn(Mono.empty());
        when(idempotencyService.markAsProcessed(anyString(), any())).thenReturn(Mono.empty());
        when(transactionRepository.findByTransactionId(anyString())).thenReturn(Mono.empty());
        when(customerLimitRepository.findById(anyString())).thenReturn(Mono.just(limit));
        when(customerLimitRepository.save(any())).thenReturn(Mono.just(limit));
        when(transactionRepository.save(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));

        when(eventProducer.sendTransactionEvent(any())).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(fraudService.processTransaction(request))
                .expectNextMatches(response ->
                        response.getStatus().equals("APPROVED") &&
                                response.getResponseCode().equals("00"))
                .verifyComplete();

        verify(customerLimitRepository).save(argThat(l -> l.getCurrentDailySpent().compareTo(new BigDecimal("100.00")) == 0));
    }

    @Test
    @DisplayName("Should REJECT transaction when daily limit is exceeded")
    void shouldRejectWhenLimitExceeded() {
        // Arrange
        when(idempotencyService.getCachedResponse(TXN_ID)).thenReturn(Mono.empty());
        when(idempotencyService.markAsProcessed(anyString(), any())).thenReturn(Mono.empty());
        limit.setCurrentDailySpent(new BigDecimal("450.00"));
        when(transactionRepository.findByTransactionId(anyString())).thenReturn(Mono.empty());
        when(customerLimitRepository.findById(anyString())).thenReturn(Mono.just(limit));
        when(transactionRepository.save(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));

        when(eventProducer.sendTransactionEvent(any())).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(fraudService.processTransaction(request))
                .expectNextMatches(response ->
                        "51".equals(response.getResponseCode()) &&
                                response.getStatus().startsWith("REJECTED"))
                .verifyComplete();

        // Verify limit safety
        verify(customerLimitRepository, never()).save(any());
    }

    @Test
    @DisplayName("Idempotency: Should return cached result if transactionId exists")
    void shouldTriggerIdempotency() {
        // Arrange
        when(idempotencyService.getCachedResponse(TXN_ID)).thenReturn(Mono.empty());
        TransactionEntity existingTx = TransactionEntity.builder()
                .transactionId("TXN-100")
                .status("APPROVED")
                .responseCode("00")
                .build();

        when(transactionRepository.findByTransactionId("TXN-100")).thenReturn(Mono.just(existingTx));

        // Act & Assert
        StepVerifier.create(fraudService.processTransaction(request))
                .expectNextMatches(response -> response.getTransactionId().equals("TXN-100"))
                .verifyComplete();

        // Core Verify: Customer logic never executed
        verify(customerLimitRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("Resilience: Should throw TechnicalException on Database Timeout")
    void shouldHandleTimeout() {
        // Arrange
        when(idempotencyService.getCachedResponse(TXN_ID)).thenReturn(Mono.empty());
        when(transactionRepository.findByTransactionId(anyString())).thenReturn(Mono.empty());
        // Simulating DB delay of 10s (exceeding the 2s and 5s timeouts in service)
        when(customerLimitRepository.findById(anyString()))
                .thenReturn(Mono.just(limit).delayElement(Duration.ofSeconds(10)));

        // Act & Assert
        StepVerifier.withVirtualTime(() -> fraudService.processTransaction(request))
                .thenAwait(Duration.ofSeconds(10))
                .expectError(TechnicalException.class)
                .verify();
    }
}