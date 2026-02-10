package com.jchacon.banking.frauddetection.controller;

import com.jchacon.banking.frauddetection.exception.BusinessException;
import com.jchacon.banking.frauddetection.exception.TechnicalException;
import com.jchacon.banking.frauddetection.model.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handling Business Logic Errors (e.g., Insufficient funds, Customer not found)
    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBusinessException(BusinessException ex) {
        log.error("Business error: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getStatus().name(), ex.getMessage(), ex.getStatus().getResponseCode());
    }

    // Handling Technical/Infrastructure Errors (e.g., DB connection, Timeouts)
    @ExceptionHandler(TechnicalException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleTechnicalException(TechnicalException ex) {
        log.error("Technical error: {}", ex.getMessage(), ex); // Logging the stack trace for developers
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "TECHNICAL_ERROR", ex.getMessage(), "96");
    }

    // Handling Input Validation Errors (@Valid)
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationExceptions(WebExchangeBindException ex) {
        String details = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.error("Validation failed: {}", details);
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_INPUT", details, "99");
    }

    // Generic Exception handler (Catch-all)
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String code = "SYSTEM_ERROR";
        String message = "An unexpected error occurred";
        String responseCode = "99";

        // If it's a Spring native exception (like 404 or 405), respect its status
        if (ex instanceof ResponseStatusException rsEx) {
            status = HttpStatus.valueOf(rsEx.getStatusCode().value());
            message = rsEx.getReason() != null ? rsEx.getReason() : rsEx.getMessage();
            code = "HTTP_ERROR_" + status.value();
        } else {
            log.error("Unexpected system error: ", ex);
        }
        return buildResponse(status, code, message, responseCode);
    }

    private Mono<ResponseEntity<ErrorResponse>> buildResponse(HttpStatus status, String code, String message, String responseCode) {
        ErrorResponse response = ErrorResponse.builder()
                .code(code)
                .message(message)
                .responseCode(responseCode)
                .timestamp(LocalDateTime.now())
                .build();
        return Mono.just(ResponseEntity.status(status).body(response));
    }
}