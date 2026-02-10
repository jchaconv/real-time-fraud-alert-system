package com.jchacon.banking.frauddetection.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionEvent {
    private String transactionId;
    private String customerId;
    private BigDecimal amount;
    private String status;
    private String responseCode;
    private LocalDateTime timestamp;
    private String correlationId;
}
