package com.jchacon.banking.frauddetection.exception;

import com.jchacon.banking.frauddetection.model.enums.TransactionStatus;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final TransactionStatus status;

    public BusinessException(TransactionStatus status, String message) {
        super(message);
        this.status = status;
    }
}
