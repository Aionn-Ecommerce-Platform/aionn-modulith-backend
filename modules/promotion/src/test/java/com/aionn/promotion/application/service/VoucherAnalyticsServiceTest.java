package com.aionn.promotion.application.service;

import com.aionn.promotion.application.dto.analytics.result.MerchantVoucherAnalyticsResult;
import com.aionn.promotion.infrastructure.persistence.repository.VoucherRepository;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherAnalyticsServiceTest {

    @Mock
    private VoucherRepository voucherRepository;
    @Mock
    private MerchantQueryPort merchantQueryPort;

    @InjectMocks
    private VoucherAnalyticsService service;

    private static VoucherRepository.MerchantVoucherProjection row(String code, String campaignId,
            Integer limit, Integer used, BigDecimal discount) {
        return new VoucherRepository.MerchantVoucherProjection() {
            @Override
            public String getVoucherCode() {
                return code;
            }

            @Override
            public String getCampaignId() {
                return campaignId;
            }

            @Override
            public Integer getUsageLimit() {
                return limit;
            }

            @Override
            public Integer getUsedCount() {
                return used;
            }

            @Override
            public BigDecimal getDiscountAmount() {
                return discount;
            }
        };
    }

    @Test
    void aggregatesIssuedRedeemedAndDiscount() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));
        when(voucherRepository.findMerchantVoucherRows("mer-1")).thenReturn(List.of(
                row("V-1", "camp-1", 100, 20, new BigDecimal("10000")),
                row("V-2", "camp-1", 50, 5, new BigDecimal("20000"))));

        MerchantVoucherAnalyticsResult result = service.getMerchantAnalytics("owner-1");

        assertThat(result.totalIssued()).isEqualTo(150);
        assertThat(result.totalRedeemed()).isEqualTo(25);
        assertThat(result.totalRemaining()).isEqualTo(125);
        assertThat(result.redemptionRate()).isCloseTo(25d / 150d, org.assertj.core.data.Offset.offset(0.0001));
        assertThat(result.totalDiscountValue()).isEqualByComparingTo("300000");
        assertThat(result.topVouchers()).extracting(MerchantVoucherAnalyticsResult.TopVoucher::voucherCode)
                .containsExactly("V-1", "V-2");
    }

    @Test
    void toleratesNullColumnsAndEmptyRows() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));
        when(voucherRepository.findMerchantVoucherRows("mer-1")).thenReturn(List.of(
                row("V-3", null, null, null, null)));

        MerchantVoucherAnalyticsResult result = service.getMerchantAnalytics("owner-1");

        assertThat(result.totalIssued()).isZero();
        assertThat(result.totalRedeemed()).isZero();
        assertThat(result.totalRemaining()).isZero();
        assertThat(result.redemptionRate()).isZero();
        assertThat(result.totalDiscountValue()).isEqualByComparingTo("0");
        assertThat(result.topVouchers()).hasSize(1);
    }

    @Test
    void throwsWhenOwnerHasNoMerchant() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMerchantAnalytics("owner-1"))
                .isInstanceOf(IllegalStateException.class);
    }
}
