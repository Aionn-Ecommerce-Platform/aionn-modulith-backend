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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantFollowServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T00:00:00Z");

    @Mock
    private MerchantPersistencePort merchantRepository;
    @Mock
    private MerchantFollowerPersistencePort followerRepository;
    @Mock
    private Merchant merchant;

    @Test
    void followsActiveMerchantIdempotently() {
        when(merchantRepository.findById("MER_1")).thenReturn(Optional.of(merchant));
        when(merchant.getStatus()).thenReturn(MerchantStatus.ACTIVE);

        service().follow("MER_1", "USER_1");

        verify(followerRepository).follow("MER_1", "USER_1", NOW);
    }

    @Test
    void rejectsInactiveMerchant() {
        when(merchantRepository.findById("MER_1")).thenReturn(Optional.of(merchant));
        when(merchant.getStatus()).thenReturn(MerchantStatus.SUSPENDED);

        assertThatThrownBy(() -> service().follow("MER_1", "USER_1"))
                .isInstanceOf(CatalogException.class);
    }

    @Test
    void rejectsUnknownMerchant() {
        when(merchantRepository.findById("MER_X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().follow("MER_X", "USER_1"))
                .isInstanceOf(CatalogException.class);
    }

    private MerchantFollowService service() {
        return new MerchantFollowService(merchantRepository, followerRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
