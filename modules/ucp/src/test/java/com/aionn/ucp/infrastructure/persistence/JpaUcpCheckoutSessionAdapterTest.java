package com.aionn.ucp.infrastructure.persistence;

import com.aionn.ucp.domain.model.UcpCheckoutSession;
import com.aionn.ucp.infrastructure.persistence.entity.UcpCheckoutSessionEntity;
import com.aionn.ucp.infrastructure.persistence.repository.UcpCheckoutSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaUcpCheckoutSessionAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");

    @Mock
    private UcpCheckoutSessionRepository repository;

    private JpaUcpCheckoutSessionAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaUcpCheckoutSessionAdapter(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void saveIgnoresSessionsWithoutIdentity() {
        assertThat(adapter.save(null)).isNull();
        assertThat(adapter.save(session(null, "incomplete", NOW.plusSeconds(60)))).isNotNull();

        verify(repository, never()).save(any());
    }

    @Test
    void saveMapsTheSessionOntoTheEntity() {
        UcpCheckoutSession session = session("chk-1", "incomplete", NOW.plusSeconds(60));

        assertThat(adapter.save(session)).isSameAs(session);

        verify(repository).save(any(UcpCheckoutSessionEntity.class));
    }

    @Test
    void updateIfMatchesReportsWhetherTheCompareAndSetApplied() {
        UcpCheckoutSession session = session("chk-1", "incomplete", NOW.plusSeconds(60));
        when(repository.updateIfMatches(any(), eq(3L), eq("incomplete"))).thenReturn(1);
        when(repository.updateIfMatches(any(), eq(4L), eq("incomplete"))).thenReturn(0);

        assertThat(adapter.updateIfMatches(session, 3L, "incomplete")).isTrue();
        assertThat(adapter.updateIfMatches(session, 4L, "incomplete")).isFalse();
        assertThat(adapter.updateIfMatches(null, 1L, "incomplete")).isFalse();
    }

    @Test
    void findByIdReturnsExpiredSessionsOnlyWhenTheyAreTerminal() {
        when(repository.findById("expired-open"))
                .thenReturn(Optional.of(entity("expired-open", "incomplete", NOW.minusSeconds(60))));
        when(repository.findById("expired-done"))
                .thenReturn(Optional.of(entity("expired-done", "completed", NOW.minusSeconds(60))));
        when(repository.findById("active"))
                .thenReturn(Optional.of(entity("active", "incomplete", NOW.plusSeconds(60))));

        assertThat(adapter.findById("expired-open")).isEmpty();
        assertThat(adapter.findById("expired-done")).isPresent();
        assertThat(adapter.findById("active")).isPresent();
        assertThat(adapter.findById(null)).isEmpty();
    }

    @Test
    void lookupsByCartAndOrderRejectBlankKeys() {
        assertThat(adapter.findIncompleteByCartId(" ")).isEmpty();
        assertThat(adapter.findByOrderId(null)).isEmpty();

        verify(repository, never()).findIncompleteByCartId(any(), any());
        verify(repository, never()).findByOrderId(any());
    }

    @Test
    void lookupsByCartAndOrderMapTheFirstMatch() {
        when(repository.findIncompleteByCartId("cart-1", NOW))
                .thenReturn(List.of(entity("chk-1", "incomplete", NOW.plusSeconds(60))));
        when(repository.findByOrderId("ord-1"))
                .thenReturn(Optional.of(entity("chk-1", "completed", NOW.plusSeconds(60))));

        assertThat(adapter.findIncompleteByCartId("cart-1")).isPresent();
        assertThat(adapter.findByOrderId("ord-1")).isPresent();
    }

    @Test
    void evictExpiredDelegatesWithTheCurrentClock() {
        when(repository.deleteExpired(NOW, NOW.minusSeconds(3600))).thenReturn(2);

        assertThat(adapter.evictExpired(NOW.minusSeconds(3600))).isEqualTo(2);
    }

    @Test
    void aMissingClockFallsBackToSystemTime() {
        assertThat(new JpaUcpCheckoutSessionAdapter(repository, null)).isNotNull();
    }

    private static UcpCheckoutSession session(String id, String status, Instant expiresAt) {
        return new UcpCheckoutSession(id, "user-1", "cart-1", status, "VND", Map.of("sku-1", 1),
                Map.of("name", "Buyer"), Map.of("ip", "127.0.0.1"), null,
                NOW, NOW, expiresAt, Map.of("sku-1", 1000L), Map.of("handler", "vnpay"),
                Map.of("service", "standard"), "SUMMER", 0L);
    }

    private static UcpCheckoutSessionEntity entity(String id, String status, Instant expiresAt) {
        return UcpCheckoutSessionEntity.builder()
                .id(id)
                .userId("user-1")
                .cartId("cart-1")
                .status(status)
                .currency("VND")
                .items(Map.of("sku-1", 1))
                .buyer(Map.of())
                .context(Map.of())
                .createdAt(NOW)
                .updatedAt(NOW)
                .expiresAt(expiresAt)
                .priceSnapshot(Map.of("sku-1", 1000L))
                .payment(Map.of())
                .fulfillment(Map.of())
                .version(0L)
                .build();
    }
}
