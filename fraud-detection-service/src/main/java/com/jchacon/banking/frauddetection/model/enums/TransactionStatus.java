package com.jchacon.banking.frauddetection.model.enums;

import lombok.Getter;

@Getter
public enum TransactionStatus {
    APPROVED("APPROVED", "00"),
    REJECTED_LIMIT("REJECTED", "51"),
    REJECTED_FRAUD("REJECTED", "34"),
    ERROR_SYSTEM("ERROR", "96"),
    CUSTOMER_NOT_FOUND("ERROR", "14");

    private final String description;
    private final String responseCode;

    TransactionStatus(String description, String responseCode) {
        this.description = description;
        this.responseCode = responseCode;
    }
}