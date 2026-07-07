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
        Merchant merchant = Merchant.register(MERCHANT_ID, OWNER_ID, "Acme Store", DEFAULT_RATE);

        assertThat(merchant.getMerchantId()).isEqualTo(MERCHANT_ID);
        assertThat(merchant.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.PENDING);
        assertThat(merchant.getCommissionRate()).isEqualByComparingTo("0.0500");
        assertThat(merchant.pullEvents()).hasSize(1);
    }

    @Test
    void updateProfilePromotesPendingToActive() {
        Merchant merchant = Merchant.register(MERCHANT_ID, OWNER_ID, "Acme", DEFAULT_RATE);
        merchant.pullEvents();

        merchant.updateProfile("Acme Pro", "logo.png", "desc", "01", "Ha Noi");

        assertThat(merchant.getName()).isEqualTo("Acme Pro");
        assertThat(merchant.getProvinceCode()).isEqualTo("01");
        assertThat(merchant.getProvinceName()).isEqualTo("Ha Noi");
        assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.ACTIVE);
        assertThat(merchant.pullEvents()).hasSize(1);
    }

    @Test
    void updateProfileRejectsBlankName() {
        Merchant merchant = Merchant.register(MERCHANT_ID, OWNER_ID, "Acme", DEFAULT_RATE);

        assertThatThrownBy(() -> merchant.updateProfile(" ", null, null, null, null))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void updateCommissionRateRejectsValuesOutsideRange() {
        Merchant merchant = Merchant.register(MERCHANT_ID, OWNER_ID, "Acme", DEFAULT_RATE);

        assertThatThrownBy(() -> merchant.updateCommissionRate(new BigDecimal("1.5")))
                .isInstanceOf(CatalogException.class);
        assertThatThrownBy(() -> merchant.updateCommissionRate(new BigDecimal("-0.1")))
                .isInstanceOf(CatalogException.class);
    }

    @Test
    void suspendTransitionsActiveMerchantToSuspended() {
        Merchant merchant = Merchant.register(MERCHANT_ID, OWNER_ID, "Acme", DEFAULT_RATE);
        merchant.updateProfile("Acme Pro", null, null, null, null);
        merchant.pullEvents();

        merchant.suspend("admin-1", "policy violation");

        assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.SUSPENDED);
        assertThat(merchant.pullEvents()).hasSize(1);
    }
}
