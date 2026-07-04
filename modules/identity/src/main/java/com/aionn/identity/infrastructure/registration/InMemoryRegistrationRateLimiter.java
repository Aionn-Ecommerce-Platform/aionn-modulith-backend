package com.aionn.identity.infrastructure.registration;

import com.aionn.identity.application.port.out.registration.RegistrationRateLimiterPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(prefix = "identity.registration.ratelimit", name = "provider", havingValue = "memory")
public class InMemoryRegistrationRateLimiter implements RegistrationRateLimiterPort {

    private final Map<String, ArrayDeque<Long>> requestsByKey = new ConcurrentHashMap<>();

    @Override
    public boolean check(String scope, String key, int maxAttempts, int windowSeconds) {
        if (key == null || key.isBlank()) {
            return true;
        }

        String bucket = scope + ":" + key;
        long now = Instant.now().getEpochSecond();
        long threshold = now - windowSeconds;

        // Atomic load-or-create + prune inside a single ConcurrentHashMap slot
        // lock; evictDrainedBuckets() removes buckets that go quiet.
        boolean[] allowed = new boolean[1];
        requestsByKey.compute(bucket, (k, queue) -> {
            ArrayDeque<Long> q = queue == null ? new ArrayDeque<>() : queue;
            while (!q.isEmpty() && q.peekFirst() <= threshold) {
                q.pollFirst();
            }
            if (q.size() >= maxAttempts) {
                allowed[0] = false;
                return q;
            }
            q.addLast(now);
            allowed[0] = true;
            return q;
        });
        return allowed[0];
    }

    // In-memory limiter is dev-only (Redis is used in production), but still
    // sweep drained buckets periodically so long-running dev sessions do not
    // accumulate stale entries.
    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "PT10M")
    void evictDrainedBuckets() {
        long threshold = Instant.now().getEpochSecond() - 3600;
        requestsByKey.entrySet().removeIf(entry -> {
            ArrayDeque<Long> q = entry.getValue();
            Long last = q.peekLast();
            return last != null && last <= threshold;
        });
    }
}
