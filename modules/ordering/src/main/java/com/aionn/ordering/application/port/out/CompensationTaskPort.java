package com.aionn.ordering.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CompensationTaskPort {

    enum Type { VOUCHER_RELEASE, RESERVATION_RELEASE }

    record Task(String taskId, Type type, String resourceId, String userId, String orderId,
            String reason, int attempts) {}

    void enqueue(Task task);

    Optional<Task> findById(String taskId);

    List<String> findRetryableIds(Instant now, int maxAttempts, int limit);

    void markCompleted(String taskId);

    void markFailed(String taskId, String error, Instant nextAttemptAt, boolean deadLetter);

    long countDeadLetters();
}
