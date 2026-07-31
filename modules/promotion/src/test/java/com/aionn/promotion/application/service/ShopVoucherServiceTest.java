package com.aionn.promotion.application.service;

import com.aionn.promotion.application.dto.voucher.command.VoucherCommands;
import com.aionn.promotion.application.port.out.VoucherPersistencePort;
import com.aionn.promotion.domain.exception.PromotionErrorCode;
import com.aionn.promotion.domain.exception.PromotionException;
import com.aionn.promotion.domain.model.Voucher;
import com.aionn.promotion.domain.valueobject.VoucherScope;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopVoucherServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private VoucherPersistencePort voucherRepository;
    @Mock
    private MerchantQueryPort merchantQueryPort;
    @Mock
    private EventPublisher eventPublisher;

    private ShopVoucherService service() {
        return new ShopVoucherService(voucherRepository, merchantQueryPort, eventPublisher, CLOCK);
    }

    private static VoucherCommands.IssueShopVoucher issueCommand(String code) {
        return new VoucherCommands.IssueShopVoucher("owner-1", code,
                new BigDecimal("30000"), "VND", 20,
                NOW.minusSeconds(60), NOW.plusSeconds(86400));
    }

    private static Voucher shopVoucher(String code, Instant validUntil) {
        return new Voucher(code, null, VoucherScope.SHOP, "mer-1",
                Money.of(new BigDecimal("30000"), "VND"), 20, 0, 0,
                NOW.minusSeconds(60), validUntil, NOW, NOW);
    }

    @Test
    void issueNormalisesCodeAndReturnsVoucher() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));
        when(voucherRepository.findByCode("SHOP10")).thenReturn(Optional.empty());
        when(voucherRepository.save(any(Voucher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Voucher issued = service().issue(issueCommand("  shop10 "));

        assertThat(issued.getVoucherCode()).isEqualTo("SHOP10");
        assertThat(issued.getScope()).isEqualTo(VoucherScope.SHOP);
        assertThat(issued.getMerchantId()).isEqualTo("mer-1");
        assertThat(issued.getCreatedAt()).isEqualTo(NOW);
        verify(eventPublisher).publish(anyCollection());
    }

    @Test
    void issueDefaultsCurrencyToVnd() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));
        when(voucherRepository.findByCode("SHOP11")).thenReturn(Optional.empty());
        when(voucherRepository.save(any(Voucher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Voucher issued = service().issue(new VoucherCommands.IssueShopVoucher("owner-1", "SHOP11",
                new BigDecimal("1000"), null, 5, null, null));

        assertThat(issued.getDiscountAmount().currency()).isEqualTo("VND");
    }

    @Test
    void issueRejectsDuplicateCode() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));
        when(voucherRepository.findByCode("SHOP10"))
                .thenReturn(Optional.of(shopVoucher("SHOP10", NOW.plusSeconds(86400))));

        ShopVoucherService service = service();
        VoucherCommands.IssueShopVoucher command = issueCommand("SHOP10");

        assertThatThrownBy(() -> service.issue(command))
                .isInstanceOf(PromotionException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        PromotionErrorCode.VOUCHER_DUPLICATE_CODE.getCode());
        verify(voucherRepository, never()).save(any());
    }

    @Test
    void issueRejectsUnknownMerchant() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.empty());

        ShopVoucherService service = service();
        VoucherCommands.IssueShopVoucher command = issueCommand("SHOP10");

        assertThatThrownBy(() -> service.issue(command))
                .isInstanceOf(PromotionException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        PromotionErrorCode.MERCHANT_NOT_FOUND.getCode());
    }

    @Test
    void listMineResolvesMerchantAndCapsLimit() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));
        when(voucherRepository.findByMerchantId("mer-1", 100))
                .thenReturn(List.of(shopVoucher("SHOP10", NOW.plusSeconds(86400))));

        assertThat(service().listMine("owner-1", 999)).hasSize(1);
        verify(voucherRepository).findByMerchantId("mer-1", 100);
    }

    @Test
    void listMineRaisesLimitFloorToOne() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));
        when(voucherRepository.findByMerchantId(eq("mer-1"), anyInt())).thenReturn(List.of());

        service().listMine("owner-1", 0);

        verify(voucherRepository).findByMerchantId("mer-1", 1);
    }

    @Test
    void listByMerchantFiltersExpiredVouchers() {
        when(voucherRepository.findByMerchantId("mer-1", 20)).thenReturn(List.of(
                shopVoucher("VALID", NOW.plusSeconds(86400)),
                shopVoucher("EXPIRED", NOW.minusSeconds(1))));

        List<Voucher> vouchers = service().listByMerchant("mer-1", 20);

        assertThat(vouchers).extracting(Voucher::getVoucherCode).containsExactly("VALID");
    }
}
