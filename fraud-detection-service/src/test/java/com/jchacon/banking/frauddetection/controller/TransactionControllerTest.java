package com.jchacon.banking.frauddetection.controller;

import com.jchacon.banking.frauddetection.model.ProcessTransactionRequestDTO;
import com.jchacon.banking.frauddetection.model.ProcessTransactionResponseDTO;
import com.jchacon.banking.frauddetection.service.FraudService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;

import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {TransactionController.class, GlobalExceptionHandler.class},
        properties = "spring.webflux.problemdetails.enabled=false") // IMPORTANT: Deactivate the default format
@ImportAutoConfiguration(exclude = {R2dbcAutoConfiguration.class})
class TransactionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private FraudService fraudService;

    @Test
    void shouldReturnOkWhenTransactionIsProcessed() {
        // Arrange
        ProcessTransactionResponseDTO mockResponse = ProcessTransactionResponseDTO.builder()
                .transactionId("TXN-123")
                .status("APPROVED")
                .responseCode("00")
                .build();

        when(fraudService.processTransaction(any())).thenReturn(Mono.just(mockResponse));

        ProcessTransactionRequestDTO validRequest = ProcessTransactionRequestDTO.builder()
                .transactionId("TXN-123")
                .accountId("ACC-1")
                .customerId("CUST-1")
                .amount(new BigDecimal("100.00"))
                .currency("PEN")
                .operationType("DEBIT")
                .merchantId("M-1")
                .merchantName("Store")
                .mcc("1234")
                .channel("WEB")
                .build();

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/fraud/process")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRequest)
                .exchange()
                .expectStatus().isOk() // Respects your @ResponseStatus(HttpStatus.OK)
                .expectBody()
                .jsonPath("$.status").isEqualTo("APPROVED")
                .jsonPath("$.responseCode").isEqualTo("00");
    }

    @Test
    void shouldReturnBadRequestWhenInputIsInvalid() {
        // Arrange
        ProcessTransactionRequestDTO invalidRequest = ProcessTransactionRequestDTO.builder()
                .amount(new BigDecimal("-10.00"))
                .build();

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/fraud/process")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                // Log content to console if it fails (helps debugging)
                .consumeWith(entityExchangeResult ->
                        System.out.println("RESPONSE BODY: " + new String(entityExchangeResult.getResponseBodyContent())))
                .jsonPath("$.code").isEqualTo("INVALID_INPUT")
                .jsonPath("$.responseCode").isEqualTo("99");
    }

}