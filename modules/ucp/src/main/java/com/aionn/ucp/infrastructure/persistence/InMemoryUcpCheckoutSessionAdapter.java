package com.aionn.ucp.infrastructure.persistence;

import com.aionn.ucp.application.port.out.UcpCheckoutSessionPort;
import com.aionn.ucp.domain.model.UcpCheckoutSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory adapter implementing UcpCheckoutSessionPort.
 * Isolates protocol-session state without introducing unnecessary database
 * tables.
 * Includes bounded-size and TTL-based eviction for expired sessions.
 */
@Component
public class InMemoryUcpCheckoutSessionAdapter implements UcpCheckoutSessionPort {

    private static final int MAX_CAPACITY = 5000;
    private final Map<String, UcpCheckoutSession> store = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public InMemoryUcpCheckoutSessionAdapter(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    public InMemoryUcpCheckoutSessionAdapter() {
        this(Clock.systemUTC());
    }

    @Override
    public UcpCheckoutSession save(UcpCheckoutSession session) {
        if (session != null && session.id() != null) {
            evictIfNecessary();
            store.put(session.id(), session);
        }
        return session;
    }

    @Override
    public Optional<UcpCheckoutSession> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        UcpCheckoutSession session = store.get(id);
        if (session != null && session.isExpired(clock.instant())) {
            store.remove(id);
            return Optional.empty();
        }
        return Optional.ofNullable(session);
    }

    @Override
    public Optional<UcpCheckoutSession> findIncompleteByCartId(String cartId) {
        if (cartId == null || cartId.isBlank()) {
            return Optional.empty();
        }
        Instant now = clock.instant();
        return store.values().stream()
                .filter(session -> !session.isExpired(now))
                .filter(session -> cartId.equals(session.cartId()))
                .filter(session -> "incomplete".equalsIgnoreCase(session.status())
                        || "ready_for_complete".equalsIgnoreCase(session.status()))
                .findFirst();
    }

    public void evictExpired() {
        Instant now = clock.instant();
        store.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private void evictIfNecessary() {
        if (store.size() >= MAX_CAPACITY) {
            evictExpired();
            if (store.size() >= MAX_CAPACITY) {
                store.entrySet().removeIf(entry -> entry.getValue().isCompleted() || entry.getValue().isCanceled());
            }
        }
    }

    public int size() {
        return store.size();
    }

    public void clear() {
        store.clear();
    }
}
