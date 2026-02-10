package com.jchacon.banking.notification.service;

import com.jchacon.banking.notification.event.TransactionEvent;
import reactor.core.publisher.Mono;

public interface NotificationService {

    Mono<Void> sendNotification(TransactionEvent event);

}
