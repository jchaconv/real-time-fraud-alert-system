package com.jchacon.banking.frauddetection.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    String code;      // E.g., "REJECTED_LIMIT"
    String message;   // E.g., "Daily transaction limit exceeded"
    String responseCode; // E.g., "51" (New field)
    LocalDateTime timestamp;

}
