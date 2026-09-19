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
    public synchronized UcpCheckoutSession save(UcpCheckoutSession session) {
        if (session != null && session.id() != null) {
            evictIfNecessary();
            if (store.size() >= MAX_CAPACITY && !store.containsKey(session.id())) {
                store.entrySet().stream()
                        .min((e1, e2) -> {
                            Instant exp1 = e1.getValue().expiresAt() != null ? e1.getValue().expiresAt() : Instant.MAX;
                            Instant exp2 = e2.getValue().expiresAt() != null ? e2.getValue().expiresAt() : Instant.MAX;
                            int cmp = exp1.compareTo(exp2);
                            if (cmp != 0) {
                                return cmp;
                            }
                            Instant cr1 = e1.getValue().createdAt() != null ? e1.getValue().createdAt() : Instant.MAX;
                            Instant cr2 = e2.getValue().createdAt() != null ? e2.getValue().createdAt() : Instant.MAX;
                            return cr1.compareTo(cr2);
                        })
                        .map(Map.Entry::getKey)
                        .ifPresent(store::remove);
            }
            if (store.size() < MAX_CAPACITY || store.containsKey(session.id())) {
                store.put(session.id(), session);
            }
        }
        return session;
    }

    @Override
    public synchronized boolean updateIfMatches(UcpCheckoutSession session, long expectedVersion, String expectedStatus) {
        if (session == null || session.id() == null) {
            return false;
        }
        UcpCheckoutSession current = store.get(session.id());
        if (current == null) {
            return false;
        }
        if (current.version() != expectedVersion) {
            return false;
        }
        if (expectedStatus != null && !expectedStatus.equalsIgnoreCase(current.status())) {
            return false;
        }
        store.put(session.id(), session);
        return true;
    }

    @Override
    public Optional<UcpCheckoutSession> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        while (true) {
            UcpCheckoutSession session = store.get(id);
            if (session == null) {
                return Optional.empty();
            }
            if (session.isExpired(clock.instant())) {
                if (store.remove(id, session)) {
                    return Optional.empty();
                }
                // If conditional removal returned false, a newer session was stored concurrently; retry lookup
                continue;
            }
            return Optional.of(session);
        }
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
        }
    }

    public int size() {
        return store.size();
    }

    public void clear() {
        store.clear();
    }
}
