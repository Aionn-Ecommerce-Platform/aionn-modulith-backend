package com.aionn.sharedkernel.infrastructure.outbox;

import com.aionn.sharedkernel.domain.model.DomainEvent;
import com.aionn.sharedkernel.domain.model.EventEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final String workerId = UUID.randomUUID().toString();
    private final int batchSize;
    private final int maxAttempts;

    public OutboxDispatcher(OutboxEventRepository repository, ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            @Value("${aionn.outbox.batch-size:50}") int batchSize,
            @Value("${aionn.outbox.max-attempts:10}") int maxAttempts) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${aionn.outbox.poll-delay-ms:1000}")
    public void dispatch() {
        for (OutboxEventRecord record : repository.claim(batchSize, workerId, Duration.ofMinutes(2))) {
            dispatch(record);
        }
    }

    private void dispatch(OutboxEventRecord record) {
        try {
            String consumerId = "spring-event-bus:" + record.eventType();
            if (repository.wasProcessed(consumerId, record.eventId())) {
                repository.markPublished(record.eventId());
                return;
            }
            Class<?> payloadType = Class.forName(record.payloadType());
            Object payload = objectMapper.readValue(record.payload(), payloadType);
            Object event = "DOMAIN".equals(record.eventKind())
                    ? new EventEnvelope(record.eventId(), record.aggregateType(), record.aggregateId(),
                            (DomainEvent) payload, record.occurredAt())
                    : payload;
            eventPublisher.publishEvent(event);
            repository.markProcessed(consumerId, record.eventId());
            repository.markPublished(record.eventId());
        } catch (Exception exception) {
            boolean deadLetter = record.attempts() >= maxAttempts;
            long backoffSeconds = Math.min(3600, 1L << Math.min(record.attempts(), 12));
            repository.markFailed(record.eventId(), exception.getMessage(),
                    Instant.now().plusSeconds(backoffSeconds), deadLetter);
            log.error("Outbox dispatch failed for event {} (attempt {}, deadLetter={})",
                    record.eventId(), record.attempts(), deadLetter, exception);
        }
    }
}
