package com.jchacon.banking.frauddetection.service;

import com.jchacon.banking.frauddetection.model.ProcessTransactionRequestDTO;
import com.jchacon.banking.frauddetection.model.ProcessTransactionResponseDTO;
import reactor.core.publisher.Mono;

public interface FraudService {

    Mono<ProcessTransactionResponseDTO> processTransaction(ProcessTransactionRequestDTO request);

}
