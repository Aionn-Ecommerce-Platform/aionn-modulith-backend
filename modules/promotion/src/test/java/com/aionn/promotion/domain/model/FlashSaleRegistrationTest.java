package com.aionn.promotion.domain.model;

import com.aionn.promotion.domain.exception.PromotionException;
import com.aionn.promotion.domain.valueobject.FlashSaleRegistrationStatus;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlashSaleRegistrationTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Money SALE_PRICE = Money.of(new BigDecimal("80000"), "VND");
    private static final Money REGULAR_PRICE = Money.of(new BigDecimal("100000"), "VND");

    private static FlashSaleRegistration pending() {
        return FlashSaleRegistration.submit("reg-1", "camp-1", "mer-1", "prod-1", "sku-1",
                SALE_PRICE, 10, CLOCK);
    }

    @Test
    void submitStartsPendingAndRecordsEvent() {
        FlashSaleRegistration reg = pending();

        assertThat(reg.getStatus()).isEqualTo(FlashSaleRegistrationStatus.PENDING);
        assertThat(reg.getSubmittedAt()).isEqualTo(NOW);
        assertThat(reg.getSoldCount()).isZero();
        assertThat(reg.pullEvents()).hasSize(1);
    }

    @Test
    void submitWithoutClockUsesSystemTime() {
        FlashSaleRegistration reg = FlashSaleRegistration.submit("reg-2", "camp-1", "mer-1",
                "prod-1", "sku-1", SALE_PRICE, 5, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(reg.getSubmittedAt()).isNotNull();
    }

    @Test
    void submitRejectsNonPositiveSalePrice() {
        assertThatThrownBy(() -> FlashSaleRegistration.submit("reg-3", "camp-1", "mer-1",
                "prod-1", "sku-1", Money.of(BigDecimal.ZERO, "VND"), 5, CLOCK))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void approveAcceptsDiscountAboveMinimum() {
        FlashSaleRegistration reg = pending();
        reg.pullEvents();

        reg.approve("admin-1", REGULAR_PRICE, CLOCK);

        assertThat(reg.getStatus()).isEqualTo(FlashSaleRegistrationStatus.APPROVED);
        assertThat(reg.getDecidedBy()).isEqualTo("admin-1");
        assertThat(reg.getDecidedAt()).isEqualTo(NOW);
        assertThat(reg.pullEvents()).hasSize(1);
    }

    @Test
    void approveRejectsDiscountBelowMinimum() {
        FlashSaleRegistration reg = FlashSaleRegistration.submit("reg-4", "camp-1", "mer-1",
                "prod-1", "sku-1", Money.of(new BigDecimal("95000"), "VND"), 10, CLOCK);

        assertThatThrownBy(() -> reg.approve("admin-1", REGULAR_PRICE, CLOCK))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void approveSkipsDiscountCheckWhenVariantPriceUnknown() {
        FlashSaleRegistration reg = pending();

        reg.approve("admin-1", null, CLOCK);

        assertThat(reg.getStatus()).isEqualTo(FlashSaleRegistrationStatus.APPROVED);
    }

    @Test
    void approveWithoutClockUsesSystemTime() {
        FlashSaleRegistration reg = pending();

        reg.approve("admin-1", REGULAR_PRICE, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(reg.getDecidedAt()).isNotNull();
    }

    @Test
    void approveRejectsAlreadyDecidedRegistration() {
        FlashSaleRegistration reg = pending();
        reg.approve("admin-1", REGULAR_PRICE, CLOCK);

        assertThatThrownBy(() -> reg.approve("admin-2", REGULAR_PRICE, CLOCK))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void rejectStoresReason() {
        FlashSaleRegistration reg = pending();
        reg.pullEvents();

        reg.reject("admin-1", "price too high", CLOCK);

        assertThat(reg.getStatus()).isEqualTo(FlashSaleRegistrationStatus.REJECTED);
        assertThat(reg.getRejectReason()).isEqualTo("price too high");
        assertThat(reg.pullEvents()).hasSize(1);
    }

    @Test
    void rejectWithoutClockUsesSystemTime() {
        FlashSaleRegistration reg = pending();

        reg.reject("admin-1", "nope", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(reg.getDecidedAt()).isNotNull();
    }

    @Test
    void cancelOnlyAllowedForOwningMerchant() {
        FlashSaleRegistration reg = pending();

        assertThatThrownBy(() -> reg.cancel("other-merchant", CLOCK))
                .isInstanceOf(PromotionException.class);

        reg.cancel("mer-1", CLOCK);
        assertThat(reg.getStatus()).isEqualTo(FlashSaleRegistrationStatus.CANCELLED);
    }

    @Test
    void cancelWithoutClockUsesSystemTime() {
        FlashSaleRegistration reg = pending();

        reg.cancel("mer-1", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(reg.getUpdatedAt()).isNotNull();
    }

    @Test
    void consumeStockRequiresApprovedState() {
        FlashSaleRegistration reg = pending();

        assertThatThrownBy(() -> reg.consumeStock(1, CLOCK))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void consumeStockIncreasesSoldCount() {
        FlashSaleRegistration reg = pending();
        reg.approve("admin-1", REGULAR_PRICE, CLOCK);

        reg.consumeStock(4, CLOCK);

        assertThat(reg.getSoldCount()).isEqualTo(4);
        assertThat(reg.hasStockLeft()).isTrue();
    }

    @Test
    void consumeStockRejectsOverflow() {
        FlashSaleRegistration reg = pending();
        reg.approve("admin-1", REGULAR_PRICE, CLOCK);

        assertThatThrownBy(() -> reg.consumeStock(11, CLOCK))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void consumeStockWithoutClockUsesSystemTime() {
        FlashSaleRegistration reg = pending();
        reg.approve("admin-1", REGULAR_PRICE, CLOCK);

        reg.consumeStock(10, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(reg.hasStockLeft()).isFalse();
    }
}
