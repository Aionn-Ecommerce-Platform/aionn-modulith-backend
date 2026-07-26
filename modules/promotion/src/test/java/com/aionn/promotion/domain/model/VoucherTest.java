package com.aionn.promotion.domain.model;

import com.aionn.promotion.domain.valueobject.VoucherScope;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class VoucherTest {

    @Test
    void platformVoucherAppliesToEveryMerchant() {
        Voucher voucher = Voucher.issue("PLATFORM50K", "CAMP_1",
                Money.of(BigDecimal.valueOf(50_000), "VND"), 100,
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600));

        assertEquals(VoucherScope.PLATFORM, voucher.getScope());
        assertNull(voucher.getMerchantId());
        assertTrue(voucher.appliesToMerchant("MERCHANT_2"));
    }

    @Test
    void shopVoucherOnlyAppliesToItsOwnerShop() {
        Voucher voucher = Voucher.issueForShop("SHOP50K", "MERCHANT_1",
                Money.of(BigDecimal.valueOf(50_000), "VND"), 100,
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600));

        assertEquals(VoucherScope.SHOP, voucher.getScope());
        assertNull(voucher.getCampaignId());
        assertTrue(voucher.appliesToMerchant("MERCHANT_1"));
        assertFalse(voucher.appliesToMerchant("MERCHANT_2"));
    }
}
