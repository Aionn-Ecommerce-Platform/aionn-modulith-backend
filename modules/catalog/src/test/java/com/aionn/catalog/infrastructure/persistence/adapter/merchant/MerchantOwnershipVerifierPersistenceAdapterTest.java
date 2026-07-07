package com.aionn.catalog.infrastructure.persistence.adapter.merchant;

import com.aionn.catalog.application.port.out.merchant.MerchantPersistencePort;
import com.aionn.catalog.domain.model.Merchant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantOwnershipVerifierPersistenceAdapterTest {

    @Mock
    private MerchantPersistencePort merchantRepository;

    @InjectMocks
    private MerchantOwnershipVerifierPersistenceAdapter adapter;

    @Test
    void returnsTrueWhenOwnerMatches() {
        Merchant merchant = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.0500"));
        when(merchantRepository.findById("m-1")).thenReturn(Optional.of(merchant));

        assertThat(adapter.isOwnedBy("m-1", "owner-1")).isTrue();
    }

    @Test
    void returnsFalseWhenOwnerDoesNotMatch() {
        Merchant merchant = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.0500"));
        when(merchantRepository.findById("m-1")).thenReturn(Optional.of(merchant));

        assertThat(adapter.isOwnedBy("m-1", "other-owner")).isFalse();
    }

    @Test
    void returnsFalseWhenMerchantMissing() {
        when(merchantRepository.findById("missing")).thenReturn(Optional.empty());

        assertThat(adapter.isOwnedBy("missing", "owner-1")).isFalse();
    }

    @Test
    void returnsFalseForNullOrBlankIds() {
        assertThat(adapter.isOwnedBy(null, "owner-1")).isFalse();
        assertThat(adapter.isOwnedBy("m-1", null)).isFalse();
        assertThat(adapter.isOwnedBy("", "owner-1")).isFalse();
        assertThat(adapter.isOwnedBy("m-1", "")).isFalse();
    }
}
