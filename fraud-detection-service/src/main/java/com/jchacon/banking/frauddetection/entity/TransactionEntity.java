package com.jchacon.banking.frauddetection.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
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
public class TransactionEntity implements Persistable<UUID> {

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
    @CreatedDate // Automatically sets the date when the record is inserted
    private LocalDateTime createdAt;

    @Column("updated_at")
    @LastModifiedDate // Automatically updates the date when the record is modified
    private LocalDateTime updatedAt;

    /*
     * R2DBC specific: Persistable implementation ensures that
     * Spring Data knows when to perform an INSERT vs UPDATE.
     */
    @Transient // This field is not persisted in the database
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew || id == null;
    }

    /*
     * Useful for setting the state after loading from DB
     */
    public TransactionEntity setAsNotNew() {
        this.isNew = false;
        return this;
    }
}
