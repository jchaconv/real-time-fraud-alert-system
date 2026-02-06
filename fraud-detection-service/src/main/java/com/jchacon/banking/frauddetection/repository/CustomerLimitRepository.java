package com.jchacon.banking.frauddetection.repository;

import com.jchacon.banking.frauddetection.model.CustomerLimitEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerLimitRepository extends ReactiveCrudRepository<CustomerLimitEntity, String> {
    // Extending ReactiveCrudRepository provides methods that return Mono and Flux by default. Non-blocking.
}
