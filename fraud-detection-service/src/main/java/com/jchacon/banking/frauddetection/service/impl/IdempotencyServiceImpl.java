package com.jchacon.banking.frauddetection.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jchacon.banking.frauddetection.model.ProcessTransactionResponseDTO;
import com.jchacon.banking.frauddetection.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.idempotency.ttl-hours}")
    private int ttlHours;

    private static final String REDIS_PREFIX = "idempotency:txn:";

    @Override
    public Mono<Boolean> isDuplicate(String key) {
        return redisTemplate.hasKey(REDIS_PREFIX + key)
                .doOnNext(exists -> {
                    if (exists) log.debug("Redis hit: Transaction {} already processed", key);
                });
    }

    @Override
    public Mono<ProcessTransactionResponseDTO> getCachedResponse(String key) {
        return redisTemplate.opsForValue().get(REDIS_PREFIX + key)
                .flatMap(json -> {
                    try {
                        log.info("Redis hit for key: {}", key);
                        return Mono.just(objectMapper.readValue(json, ProcessTransactionResponseDTO.class));
                    } catch (JsonProcessingException e) {
                        log.error("Error deserializing cached response", e);
                        return Mono.empty();
                    }
                });
    }

    @Override
    public Mono<Void> markAsProcessed(String key, ProcessTransactionResponseDTO response) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(response))
                .flatMap(json -> redisTemplate.opsForValue()
                        .set(REDIS_PREFIX + key, json, Duration.ofHours(ttlHours)))
                .doOnSuccess(v -> log.debug("Redis save: Transaction {} cached successfully", key))
                .onErrorResume(e -> {
                    log.error("Failed to save in Redis: {}", e.getMessage());
                    return Mono.empty();
                })
                .then();
    }
}