package com.aionn.catalog.domain.model;

import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.valueobject.MerchantStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchantTest {

    private static final String MERCHANT_ID = "01HZMER0000000000000000001";
    private static final String OWNER_ID = "01HZOWN0000000000000000001";
    private static final BigDecimal DEFAULT_RATE = new BigDecimal("0.0500");

    @Test
    void registerInitializesAsPendingWithDefaultCommission() {
        Merchant merchant = Merchant.register(MERCHANT_ID, OWNER_ID, "Acme Store", DEFAULT_RATE, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(merchant.getMerchantId()).isEqualTo(MERCHANT_ID);
        assertThat(merchant.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.PENDING);
        assertThat(merchant.getCommissionRate()).isEqualByComparingTo("0.0500");
        assertThat(merchant.pullEvents()).hasSize(1);
    }

    @Test
    void updateProfilePromotesPendingToActive() {
        Merchant merchant = Merchant.register(MERCHANT_ID, OWNER_ID, "Acme", DEFAULT_RATE, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        merchant.pullEvents();

        merchant.updateProfile("Acme Pro", "logo.png", "desc", "01", "Ha Noi", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(merchant.getName()).isEqualTo("Acme Pro");
        assertThat(merchant.getProvinceCode()).isEqualTo("01");
        assertThat(merchant.getProvinceName()).isEqualTo("Ha Noi");
        assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.ACTIVE);
        assertThat(merchant.pullEvents()).hasSize(1);
    }

    @Test
    void updateProfileRejectsBlankName() {
        Merchant merchant = Merchant.register(MERCHANT_ID, OWNER_ID, "Acme", DEFAULT_RATE, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThatThrownBy(() -> merchant.updateProfile(" ", null, null, null, null, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void updateCommissionRateRejectsValuesOutsideRange() {
        Merchant merchant = Merchant.register(MERCHANT_ID, OWNER_ID, "Acme", DEFAULT_RATE, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThatThrownBy(() -> merchant.updateCommissionRate(new BigDecimal("1.5"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)))
                .isInstanceOf(CatalogException.class);
        assertThatThrownBy(() -> merchant.updateCommissionRate(new BigDecimal("-0.1"), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)))
                .isInstanceOf(CatalogException.class);
    }

    @Test
    void suspendTransitionsActiveMerchantToSuspended() {
        Merchant merchant = Merchant.register(MERCHANT_ID, OWNER_ID, "Acme", DEFAULT_RATE, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        merchant.updateProfile("Acme Pro", null, null, null, null, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
        merchant.pullEvents();

        merchant.suspend("admin-1", "policy violation", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.SUSPENDED);
        assertThat(merchant.pullEvents()).hasSize(1);
    }
}
