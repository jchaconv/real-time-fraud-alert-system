package com.jchacon.banking.frauddetection.model;

import com.jchacon.banking.frauddetection.model.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessTransactionResponseDTO {
    private String id; // DB Primary Key (UUID)
    private String transactionId;
    private String status;
    private String responseCode;
    private String description;
    private LocalDateTime createdAt;
}
