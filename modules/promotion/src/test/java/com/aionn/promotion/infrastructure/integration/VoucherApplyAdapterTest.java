package com.aionn.promotion.infrastructure.integration;

import com.aionn.promotion.application.port.out.PromotionCampaignPersistencePort;
import com.aionn.promotion.application.port.out.UserVoucherPersistencePort;
import com.aionn.promotion.application.port.out.VoucherPersistencePort;
import com.aionn.promotion.domain.model.UserVoucher;
import com.aionn.promotion.domain.model.Voucher;
import com.aionn.promotion.domain.valueobject.UserVoucherStatus;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VoucherApplyAdapterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), ZoneOffset.UTC);

    private final VoucherPersistencePort voucherRepository = mock(VoucherPersistencePort.class);
    private final UserVoucherPersistencePort userVoucherRepository = mock(UserVoucherPersistencePort.class);
    private final PromotionCampaignPersistencePort campaignRepository = mock(PromotionCampaignPersistencePort.class);
    private final VoucherApplyAdapter adapter = new VoucherApplyAdapter(
            voucherRepository, userVoucherRepository, campaignRepository, CLOCK);

    @Test
    void applyCommitsOneSlotAndReleaseRestoresCapacity() {
        Voucher voucher = Voucher.issueForShop("SAVE10", "merchant-1", Money.of(BigDecimal.TEN, "VND"),
                1, CLOCK.instant().minusSeconds(60), CLOCK.instant().plusSeconds(3600), CLOCK);
        when(voucherRepository.lockByCode("SAVE10")).thenReturn(Optional.of(voucher));
        when(voucherRepository.findByCode("SAVE10")).thenReturn(Optional.of(voucher));
        when(userVoucherRepository.findByUserAndCode("user-1", "SAVE10")).thenReturn(Optional.empty());
        when(userVoucherRepository.save(any(UserVoucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var discount = adapter.apply("user-1", "merchant-1", "SAVE10", "order-1",
                BigDecimal.valueOf(100), "VND");

        assertThat(discount.applied()).isTrue();
        assertThat(voucher.getReservedCount()).isZero();
        assertThat(voucher.getUsedCount()).isEqualTo(1);
        var captor = org.mockito.ArgumentCaptor.forClass(UserVoucher.class);
        verify(userVoucherRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        UserVoucher applied = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(applied.getStatus()).isEqualTo(UserVoucherStatus.APPLIED);

        when(userVoucherRepository.findByReservedOrderId("order-1")).thenReturn(Optional.of(applied));
        adapter.release("user-1", "order-1", "placement-failed");

        assertThat(voucher.getUsedCount()).isZero();
        assertThat(voucher.remainingUses()).isEqualTo(1);
        assertThat(applied.getStatus()).isEqualTo(UserVoucherStatus.RELEASED);
    }
}
