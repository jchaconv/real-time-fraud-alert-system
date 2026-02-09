package com.jchacon.banking.frauddetection.model;

import com.jchacon.banking.frauddetection.model.enums.ChannelType;
import com.jchacon.banking.frauddetection.model.enums.CurrencyType;
import com.jchacon.banking.frauddetection.model.enums.OperationType;
import com.jchacon.banking.frauddetection.validation.ValueInEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessTransactionRequestDTO {

    @NotBlank(message = "transactionId is required")
    private String transactionId;

    @NotBlank(message = "accountId is required")
    private String accountId;

    @NotBlank(message = "customerId is required")
    private String customerId;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than zero")
    @DecimalMin(value = "0.01", message = "minimum amount allowed is 0.01")
    private BigDecimal amount;

    @NotBlank(message = "currency is required")
    @ValueInEnum(enumClass = CurrencyType.class, message = "invalid currency type.")
    private String currency; // E.g., USD, PEN, EUR

    @NotBlank(message = "operationType is required")
    @ValueInEnum(enumClass = OperationType.class, message = "invalid operation type.")
    private String operationType; // E.g., DEBIT, CREDIT

    @NotBlank(message = "merchantId is required")
    private String merchantId;

    @NotBlank(message = "merchantName is required")
    private String merchantName;

    @NotBlank(message = "mcc is required")
    @Size(min = 4, max = 4, message = "mcc must be a 4-digit code")
    private String mcc; // ISO 18245 for Merchant Category Codes

    private String terminalId; // Optional depending on the channel

    @Pattern(regexp = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$",
            message = "invalid ipAddress format")
    private String ipAddress;

    @NotBlank(message = "channel is required")
    @ValueInEnum(enumClass = ChannelType.class, message = "invalid channel.")
    private String channel;
}
