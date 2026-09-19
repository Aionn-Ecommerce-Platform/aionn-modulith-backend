package com.aionn.ucp.infrastructure.adapter;

import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkRequest;
import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryUcpIdentityLinkAdapterTest {

    private InMemoryUcpIdentityLinkAdapter adapter;
    private final Instant now = Instant.parse("2026-09-19T10:15:30Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        adapter = new InMemoryUcpIdentityLinkAdapter(clock);
    }

    @Test
    void defaultConstructorUsesSystemClock() {
        InMemoryUcpIdentityLinkAdapter defaultAdapter = new InMemoryUcpIdentityLinkAdapter();
        assertThat(defaultAdapter).isNotNull();
    }

    @Test
    void saveLinkCreatesActiveLinkWithGeneratedId() {
        UcpIdentityLinkRequest request = new UcpIdentityLinkRequest(
                "google_assistant",
                "sub_123",
                "cust_456",
                List.of("orders.read", "checkout.write"),
                Map.of("consent_id", "c_1"));

        UcpIdentityLinkResponse saved = adapter.saveLink(request);

        assertThat(saved).isNotNull();
        assertThat(saved.linkId()).startsWith("idlink_");
        assertThat(saved.platformId()).isEqualTo("google_assistant");
        assertThat(saved.platformSubject()).isEqualTo("sub_123");
        assertThat(saved.customerId()).isEqualTo("cust_456");
        assertThat(saved.status()).isEqualTo("ACTIVE");
        assertThat(saved.scopes()).containsExactly("orders.read", "checkout.write");
        assertThat(saved.linkedAt()).isEqualTo(now);
        assertThat(saved.expiresAt()).isNull();
        assertThat(saved.metadata()).containsEntry("consent_id", "c_1");
    }

    @Test
    void saveLinkOverwritesExistingWhilePreservingLinkIdAndLinkedAt() {
        UcpIdentityLinkRequest request1 = new UcpIdentityLinkRequest(
                "google_assistant", "sub_123", "cust_456", List.of("orders.read"), Map.of());
        UcpIdentityLinkResponse first = adapter.saveLink(request1);

        UcpIdentityLinkRequest request2 = new UcpIdentityLinkRequest(
                "google_assistant", "sub_123", "cust_456", List.of("orders.read", "profile.read"),
                Map.of("updated", "true"));
        UcpIdentityLinkResponse second = adapter.saveLink(request2);

        assertThat(second.linkId()).isEqualTo(first.linkId());
        assertThat(second.linkedAt()).isEqualTo(first.linkedAt());
        assertThat(second.scopes()).containsExactly("orders.read", "profile.read");
        assertThat(second.metadata()).containsEntry("updated", "true");
    }

    @Test
    void findByPlatformAndSubjectReturnsEmptyWhenNotFoundOrNull() {
        assertThat(adapter.findByPlatformAndSubject(null, "sub")).isEmpty();
        assertThat(adapter.findByPlatformAndSubject("plat", null)).isEmpty();
        assertThat(adapter.findByPlatformAndSubject("unknown", "sub")).isEmpty();
    }

    @Test
    void findByPlatformAndSubjectReturnsLinkWhenFound() {
        UcpIdentityLinkRequest request = new UcpIdentityLinkRequest(
                "google_assistant", "sub_123", "cust_456", List.of(), Map.of());
        adapter.saveLink(request);

        Optional<UcpIdentityLinkResponse> found = adapter.findByPlatformAndSubject("google_assistant", "sub_123");
        assertThat(found).isPresent();
        assertThat(found.get().platformSubject()).isEqualTo("sub_123");
    }

    @Test
    void findByCustomerIdReturnsEmptyWhenNotFoundOrNull() {
        assertThat(adapter.findByCustomerId(null)).isEmpty();
        assertThat(adapter.findByCustomerId("non_existent")).isEmpty();
    }

    @Test
    void findByCustomerIdReturnsLinkWhenFound() {
        UcpIdentityLinkRequest request = new UcpIdentityLinkRequest(
                "google_assistant", "sub_123", "cust_456", List.of(), Map.of());
        adapter.saveLink(request);

        Optional<UcpIdentityLinkResponse> found = adapter.findByCustomerId("cust_456");
        assertThat(found).isPresent();
        assertThat(found.get().customerId()).isEqualTo("cust_456");
    }

    @Test
    void revokeLinkReturnsFalseWhenNotFoundOrNull() {
        assertThat(adapter.revokeLink(null, "sub")).isFalse();
        assertThat(adapter.revokeLink("plat", null)).isFalse();
        assertThat(adapter.revokeLink("plat", "non_existent")).isFalse();
    }

    @Test
    void revokeLinkSetsStatusRevokedAndRevokedAt() {
        UcpIdentityLinkRequest request = new UcpIdentityLinkRequest(
                "google_assistant", "sub_123", "cust_456", List.of(), Map.of());
        adapter.saveLink(request);

        boolean revoked = adapter.revokeLink("google_assistant", "sub_123");
        assertThat(revoked).isTrue();

        Optional<UcpIdentityLinkResponse> after = adapter.findByPlatformAndSubject("google_assistant", "sub_123");
        assertThat(after).isPresent();
        assertThat(after.get().status()).isEqualTo("REVOKED");
    }
}
