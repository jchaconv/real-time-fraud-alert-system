package com.jchacon.banking.frauddetection.model.enums;

import lombok.Getter;

@Getter
public enum OutboxEventStatus {
    PENDING("PENDING"),     // Initial state (optional if you want to use it)
    FAILED("FAILED"),       // Kafka failed, waiting for scheduler
    PROCESSING("PROCESSING"), // Currently being retried
    COMPLETED("COMPLETED"), // Sent successfully (if you don't delete the record)
    FATAL_ERROR("FATAL");   // Max retries reached, needs manual check

    private final String code;

    OutboxEventStatus(String code) {
        this.code = code;
    }
}