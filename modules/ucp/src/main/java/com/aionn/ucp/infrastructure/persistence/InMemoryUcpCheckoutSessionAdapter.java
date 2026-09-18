package com.aionn.ucp.infrastructure.persistence;

import com.aionn.ucp.application.port.out.UcpCheckoutSessionPort;
import com.aionn.ucp.domain.model.UcpCheckoutSession;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory adapter implementing UcpCheckoutSessionPort.
 * Isolates protocol-session state without introducing unnecessary database
 * tables.
 */
@Component
public class InMemoryUcpCheckoutSessionAdapter implements UcpCheckoutSessionPort {

    private final Map<String, UcpCheckoutSession> store = new ConcurrentHashMap<>();

    @Override
    public UcpCheckoutSession save(UcpCheckoutSession session) {
        if (session != null && session.id() != null) {
            store.put(session.id(), session);
        }
        return session;
    }

    @Override
    public Optional<UcpCheckoutSession> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<UcpCheckoutSession> findIncompleteByCartId(String cartId) {
        if (cartId == null || cartId.isBlank()) {
            return Optional.empty();
        }
        return store.values().stream()
                .filter(session -> cartId.equals(session.cartId()))
                .filter(session -> "incomplete".equalsIgnoreCase(session.status())
                        || "ready_for_complete".equalsIgnoreCase(session.status()))
                .findFirst();
    }

    public void clear() {
        store.clear();
    }
}
