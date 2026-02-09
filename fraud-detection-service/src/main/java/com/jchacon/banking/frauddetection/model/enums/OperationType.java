package com.jchacon.banking.frauddetection.model.enums;

import lombok.Getter;

@Getter
public enum OperationType {
    DEBIT,
    CREDIT,
    TRANSFER,
    CASH_WITHDRAWAL;
}