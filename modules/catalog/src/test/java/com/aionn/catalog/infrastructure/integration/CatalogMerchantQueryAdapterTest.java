package com.aionn.catalog.infrastructure.integration;

import com.aionn.catalog.application.port.out.merchant.MerchantPersistencePort;
import com.aionn.catalog.domain.model.Merchant;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort.StripeConnectInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogMerchantQueryAdapterTest {

    @Mock
    private MerchantPersistencePort merchantRepository;

    @InjectMocks
    private CatalogMerchantQueryAdapter adapter;

    @Test
    void findMerchantIdByOwnerIdReturnsIdWhenPresent() {
        Merchant merchant = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"));
        when(merchantRepository.findByOwnerId("owner-1")).thenReturn(Optional.of(merchant));

        assertThat(adapter.findMerchantIdByOwnerId("owner-1")).contains("m-1");
    }

    @Test
    void findMerchantIdByOwnerIdReturnsEmptyForBlankInput() {
        assertThat(adapter.findMerchantIdByOwnerId(null)).isEmpty();
        assertThat(adapter.findMerchantIdByOwnerId(" ")).isEmpty();
    }

    @Test
    void findOwnerIdByMerchantIdReturnsOwner() {
        Merchant merchant = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"));
        when(merchantRepository.findById("m-1")).thenReturn(Optional.of(merchant));

        assertThat(adapter.findOwnerIdByMerchantId("m-1")).contains("owner-1");
    }

    @Test
    void findOwnerIdByMerchantIdReturnsEmptyForBlankInput() {
        assertThat(adapter.findOwnerIdByMerchantId(null)).isEmpty();
        assertThat(adapter.findOwnerIdByMerchantId("")).isEmpty();
    }

    @Test
    void findCommissionRateReturnsRateWhenMerchantExists() {
        Merchant merchant = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.0750"));
        when(merchantRepository.findById("m-1")).thenReturn(Optional.of(merchant));

        assertThat(adapter.findCommissionRate("m-1")).contains(new BigDecimal("0.0750"));
    }

    @Test
    void findCommissionRateReturnsEmptyForBlankInput() {
        assertThat(adapter.findCommissionRate(null)).isEmpty();
        assertThat(adapter.findCommissionRate("")).isEmpty();
    }

    @Test
    void findStripeConnectInfoReturnsEmptyWhenNoStripeAccount() {
        Merchant merchant = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"));
        when(merchantRepository.findById("m-1")).thenReturn(Optional.of(merchant));

        assertThat(adapter.findStripeConnectInfo("m-1")).isEmpty();
    }

    @Test
    void findStripeConnectInfoReturnsInfoWhenLinked() {
        Merchant merchant = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"));
        merchant.linkStripeAccount("acct_test");
        merchant.updateStripeCapabilities(true, false);
        when(merchantRepository.findById("m-1")).thenReturn(Optional.of(merchant));

        Optional<StripeConnectInfo> info = adapter.findStripeConnectInfo("m-1");

        assertThat(info).isPresent();
        assertThat(info.get().stripeAccountId()).isEqualTo("acct_test");
        assertThat(info.get().chargesEnabled()).isTrue();
        assertThat(info.get().payoutsEnabled()).isFalse();
    }

    @Test
    void saveStripeAccountIdPersistsChanges() {
        Merchant merchant = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"));
        when(merchantRepository.findById("m-1")).thenReturn(Optional.of(merchant));

        adapter.saveStripeAccountId("m-1", "acct_test");

        ArgumentCaptor<Merchant> captor = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantRepository).save(captor.capture());
        assertThat(captor.getValue().getStripeAccountId()).isEqualTo("acct_test");
    }

    @Test
    void saveStripeAccountIdThrowsWhenMerchantMissing() {
        when(merchantRepository.findById("m-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.saveStripeAccountId("m-1", "acct_test"))
                .isInstanceOf(com.aionn.catalog.domain.exception.CatalogException.class);

        verify(merchantRepository, never()).save(any(Merchant.class));
    }

    @Test
    void updateStripeCapabilitiesPersistsChanges() {
        Merchant merchant = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"));
        merchant.linkStripeAccount("acct_test");
        when(merchantRepository.findById("m-1")).thenReturn(Optional.of(merchant));

        adapter.updateStripeCapabilities("m-1", true, true);

        ArgumentCaptor<Merchant> captor = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantRepository).save(captor.capture());
        assertThat(captor.getValue().isStripeChargesEnabled()).isTrue();
        assertThat(captor.getValue().isStripePayoutsEnabled()).isTrue();
    }

    @Test
    void updateStripeCapabilitiesThrowsWhenMerchantMissing() {
        when(merchantRepository.findById("m-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.updateStripeCapabilities("m-1", true, true))
                .isInstanceOf(com.aionn.catalog.domain.exception.CatalogException.class);
    }
}
