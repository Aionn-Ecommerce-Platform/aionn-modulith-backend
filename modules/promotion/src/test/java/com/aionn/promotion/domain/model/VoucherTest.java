package com.aionn.promotion.domain.model;

import com.aionn.promotion.domain.exception.PromotionErrorCode;
import com.aionn.promotion.domain.exception.PromotionException;
import com.aionn.promotion.domain.valueobject.VoucherScope;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VoucherTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Money DISCOUNT = Money.of(BigDecimal.valueOf(50_000), "VND");

    private static Voucher platform(int usageLimit, Instant validFrom, Instant validUntil) {
        return Voucher.issue("PLATFORM50K", "CAMP_1", DISCOUNT, usageLimit, validFrom, validUntil, CLOCK);
    }

    @Test
    void platformVoucherAppliesToEveryMerchant() {
        Voucher voucher = platform(100, NOW.minusSeconds(60), NOW.plusSeconds(3600));

        assertThat(voucher.getScope()).isEqualTo(VoucherScope.PLATFORM);
        assertThat(voucher.getMerchantId()).isNull();
        assertThat(voucher.appliesToMerchant("MERCHANT_2")).isTrue();
        assertThat(voucher.getCreatedAt()).isEqualTo(NOW);
        assertThat(voucher.pullEvents()).hasSize(1);
    }

    @Test
    void shopVoucherOnlyAppliesToItsOwnerShop() {
        Voucher voucher = Voucher.issueForShop("SHOP50K", "MERCHANT_1", DISCOUNT, 100,
                NOW.minusSeconds(60), NOW.plusSeconds(3600), CLOCK);

        assertThat(voucher.getScope()).isEqualTo(VoucherScope.SHOP);
        assertThat(voucher.getCampaignId()).isNull();
        assertThat(voucher.appliesToMerchant("MERCHANT_1")).isTrue();
        assertThat(voucher.appliesToMerchant("MERCHANT_2")).isFalse();
    }

    @Test
    void issueWithoutClockUsesSystemTime() {
        Voucher voucher = Voucher.issue("V-1", "CAMP_1", DISCOUNT, 10, null, null);

        assertThat(voucher.getCreatedAt()).isNotNull();
    }

    @Test
    void issueForShopWithoutClockUsesSystemTime() {
        Voucher voucher = Voucher.issueForShop("V-2", "MERCHANT_1", DISCOUNT, 10, null, null);

        assertThat(voucher.getCreatedAt()).isNotNull();
    }

    @Test
    void issueRejectsNonPositiveUsageLimit() {
        assertThatThrownBy(() -> platform(0, null, null))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void issueRejectsInvertedValidityWindow() {
        assertThatThrownBy(() -> platform(10, NOW.plusSeconds(60), NOW))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void issueForShopRejectsBlankMerchantId() {
        assertThatThrownBy(() -> Voucher.issueForShop("V-3", " ", DISCOUNT, 10, null, null, CLOCK))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void issueForShopRejectsNonPositiveUsageLimit() {
        assertThatThrownBy(() -> Voucher.issueForShop("V-4", "MERCHANT_1", DISCOUNT, 0, null, null, CLOCK))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void issueForShopRejectsInvertedValidityWindow() {
        assertThatThrownBy(() -> Voucher.issueForShop("V-5", "MERCHANT_1", DISCOUNT, 10,
                NOW.plusSeconds(60), NOW, CLOCK))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void isValidNowHandlesOpenEndedWindows() {
        Voucher open = platform(10, null, null);

        assertThat(open.isValidNow(NOW)).isTrue();
        assertThat(open.isValidNow(NOW.plusSeconds(100_000))).isTrue();
    }

    @Test
    void isValidNowRejectsOutsideWindow() {
        Voucher voucher = platform(10, NOW, NOW.plusSeconds(3600));

        assertThat(voucher.isValidNow(NOW.minusSeconds(1))).isFalse();
        assertThat(voucher.isValidNow(NOW.plusSeconds(3601))).isFalse();
        assertThat(voucher.isValidNow(NOW.plusSeconds(60))).isTrue();
    }

    @Test
    void remainingUsesNeverGoesNegative() {
        Voucher voucher = platform(1, null, null);
        voucher.reserveSlot(CLOCK);
        voucher.commitSlot(CLOCK);

        assertThat(voucher.getUsedCount()).isEqualTo(1);
        assertThat(voucher.remainingUses()).isZero();
        assertThat(voucher.isUsable(NOW)).isFalse();
    }

    @Test
    void isUsableRequiresValidWindowAndRemainingUses() {
        Voucher voucher = platform(5, NOW, NOW.plusSeconds(3600));

        assertThat(voucher.isUsable(NOW.plusSeconds(60))).isTrue();
        assertThat(voucher.isUsable(NOW.plusSeconds(100_000))).isFalse();
    }

    @Test
    void claimSlotRejectsExpiredVoucher() {
        Voucher voucher = platform(5, NOW.minusSeconds(600), NOW.minusSeconds(1));

        assertThatThrownBy(() -> voucher.claimSlot(CLOCK))
                .isInstanceOf(PromotionException.class)
                .hasFieldOrPropertyWithValue("errorCode", PromotionErrorCode.VOUCHER_EXPIRED.getCode());
    }

    @Test
    void claimSlotRejectsExhaustedVoucher() {
        Voucher voucher = platform(1, null, null);
        voucher.reserveSlot(CLOCK);
        voucher.commitSlot(CLOCK);

        assertThatThrownBy(() -> voucher.claimSlot(CLOCK))
                .isInstanceOf(PromotionException.class)
                .hasFieldOrPropertyWithValue("errorCode", PromotionErrorCode.VOUCHER_NO_USAGE_LEFT.getCode());
    }

    @Test
    void claimSlotWithoutClockUsesSystemTime() {
        Voucher voucher = platform(5, null, null);

        voucher.claimSlot();

        assertThat(voucher.getUsedCount()).isZero();
    }

    @Test
    void reserveSlotRejectsExpiredVoucher() {
        Voucher voucher = platform(5, NOW.minusSeconds(600), NOW.minusSeconds(1));

        assertThatThrownBy(() -> voucher.reserveSlot(CLOCK))
                .isInstanceOf(PromotionException.class)
                .hasFieldOrPropertyWithValue("errorCode", PromotionErrorCode.VOUCHER_EXPIRED.getCode());
    }

    @Test
    void reserveCommitAndReleaseTouchUpdatedAt() {
        Voucher voucher = platform(5, null, null);

        voucher.reserveSlot(CLOCK);
        assertThat(voucher.getUpdatedAt()).isEqualTo(NOW);

        voucher.commitSlot(CLOCK);
        assertThat(voucher.getUpdatedAt()).isEqualTo(NOW);

        voucher.releaseCommittedSlot(CLOCK);
        assertThat(voucher.getUpdatedAt()).isEqualTo(NOW);
        assertThat(voucher.getUsedCount()).isZero();
    }

    @Test
    void slotOperationsWithoutClockUseSystemTime() {
        Voucher voucher = platform(5, null, null);

        voucher.reserveSlot();
        voucher.commitSlot();
        voucher.releaseCommittedSlot(Clock.systemUTC());

        assertThat(voucher.getUpdatedAt()).isNotNull();
        assertThat(voucher.getReservedCount()).isZero();
    }
}
