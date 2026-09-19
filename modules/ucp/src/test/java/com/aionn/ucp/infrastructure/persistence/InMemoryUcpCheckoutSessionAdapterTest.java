package com.aionn.ucp.infrastructure.persistence;

import com.aionn.ucp.domain.model.UcpCheckoutSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryUcpCheckoutSessionAdapterTest {

    private InMemoryUcpCheckoutSessionAdapter adapter;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.parse("2026-09-18T10:00:00Z");
        Clock fixedClock = Clock.fixed(now, ZoneId.of("UTC"));
        adapter = new InMemoryUcpCheckoutSessionAdapter(fixedClock);
    }

    @Test
    void defaultConstructorInitializesNonNull() {
        InMemoryUcpCheckoutSessionAdapter defaultAdapter = new InMemoryUcpCheckoutSessionAdapter();
        assertThat(defaultAdapter.size()).isZero();
    }

    @Test
    void saveAndFindByIdReturnsActiveSession() {
        UcpCheckoutSession session = new UcpCheckoutSession(
                "chk-1", "user-1", "cart-1", "incomplete", "USD", Map.of("sku-1", 1),
                null, null, null, now, now, now.plusSeconds(3600));

        adapter.save(session);

        Optional<UcpCheckoutSession> found = adapter.findById("chk-1");
        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo("chk-1");
        assertThat(adapter.size()).isEqualTo(1);
    }

    @Test
    void saveIgnoresNullSessionOrNullId() {
        adapter.save(null);
        adapter.save(new UcpCheckoutSession(null, "user-1", null, "incomplete", "USD", Map.of(), null, null, null, now,
                now, now.plusSeconds(3600)));
        assertThat(adapter.size()).isZero();
    }

    @Test
    void findByIdReturnsEmptyForNullOrNotFound() {
        assertThat(adapter.findById(null)).isEmpty();
        assertThat(adapter.findById("non-existent")).isEmpty();
    }

    @Test
    void findByIdEvictsAndReturnsEmptyForExpiredSession() {
        UcpCheckoutSession expired = new UcpCheckoutSession(
                "chk-exp", "user-1", "cart-1", "incomplete", "USD", Map.of("sku-1", 1),
                null, null, null, now.minusSeconds(7200), now.minusSeconds(7200), now.minusSeconds(3600));

        adapter.save(expired);
        assertThat(adapter.size()).isEqualTo(1);

        Optional<UcpCheckoutSession> found = adapter.findById("chk-exp");
        assertThat(found).isEmpty();
        assertThat(adapter.size()).isZero();
    }

    @Test
    void findIncompleteByCartIdReturnsMatchingIncompleteSession() {
        UcpCheckoutSession active = new UcpCheckoutSession(
                "chk-cart", "user-1", "cart-42", "incomplete", "USD", Map.of("sku-1", 1),
                null, null, null, now, now, now.plusSeconds(3600));

        adapter.save(active);

        assertThat(adapter.findIncompleteByCartId("cart-42")).contains(active);
        assertThat(adapter.findIncompleteByCartId(null)).isEmpty();
        assertThat(adapter.findIncompleteByCartId("")).isEmpty();
        assertThat(adapter.findIncompleteByCartId("other-cart")).isEmpty();
    }

    @Test
    void findIncompleteByCartIdIgnoresExpiredOrCompletedSession() {
        UcpCheckoutSession expired = new UcpCheckoutSession(
                "chk-exp", "user-1", "cart-exp", "incomplete", "USD", Map.of("sku-1", 1),
                null, null, null, now.minusSeconds(7200), now.minusSeconds(7200), now.minusSeconds(100));
        UcpCheckoutSession completed = new UcpCheckoutSession(
                "chk-comp", "user-1", "cart-comp", "completed", "USD", Map.of("sku-1", 1),
                null, null, "ord-1", now, now, now.plusSeconds(3600));

        adapter.save(expired);
        adapter.save(completed);

        assertThat(adapter.findIncompleteByCartId("cart-exp")).isEmpty();
        assertThat(adapter.findIncompleteByCartId("cart-comp")).isEmpty();
    }

    @Test
    void evictExpiredRemovesOnlyExpiredSessions() {
        UcpCheckoutSession active = new UcpCheckoutSession(
                "chk-active", "user-1", null, "incomplete", "USD", Map.of(),
                null, null, null, now, now, now.plusSeconds(3600));
        UcpCheckoutSession expired = new UcpCheckoutSession(
                "chk-expired", "user-1", null, "incomplete", "USD", Map.of(),
                null, null, null, now.minusSeconds(7200), now.minusSeconds(7200), now.minusSeconds(100));

        adapter.save(active);
        adapter.save(expired);
        assertThat(adapter.size()).isEqualTo(2);

        adapter.evictExpired();

        assertThat(adapter.size()).isEqualTo(1);
        assertThat(adapter.findById("chk-active")).isPresent();
        assertThat(adapter.findById("chk-expired")).isEmpty();
    }

    @Test
    void clearRemovesAllSessions() {
        adapter.save(new UcpCheckoutSession(
                "chk-1", "user-1", null, "incomplete", "USD", Map.of(),
                null, null, null, now, now, now.plusSeconds(3600)));
        adapter.clear();
        assertThat(adapter.size()).isZero();
    }

    @Test
    void updateIfMatchesSucceedsWhenVersionAndStatusMatch() {
        UcpCheckoutSession session = new UcpCheckoutSession(
                "chk-1", "user-1", "cart-1", "incomplete", "USD", Map.of("sku-1", 1),
                null, null, null, now, now, now.plusSeconds(3600), Map.of(), 1L);
        adapter.save(session);

        UcpCheckoutSession updated = session.withUpdatedItems(Map.of("sku-1", 2), null, null, "USD", now);
        assertThat(updated.version()).isEqualTo(2L);

        boolean ok = adapter.updateIfMatches(updated, 1L, "incomplete");
        assertThat(ok).isTrue();

        Optional<UcpCheckoutSession> found = adapter.findById("chk-1");
        assertThat(found).isPresent();
        assertThat(found.get().version()).isEqualTo(2L);
        assertThat(found.get().items().get("sku-1")).isEqualTo(2);
    }

    @Test
    void updateIfMatchesFailsWhenVersionOrStatusMismatch() {
        UcpCheckoutSession session = new UcpCheckoutSession(
                "chk-1", "user-1", "cart-1", "incomplete", "USD", Map.of("sku-1", 1),
                null, null, null, now, now, now.plusSeconds(3600), Map.of(), 1L);
        adapter.save(session);

        UcpCheckoutSession updated = session.withUpdatedItems(Map.of("sku-1", 2), null, null, "USD", now);

        // Wrong version
        assertThat(adapter.updateIfMatches(updated, 99L, "incomplete")).isFalse();

        // Wrong status
        assertThat(adapter.updateIfMatches(updated, 1L, "completed")).isFalse();

        // Not found / null
        assertThat(adapter.updateIfMatches(null, 1L, "incomplete")).isFalse();
        UcpCheckoutSession nonexistent = new UcpCheckoutSession("chk-unknown", "user-1", null, "incomplete", "USD",
                Map.of(), null, null, null, now, now, now.plusSeconds(3600));
        assertThat(adapter.updateIfMatches(nonexistent, 1L, "incomplete")).isFalse();
    }

    @Test
    void saveEnforcesMaxCapacityByEvictingEarliestExpiringSession() {
        // Insert 5000 sessions with future expiration
        for (int i = 0; i < 5000; i++) {
            UcpCheckoutSession s = new UcpCheckoutSession(
                    "chk-" + i, "user-1", null, "incomplete", "USD", Map.of(),
                    null, null, null, now, now, now.plusSeconds(1000 + i));
            adapter.save(s);
        }
        assertThat(adapter.size()).isEqualTo(5000);
        assertThat(adapter.findById("chk-0")).isPresent();

        // Insert 5001st session
        UcpCheckoutSession s5001 = new UcpCheckoutSession(
                "chk-5001", "user-1", null, "incomplete", "USD", Map.of(),
                null, null, null, now, now, now.plusSeconds(99999));
        adapter.save(s5001);

        // Size must not exceed 5000 and chk-0 (earliest expiring) must be evicted
        assertThat(adapter.size()).isEqualTo(5000);
        assertThat(adapter.findById("chk-0")).isEmpty();
        assertThat(adapter.findById("chk-5001")).isPresent();
    }

    @Test
    void evictIfNecessaryPreservesNonExpiredTerminalSessions() {
        UcpCheckoutSession completed = new UcpCheckoutSession(
                "chk-terminal", "user-1", null, "completed", "USD", Map.of(),
                null, null, "ord-1", now, now, now.plusSeconds(3600));
        adapter.save(completed);

        adapter.evictExpired();

        assertThat(adapter.findById("chk-terminal")).isPresent();
        assertThat(adapter.findById("chk-terminal").get().isCompleted()).isTrue();
    }
}
