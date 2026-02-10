package com.jchacon.banking.notification.service;

import com.jchacon.banking.notification.event.TransactionEvent;
import com.jchacon.banking.notification.model.enums.TransactionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    /**
     * Simulates sending a notification to the customer based on the transaction status.
     * @param event The transaction event received from Kafka
     * @return Mono<Void>
     */
    public Mono<Void> sendNotification(TransactionEvent event) {
        return Mono.fromRunnable(() -> {
            log.info("Sending notification for Transaction: {} | Status: {}",
                    event.getTransactionId(), event.getStatus());

            // Simulate an unexpected error (e.g., Mail server down)
            if (event.getAmount().doubleValue() > 500) {
                throw new RuntimeException("Simulated failure for high value transaction!");
            }

            // Business Logic: If the amount is too high, we might simulate a delay or specific alert
            if (event.getStatus().equals(TransactionStatus.APPROVED.getDescription())) {
                log.info("Message: 'Dear customer, your transaction of {} was successful.'", event.getAmount());
            } else {
                log.warn("Alert: 'Security notice: A transaction for {} was rejected.'", event.getAmount());
            }
        }).then();
    }
}
