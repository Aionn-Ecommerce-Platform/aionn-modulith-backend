package com.aionn.promotion.infrastructure.integration;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.aionn.promotion.application.port.out.PromotionCampaignPersistencePort;
import com.aionn.promotion.application.port.out.UserVoucherPersistencePort;
import com.aionn.promotion.application.port.out.VoucherPersistencePort;
import com.aionn.promotion.application.policy.VoucherReservationPolicy;
import com.aionn.promotion.domain.model.UserVoucher;
import com.aionn.promotion.domain.model.Voucher;
import com.aionn.promotion.domain.model.PromotionCampaign;
import com.aionn.promotion.domain.valueobject.CampaignType;
import com.aionn.promotion.domain.valueobject.PromotionCondition;
import com.aionn.promotion.domain.valueobject.UserVoucherStatus;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class VoucherApplyAdapterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), ZoneOffset.UTC);

    private final VoucherPersistencePort voucherPersistencePort = mock(VoucherPersistencePort.class);
    private final UserVoucherPersistencePort userVoucherPersistencePort = mock(UserVoucherPersistencePort.class);
    private final PromotionCampaignPersistencePort promotionCampaignPersistencePort = mock(PromotionCampaignPersistencePort.class);
    private final VoucherReservationPolicy reservationPolicy = mock(VoucherReservationPolicy.class);
    private final VoucherApplyAdapter adapter = new VoucherApplyAdapter(
            voucherPersistencePort, userVoucherPersistencePort, promotionCampaignPersistencePort, reservationPolicy, CLOCK);

    {
        when(reservationPolicy.expiresFrom(any()))
                .thenReturn(CLOCK.instant().plusSeconds(900));
    }

    @Test
    void applyCommitsOneSlotAndReleaseRestoresCapacity() {
        Voucher voucher = Voucher.issueForShop("SAVE10", "merchant-1", Money.of(BigDecimal.TEN, "VND"),
                1, CLOCK.instant().minusSeconds(60), CLOCK.instant().plusSeconds(3600), CLOCK);
        when(voucherPersistencePort.lockByCode("SAVE10")).thenReturn(Optional.of(voucher));
        when(voucherPersistencePort.findByCode("SAVE10")).thenReturn(Optional.of(voucher));
        when(userVoucherPersistencePort.findByUserAndCodeForUpdate("user-1", "SAVE10")).thenReturn(Optional.empty());
        when(userVoucherPersistencePort.save(any(UserVoucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var discount = adapter.apply("user-1", "merchant-1", "SAVE10", "order-1",
                BigDecimal.valueOf(100), "VND", List.of());

        assertThat(discount.applied()).isTrue();
        assertThat(voucher.getReservedCount()).isZero();
        assertThat(voucher.getUsedCount()).isEqualTo(1);
        var captor = ArgumentCaptor.forClass(UserVoucher.class);
        verify(userVoucherPersistencePort, Mockito.atLeastOnce()).save(captor.capture());
        UserVoucher applied = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(applied.getStatus()).isEqualTo(UserVoucherStatus.APPLIED);

        when(userVoucherPersistencePort.findByReservedOrderIdForUpdate("order-1")).thenReturn(Optional.of(applied));
        adapter.release("user-1", "order-1", "placement-failed");

        assertThat(voucher.getUsedCount()).isZero();
        assertThat(voucher.remainingUses()).isEqualTo(1);
        assertThat(applied.getStatus()).isEqualTo(UserVoucherStatus.RELEASED);
    }

    @Test
    void categoryRestrictedCampaignAcceptsMatchingOrderCategories() {
        Voucher voucher = Voucher.issue("CATEGORY10", "campaign-1", Money.of(BigDecimal.TEN, "VND"),
                2, CLOCK.instant().minusSeconds(60), CLOCK.instant().plusSeconds(3600), CLOCK);
        PromotionCampaign campaign = runningCampaign(List.of("category-1"));
        when(voucherPersistencePort.lockByCode("CATEGORY10")).thenReturn(Optional.of(voucher));
        when(promotionCampaignPersistencePort.findByIdForUpdate("campaign-1")).thenReturn(Optional.of(campaign));
        when(userVoucherPersistencePort.findByUserAndCodeForUpdate("user-1", "CATEGORY10")).thenReturn(Optional.empty());
        when(userVoucherPersistencePort.save(any(UserVoucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var discount = adapter.apply("user-1", "merchant-1", "CATEGORY10", "order-1",
                BigDecimal.valueOf(100), "VND", List.of("category-1", "category-2"));

        assertThat(discount.applied()).isTrue();
    }

    @Test
    void categoryRestrictedCampaignRejectsNonMatchingOrderCategories() {
        Voucher voucher = Voucher.issue("CATEGORY10", "campaign-1", Money.of(BigDecimal.TEN, "VND"),
                2, CLOCK.instant().minusSeconds(60), CLOCK.instant().plusSeconds(3600), CLOCK);
        when(voucherPersistencePort.lockByCode("CATEGORY10")).thenReturn(Optional.of(voucher));
        when(promotionCampaignPersistencePort.findByIdForUpdate("campaign-1")).thenReturn(Optional.of(runningCampaign(List.of("category-1"))));

        var discount = adapter.apply("user-1", "merchant-1", "CATEGORY10", "order-1",
                BigDecimal.valueOf(100), "VND", List.of("other-category"));

        assertThat(discount.applied()).isFalse();
        assertThat(discount.reason()).isEqualTo("campaign-condition-not-met");
    }

    @Test
    void replayReturnsStoredDiscountAfterCampaignHasEnded() {
        Voucher voucher = Voucher.issue("CATEGORY10", "campaign-1", Money.of(BigDecimal.TEN, "VND"),
                2, CLOCK.instant().minusSeconds(3600), CLOCK.instant().minusSeconds(60), CLOCK);
        UserVoucher applied = UserVoucher.claim("uv-1", "CATEGORY10", "user-1", CLOCK);
        applied.reserve("order-1", CLOCK.instant().plusSeconds(900), CLOCK);
        applied.apply(Money.of(BigDecimal.TEN, "VND"), CLOCK);
        when(voucherPersistencePort.lockByCode("CATEGORY10")).thenReturn(Optional.of(voucher));
        when(userVoucherPersistencePort.findByUserAndCodeForUpdate("user-1", "CATEGORY10")).thenReturn(Optional.of(applied));

        var discount = adapter.apply("user-1", "merchant-1", "CATEGORY10", "order-1",
                BigDecimal.valueOf(100), "VND", List.of("category-1"));

        assertThat(discount.applied()).isTrue();
        assertThat(discount.amount()).isEqualByComparingTo(BigDecimal.TEN);
        verify(promotionCampaignPersistencePort, never()).findByIdForUpdate(any());
    }

    private static PromotionCampaign runningCampaign(List<String> categoryIds) {
        PromotionCampaign campaign = PromotionCampaign.create(
                "campaign-1", "Category campaign", CampaignType.DISCOUNT,
                Money.of(BigDecimal.valueOf(1_000), "VND"),
                CLOCK.instant().minusSeconds(60), CLOCK.instant().plusSeconds(3600), "admin-1", CLOCK);
        campaign.configureCondition(new PromotionCondition(BigDecimal.ZERO, categoryIds, null, null), CLOCK);
        campaign.activate(CLOCK);
        campaign.pullEvents();
        return campaign;
    }
}

