package com.aionn.catalog.application.service;

import com.aionn.catalog.application.port.out.merchant.MerchantFollowerPersistencePort;
import com.aionn.catalog.application.port.out.merchant.MerchantPersistencePort;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.model.Merchant;
import com.aionn.catalog.domain.valueobject.MerchantStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantFollowServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T00:00:00Z");

    @Mock
    private MerchantPersistencePort merchantPersistencePort;
    @Mock
    private MerchantFollowerPersistencePort merchantFollowerPersistencePort;
    @Mock
    private Merchant merchant;

    @Test
    void followsActiveMerchantIdempotently() {
        when(merchantPersistencePort.findById("MER_1")).thenReturn(Optional.of(merchant));
        when(merchant.getStatus()).thenReturn(MerchantStatus.ACTIVE);

        service().follow("MER_1", "USER_1");

        verify(merchantFollowerPersistencePort).follow("MER_1", "USER_1", NOW);
    }

    @Test
    void rejectsInactiveMerchant() {
        when(merchantPersistencePort.findById("MER_1")).thenReturn(Optional.of(merchant));
        when(merchant.getStatus()).thenReturn(MerchantStatus.SUSPENDED);

        assertThatThrownBy(() -> service().follow("MER_1", "USER_1"))
                .isInstanceOf(CatalogException.class);
    }

    @Test
    void rejectsUnknownMerchant() {
        when(merchantPersistencePort.findById("MER_X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().follow("MER_X", "USER_1"))
                .isInstanceOf(CatalogException.class);
    }

    private MerchantFollowService service() {
        return new MerchantFollowService(merchantPersistencePort, merchantFollowerPersistencePort,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void unfollowDelegatesToTheRepository() {
        service().unfollow("MER_1", "USER_1");

        verify(merchantFollowerPersistencePort).unfollow("MER_1", "USER_1");
    }

    @Test
    void followerCountDelegatesToTheRepository() {
        when(merchantFollowerPersistencePort.countByMerchantId("MER_1")).thenReturn(7L);

        assertThat(service().followerCount("MER_1")).isEqualTo(7L);
    }

    @Test
    void isFollowingRequiresAUser() {
        assertThat(service().isFollowing("MER_1", null)).isFalse();
        when(merchantFollowerPersistencePort.exists("MER_1", "USER_1")).thenReturn(true);
        assertThat(service().isFollowing("MER_1", "USER_1")).isTrue();
    }
}
