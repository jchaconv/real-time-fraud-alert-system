package com.jchacon.banking.notification.consumer;

import com.jchacon.banking.notification.event.TransactionEvent;
import com.jchacon.banking.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final NotificationService notificationService;
    private static final String CORRELATION_ID_KEY = "correlationId";

    /**
     * Main Kafka listener. Consumes messages from the fraud-detection-events topic.
     * It manually manages the MDC to ensure the Correlation ID is logged.
     */
    @KafkaListener(topics = "fraud-detection-events", groupId = "notification-group")
    public void consume(TransactionEvent event) {
        try {
            // Manual propagation of correlationId for logging purposes in the consumer thread
            MDC.put(CORRELATION_ID_KEY, event.getCorrelationId());

            log.info("Received event from Kafka. TransactionId: {} | CustomerId: {}",
                    event.getTransactionId(), event.getCustomerId());

            // USE .block() to ensure the Kafka thread waits for the result.
            // If the Mono throws an error, it will be caught by the catch block below.
            notificationService.sendNotification(event).block();

        } catch (Exception e) {
            log.error("Error processing notification event: {}", e.getMessage());
            // Re-throw to trigger the DefaultErrorHandler (DLQ)
            throw e;
        } finally {
            MDC.remove(CORRELATION_ID_KEY);
        }
    }
}