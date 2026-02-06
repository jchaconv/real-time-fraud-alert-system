package com.jchacon.banking.frauddetection.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("customer_limits") //org.springframework.data.relational, no javax.persistence
public class CustomerLimitEntity {

    @Id
    @Column("customer_id")
    private String customerId;

    @Column("daily_max_amount")
    private BigDecimal dailyMaxAmount;

    @Column("current_daily_spent")
    private BigDecimal currentDailySpent;

    @Column("last_reset")
    private LocalDateTime lastReset;

}
