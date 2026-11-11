package com.aionn.ucp.infrastructure.persistence;

import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkRequest;
import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkResponse;
import com.aionn.ucp.infrastructure.persistence.entity.UcpIdentityLinkEntity;
import com.aionn.ucp.infrastructure.persistence.repository.UcpIdentityLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaUcpIdentityLinkAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");

    @Mock
    private UcpIdentityLinkRepository repository;
    @Mock
    private PlatformTransactionManager transactionManager;

    private JpaUcpIdentityLinkAdapter adapter;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(transactionManager.getTransaction(any()))
                .thenReturn(mock(TransactionStatus.class));
        adapter = new JpaUcpIdentityLinkAdapter(repository, Clock.fixed(NOW, ZoneOffset.UTC),
                transactionManager);
    }

    @Test
    void createsANewLinkWhenNoneExists() {
        when(repository.findByPlatformIdAndPlatformSubject("platform-1", "subject-1"))
                .thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(UcpIdentityLinkEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UcpIdentityLinkResponse response = adapter.saveLink(
                new UcpIdentityLinkRequest("platform-1", "subject-1", "customer-1",
                        List.of("checkout"), Map.of("agent", "a")));

        ArgumentCaptor<UcpIdentityLinkEntity> captor = ArgumentCaptor.forClass(UcpIdentityLinkEntity.class);
        verify(repository).saveAndFlush(captor.capture());
        UcpIdentityLinkEntity saved = captor.getValue();
        assertThat(saved.getLinkId()).startsWith("idlink_");
        assertThat(saved.getPlatformId()).isEqualTo("platform-1");
        assertThat(saved.getCustomerId()).isEqualTo("customer-1");
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        assertThat(saved.getLinkedAt()).isEqualTo(NOW);
        assertThat(saved.getExpiresAt()).isNull();
        assertThat(response.linkId()).isEqualTo(saved.getLinkId());
        assertThat(response.scopes()).containsExactly("checkout");
    }

    @Test
    void refreshesAnExistingLinkWithoutChangingItsIdentity() {
        UcpIdentityLinkEntity existing = UcpIdentityLinkEntity.builder()
                .linkId("idlink_existing")
                .platformId("platform-1")
                .platformSubject("subject-1")
                .customerId("old-customer")
                .status("REVOKED")
                .linkedAt(NOW.minusSeconds(3600))
                .expiresAt(NOW.plusSeconds(3600))
                .build();
        when(repository.findByPlatformIdAndPlatformSubject("platform-1", "subject-1"))
                .thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(existing)).thenReturn(existing);

        UcpIdentityLinkResponse response = adapter.saveLink(
                new UcpIdentityLinkRequest("platform-1", "subject-1", "new-customer", null, null));

        assertThat(response.linkId()).isEqualTo("idlink_existing");
        assertThat(response.customerId()).isEqualTo("new-customer");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.linkedAt()).isEqualTo(NOW.minusSeconds(3600));
        assertThat(response.expiresAt()).isNull();
        assertThat(response.scopes()).isEmpty();
        assertThat(response.metadata()).isEmpty();
    }

    @Test
    void retriesOnceWhenAConcurrentInsertWinsTheUniqueKey() {
        when(repository.findByPlatformIdAndPlatformSubject("platform-1", "subject-1"))
                .thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(UcpIdentityLinkEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UcpIdentityLinkResponse response = adapter.saveLink(
                new UcpIdentityLinkRequest("platform-1", "subject-1", "customer-1", List.of(), Map.of()));

        assertThat(response.linkId()).startsWith("idlink_");
    }

    @Test
    void lookupsRejectMissingKeys() {
        assertThat(adapter.findByPlatformAndSubject(null, "subject-1")).isEmpty();
        assertThat(adapter.findByPlatformAndSubject("platform-1", null)).isEmpty();
        assertThat(adapter.findByCustomerId(null)).isEmpty();
        assertThat(adapter.revokeLink(null, "subject-1")).isFalse();
        assertThat(adapter.revokeLink("platform-1", null)).isFalse();
    }

    @Test
    void lookupsMapStoredLinks() {
        UcpIdentityLinkEntity entity = UcpIdentityLinkEntity.builder()
                .linkId("idlink_1")
                .platformId("platform-1")
                .platformSubject("subject-1")
                .customerId("customer-1")
                .status("ACTIVE")
                .linkedAt(NOW)
                .build();
        when(repository.findByPlatformIdAndPlatformSubject("platform-1", "subject-1"))
                .thenReturn(Optional.of(entity));
        when(repository.findFirstByCustomerId("customer-1")).thenReturn(Optional.of(entity));

        assertThat(adapter.findByPlatformAndSubject("platform-1", "subject-1")).isPresent();
        assertThat(adapter.findByCustomerId("customer-1")).isPresent();
    }

    @Test
    void revokeReportsWhetherARowChanged() {
        when(repository.revokeByPlatformAndSubject("platform-1", "subject-1", NOW)).thenReturn(1);
        when(repository.revokeByPlatformAndSubject("platform-1", "subject-2", NOW)).thenReturn(0);

        assertThat(adapter.revokeLink("platform-1", "subject-1")).isTrue();
        assertThat(adapter.revokeLink("platform-1", "subject-2")).isFalse();
    }

    @Test
    void aMissingClockFallsBackToSystemTime() {
        assertThat(new JpaUcpIdentityLinkAdapter(repository, null, transactionManager)).isNotNull();
    }
}
