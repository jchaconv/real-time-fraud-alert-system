package com.jchacon.banking.frauddetection.model;

import lombok.Builder;
import lombok.Value;
import java.time.LocalDateTime;

@Value
@Builder
public class ErrorResponse {

    String code;
    String message;
    LocalDateTime timestamp;

}
