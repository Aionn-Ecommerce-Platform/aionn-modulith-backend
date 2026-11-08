package com.aionn.ucp.infrastructure.persistence;

import com.aionn.ucp.application.port.out.UcpCheckoutSessionPort;
import com.aionn.ucp.domain.model.UcpCheckoutSession;
import com.aionn.ucp.infrastructure.persistence.entity.UcpCheckoutSessionEntity;
import com.aionn.ucp.infrastructure.persistence.repository.UcpCheckoutSessionRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * JPA-backed {@link UcpCheckoutSessionPort}. The checkout id is the protocol's idempotency handle, so
 * sessions must survive restarts: a retried {@code complete} on any node must replay the same order, not
 * 404 into a duplicate. Expiry is enforced on read; mutations use their own transaction so the
 * {@code completing} reservation commits before placement and the CAS stays out of the caller's tx.
 */
@Component
@Primary
public class JpaUcpCheckoutSessionAdapter implements UcpCheckoutSessionPort {

    private final UcpCheckoutSessionRepository repository;
    private final Clock clock;

    public JpaUcpCheckoutSessionAdapter(UcpCheckoutSessionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UcpCheckoutSession save(UcpCheckoutSession session) {
        if (session == null || session.id() == null) {
            return session;
        }
        repository.save(toEntity(session));
        return session;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean updateIfMatches(UcpCheckoutSession session, long expectedVersion, String expectedStatus) {
        if (session == null || session.id() == null) {
            return false;
        }
        int updated = repository.updateIfMatches(toEntity(session), expectedVersion, expectedStatus);
        return updated > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UcpCheckoutSession> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return repository.findById(id)
                .filter(e -> !isExpired(e.getExpiresAt()))
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UcpCheckoutSession> findIncompleteByCartId(String cartId) {
        if (cartId == null || cartId.isBlank()) {
            return Optional.empty();
        }
        return repository.findIncompleteByCartId(cartId, now()).stream()
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UcpCheckoutSession> findByOrderId(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return Optional.empty();
        }
        return repository.findByOrderId(orderId, now()).map(this::toDomain);
    }

    /** Deletes expired sessions; run by the housekeeping job, safe to call any time. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int evictExpired() {
        return repository.deleteExpired(now());
    }

    private Instant now() {
        return clock.instant();
    }

    private boolean isExpired(Instant expiresAt) {
        return expiresAt != null && now().isAfter(expiresAt);
    }

    private UcpCheckoutSessionEntity toEntity(UcpCheckoutSession s) {
        return UcpCheckoutSessionEntity.builder()
                .id(s.id())
                .userId(s.userId())
                .cartId(s.cartId())
                .status(s.status())
                .currency(s.currency())
                .items(s.items())
                .buyer(s.buyer())
                .context(s.context())
                .orderId(s.orderId())
                .createdAt(s.createdAt())
                .updatedAt(s.updatedAt())
                .expiresAt(s.expiresAt())
                .priceSnapshot(s.priceSnapshot())
                .payment(s.payment())
                .fulfillment(s.fulfillment())
                .voucherCode(s.voucherCode())
                .version(s.version())
                .build();
    }

    private UcpCheckoutSession toDomain(UcpCheckoutSessionEntity e) {
        return new UcpCheckoutSession(
                e.getId(),
                e.getUserId(),
                e.getCartId(),
                e.getStatus(),
                e.getCurrency(),
                e.getItems(),
                e.getBuyer(),
                e.getContext(),
                e.getOrderId(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getExpiresAt(),
                e.getPriceSnapshot(),
                e.getPayment(),
                e.getFulfillment(),
                e.getVoucherCode(),
                e.getVersion());
    }
}
