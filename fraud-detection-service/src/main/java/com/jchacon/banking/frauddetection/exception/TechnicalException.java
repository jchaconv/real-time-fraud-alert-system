package com.jchacon.banking.frauddetection.exception;

import lombok.Getter;

@Getter
public class TechnicalException extends RuntimeException {
    private final String errorCode;

    public TechnicalException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "INTERNAL_SERVER_ERROR";
    }

    public TechnicalException(String message) {
        super(message);
        this.errorCode = "INTERNAL_SERVER_ERROR";
    }
}
