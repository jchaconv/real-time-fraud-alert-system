package com.jchacon.banking.frauddetection.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("transactions")
public class TransactionEntity {

    @Id
    private UUID id;

    @Column("transaction_id")
    private String transactionId;

    @Column("correlation_id")
    private String correlationId;

    @Column("account_id")
    private String accountId;

    @Column("customer_id")
    private String customerId;

    private BigDecimal amount;

    private String currency;

    @Column("operation_type")
    private String operationType;

    @Column("merchant_id")
    private String merchantId;

    @Column("merchant_name")
    private String merchantName;

    private String mcc;

    @Column("terminal_id")
    private String terminalId;

    @Column("ip_address")
    private String ipAddress;

    private String channel;

    private String status;

    @Column("response_code")
    private String responseCode;

    private String description;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

}
