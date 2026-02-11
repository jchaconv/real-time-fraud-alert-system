package com.jchacon.banking.frauddetection.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("outbox_events")
public class OutboxEventEntity {

    @Id
    private Long id;

    @Column("transaction_id")
    private String transactionId;

    /**
     * Store the serialized TransactionEvent as a JSON string.
     * In Postgres, this maps to the JSONB column we created.
     */
    private String payload;

    private String status;

    @Column("error_message")
    private String errorMessage;

    @Column("retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column("created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column("updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}