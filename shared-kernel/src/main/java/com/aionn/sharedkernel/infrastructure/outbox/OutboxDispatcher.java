package com.aionn.sharedkernel.infrastructure.outbox;

import com.aionn.sharedkernel.domain.model.DomainEvent;
import com.aionn.sharedkernel.domain.model.EventEnvelope;
import com.aionn.sharedkernel.integration.event.IntegrationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Clock;
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
    private final Duration leaseDuration;
    private final Clock clock;

    public OutboxDispatcher(OutboxEventRepository repository, ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            @Value("${aionn.outbox.batch-size:50}") int batchSize,
            @Value("${aionn.outbox.max-attempts:10}") int maxAttempts,
            @Value("${aionn.outbox.lease-duration-seconds:300}") long leaseDurationSeconds,
            Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
        this.leaseDuration = Duration.ofSeconds(leaseDurationSeconds);
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${aionn.outbox.poll-delay-ms:1000}", scheduler = "outboxTaskScheduler")
    public void dispatch() {
        for (OutboxEventRecord record : repository.claim(batchSize, workerId, leaseDuration)) {
            dispatch(record);
        }
    }

    private void dispatch(OutboxEventRecord record) {
        try {
            Class<?> payloadType = resolvePayloadType(record);
            Object payload = objectMapper.readValue(record.payload(), payloadType);
            Object event = "DOMAIN".equals(record.eventKind())
                    ? new EventEnvelope(record.eventId(), record.aggregateType(), record.aggregateId(),
                            (DomainEvent) payload, record.occurredAt())
                    : payload;
            eventPublisher.publishEvent(event);
            markPublished(record.eventId());
        } catch (Exception exception) {
            boolean deadLetter = record.attempts() >= maxAttempts;
            long backoffSeconds = Math.min(3600, 1L << Math.min(record.attempts(), 12));
            boolean updated = repository.markFailed(record.eventId(), workerId, exception.getMessage(),
                    clock.instant().plusSeconds(backoffSeconds), deadLetter);
            if (!updated) {
                log.warn("Lost outbox lease before failure update for event {}", record.eventId());
            }
            log.error("Outbox dispatch failed for event {} (attempt {}, deadLetter={})",
                    record.eventId(), record.attempts(), deadLetter, exception);
        }
    }

    private void markPublished(String eventId) {
        if (!repository.markPublished(eventId, workerId)) {
            log.warn("Lost outbox lease before publish completion for event {}", eventId);
        }
    }

    private static Class<?> resolvePayloadType(OutboxEventRecord record) throws ClassNotFoundException {
        if (!record.payloadType().startsWith("com.aionn.")) {
            throw new IllegalArgumentException("Outbox payload type is not allowed: " + record.payloadType());
        }
        Class<?> payloadType = Class.forName(record.payloadType());
        Class<?> requiredType = "DOMAIN".equals(record.eventKind()) ? DomainEvent.class : IntegrationEvent.class;
        if (!requiredType.isAssignableFrom(payloadType)) {
            throw new IllegalArgumentException("Outbox payload type does not implement "
                    + requiredType.getSimpleName() + ": " + record.payloadType());
        }
        return payloadType;
    }
}
