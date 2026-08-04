package com.aionn.sharedkernel.infrastructure.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

@Service
public class OutboxDeadLetterService {

    private static final int MAX_PAGE_SIZE = 100;

    private final OutboxEventRepository repository;

    OutboxDeadLetterService(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public DeadLetterPage list(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to zero");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return new DeadLetterPage(
                repository.findDeadLetters(size, Math.multiplyExact(page, size)),
                page, size, repository.countDeadLetters());
    }

    @Transactional
    public void requeue(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        if (!repository.requeueDeadLetter(eventId)) {
            throw new DeadLetterNotFoundException(eventId);
        }
    }

    public record DeadLetterPage(List<DeadLetterEvent> content, int page, int size, long totalElements) {
        public DeadLetterPage {
            content = List.copyOf(content);
        }
    }

    public record DeadLetterEvent(
            String eventId,
            String eventKind,
            String eventType,
            String aggregateType,
            String aggregateId,
            Instant occurredAt,
            int attempts,
            String lastError,
            Instant deadLetteredAt) {
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static final class DeadLetterNotFoundException extends RuntimeException {
        public DeadLetterNotFoundException(String eventId) {
            super("Dead-letter event not found: " + eventId);
        }
    }
}
