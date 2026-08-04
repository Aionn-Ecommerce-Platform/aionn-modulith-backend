package com.aionn.promotion.application.service;

import com.aionn.promotion.application.dto.flashsale.command.FlashSaleCommands;
import com.aionn.promotion.application.dto.flashsale.result.ActiveFlashSaleResult;
import com.aionn.promotion.application.port.out.FlashSaleRegistrationPersistencePort;
import com.aionn.promotion.application.port.out.PromotionCampaignPersistencePort;
import com.aionn.promotion.domain.exception.PromotionErrorCode;
import com.aionn.promotion.domain.exception.PromotionException;
import com.aionn.promotion.domain.model.FlashSaleRegistration;
import com.aionn.promotion.domain.model.PromotionCampaign;
import com.aionn.promotion.domain.valueobject.CampaignType;
import com.aionn.promotion.domain.valueobject.FlashSaleRegistrationStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import com.aionn.sharedkernel.integration.port.catalog.PricingQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlashSaleServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private FlashSaleRegistrationPersistencePort registrationRepository;
    @Mock
    private PromotionCampaignPersistencePort campaignRepository;
    @Mock
    private MerchantQueryPort merchantQueryPort;
    @Mock
    private PricingQueryPort pricingQueryPort;
    @Mock
    private EventPublisher eventPublisher;

    private FlashSaleService service() {
        return new FlashSaleService(registrationRepository, campaignRepository,
                merchantQueryPort, pricingQueryPort, CLOCK, eventPublisher);
    }

    private static PromotionCampaign campaign(CampaignType type) {
        return PromotionCampaign.create("camp-1", "Flash", type,
                Money.of(new BigDecimal("1000000"), "VND"),
                NOW.minusSeconds(60), NOW.plusSeconds(86400), "admin-1", CLOCK);
    }

    private static PromotionCampaign runningFlashCampaign() {
        PromotionCampaign c = campaign(CampaignType.FLASH_SALE);
        c.activate(CLOCK);
        c.pullEvents();
        return c;
    }

    private static FlashSaleRegistration pending() {
        return FlashSaleRegistration.submit("reg-1", "camp-1", "mer-1", "prod-1", "sku-1",
                Money.of(new BigDecimal("80000"), "VND"), 10, CLOCK);
    }

    private static FlashSaleRegistration approved() {
        FlashSaleRegistration reg = pending();
        reg.approve("admin-1", Money.of(new BigDecimal("100000"), "VND"), CLOCK);
        reg.pullEvents();
        return reg;
    }

    private static FlashSaleCommands.RegisterFlashSale registerCommand() {
        return new FlashSaleCommands.RegisterFlashSale("camp-1", "owner-1",
                "prod-1", "sku-1", new BigDecimal("80000"), "VND", 10);
    }

    private void echoSave() {
        when(registrationRepository.save(any(FlashSaleRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registerCreatesPendingRegistration() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));
        when(campaignRepository.findById("camp-1")).thenReturn(Optional.of(campaign(CampaignType.FLASH_SALE)));
        when(registrationRepository.findActiveBySkuAndCampaign("camp-1", "sku-1"))
                .thenReturn(Optional.empty());
        when(pricingQueryPort.resolvePricing(List.of("sku-1"))).thenReturn(Map.of("sku-1",
                new PricingQueryPort.SkuPricing("sku-1", "mer-1", new BigDecimal("100000"), "VND", true)));
        echoSave();

        FlashSaleRegistration reg = service().register(registerCommand());

        assertThat(reg.getStatus()).isEqualTo(FlashSaleRegistrationStatus.PENDING);
        assertThat(reg.getMerchantId()).isEqualTo("mer-1");
        assertThat(reg.getSubmittedAt()).isEqualTo(NOW);
        verify(eventPublisher).publish(anyCollection());
    }

    @Test
    void registerFallsBackToCatalogCurrency() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));
        when(campaignRepository.findById("camp-1")).thenReturn(Optional.of(campaign(CampaignType.FLASH_SALE)));
        when(registrationRepository.findActiveBySkuAndCampaign("camp-1", "sku-1"))
                .thenReturn(Optional.empty());
        when(pricingQueryPort.resolvePricing(List.of("sku-1"))).thenReturn(Map.of("sku-1",
                new PricingQueryPort.SkuPricing("sku-1", "mer-1", new BigDecimal("100000"), "USD", true)));
        echoSave();

        FlashSaleRegistration reg = service().register(new FlashSaleCommands.RegisterFlashSale(
                "camp-1", "owner-1", "prod-1", "sku-1", new BigDecimal("80000"), null, 10));

        assertThat(reg.getSalePrice().currency()).isEqualTo("USD");
    }

    @Test
    void registerRejectsUnknownMerchant() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.empty());

        FlashSaleService service = service();
        FlashSaleCommands.RegisterFlashSale command = registerCommand();

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void registerRejectsMissingCampaign() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));
        when(campaignRepository.findById("camp-1")).thenReturn(Optional.empty());

        FlashSaleService service = service();
        FlashSaleCommands.RegisterFlashSale command = registerCommand();

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(PromotionException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        PromotionErrorCode.CAMPAIGN_NOT_FOUND.getCode());
    }

    @Test
    void registerRejectsNonFlashSaleCampaign() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));
        when(campaignRepository.findById("camp-1")).thenReturn(Optional.of(campaign(CampaignType.DISCOUNT)));

        FlashSaleService service = service();
        FlashSaleCommands.RegisterFlashSale command = registerCommand();

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(PromotionException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        PromotionErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void registerRejectsDuplicateSku() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));
        when(campaignRepository.findById("camp-1")).thenReturn(Optional.of(campaign(CampaignType.FLASH_SALE)));
        when(registrationRepository.findActiveBySkuAndCampaign("camp-1", "sku-1"))
                .thenReturn(Optional.of(pending()));

        FlashSaleService service = service();
        FlashSaleCommands.RegisterFlashSale command = registerCommand();

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(PromotionException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        PromotionErrorCode.FLASH_SALE_DUPLICATE.getCode());
    }

    @Test
    void registerRejectsSkuOwnedByAnotherMerchant() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));
        when(campaignRepository.findById("camp-1")).thenReturn(Optional.of(campaign(CampaignType.FLASH_SALE)));
        when(registrationRepository.findActiveBySkuAndCampaign("camp-1", "sku-1"))
                .thenReturn(Optional.empty());
        when(pricingQueryPort.resolvePricing(List.of("sku-1"))).thenReturn(Map.of("sku-1",
                new PricingQueryPort.SkuPricing("sku-1", "other-mer", new BigDecimal("100000"), "VND", true)));

        FlashSaleService service = service();
        FlashSaleCommands.RegisterFlashSale command = registerCommand();

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(PromotionException.class);
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void registerRejectsUnknownSku() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));
        when(campaignRepository.findById("camp-1")).thenReturn(Optional.of(campaign(CampaignType.FLASH_SALE)));
        when(registrationRepository.findActiveBySkuAndCampaign("camp-1", "sku-1"))
                .thenReturn(Optional.empty());
        when(pricingQueryPort.resolvePricing(List.of("sku-1"))).thenReturn(Map.of());

        FlashSaleService service = service();
        FlashSaleCommands.RegisterFlashSale command = registerCommand();

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void approveEnforcesMinimumDiscountAgainstCatalogPrice() {
        when(registrationRepository.findById("reg-1")).thenReturn(Optional.of(pending()));
        when(pricingQueryPort.resolvePricing(List.of("sku-1"))).thenReturn(Map.of("sku-1",
                new PricingQueryPort.SkuPricing("sku-1", "mer-1", new BigDecimal("100000"), "VND", true)));
        echoSave();

        FlashSaleRegistration reg = service().approve(
                new FlashSaleCommands.ApproveFlashSale("reg-1", "admin-1"));

        assertThat(reg.getStatus()).isEqualTo(FlashSaleRegistrationStatus.APPROVED);
        assertThat(reg.getDecidedBy()).isEqualTo("admin-1");
    }

    @Test
    void approveSkipsDiscountCheckWhenPricingUnavailable() {
        when(registrationRepository.findById("reg-1")).thenReturn(Optional.of(pending()));
        when(pricingQueryPort.resolvePricing(List.of("sku-1"))).thenReturn(Map.of());
        echoSave();

        FlashSaleRegistration reg = service().approve(
                new FlashSaleCommands.ApproveFlashSale("reg-1", "admin-1"));

        assertThat(reg.getStatus()).isEqualTo(FlashSaleRegistrationStatus.APPROVED);
    }

    @Test
    void approveThrowsWhenRegistrationMissing() {
        when(registrationRepository.findById("nope")).thenReturn(Optional.empty());

        FlashSaleService service = service();
        FlashSaleCommands.ApproveFlashSale command = new FlashSaleCommands.ApproveFlashSale("nope", "admin-1");

        assertThatThrownBy(() -> service.approve(command))
                .isInstanceOf(PromotionException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        PromotionErrorCode.FLASH_SALE_NOT_FOUND.getCode());
    }

    @Test
    void rejectStoresReason() {
        when(registrationRepository.findById("reg-1")).thenReturn(Optional.of(pending()));
        echoSave();

        FlashSaleRegistration reg = service().reject(
                new FlashSaleCommands.RejectFlashSale("reg-1", "admin-1", "too expensive"));

        assertThat(reg.getStatus()).isEqualTo(FlashSaleRegistrationStatus.REJECTED);
        assertThat(reg.getRejectReason()).isEqualTo("too expensive");
    }

    @Test
    void cancelResolvesMerchantBeforeCancelling() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));
        when(registrationRepository.findById("reg-1")).thenReturn(Optional.of(pending()));
        echoSave();

        FlashSaleRegistration reg = service().cancel(
                new FlashSaleCommands.CancelFlashSale("reg-1", "owner-1"));

        assertThat(reg.getStatus()).isEqualTo(FlashSaleRegistrationStatus.CANCELLED);
    }

    @Test
    void cancelRejectsUnknownMerchant() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.empty());

        FlashSaleService service = service();
        FlashSaleCommands.CancelFlashSale command = new FlashSaleCommands.CancelFlashSale("reg-1", "owner-1");

        assertThatThrownBy(() -> service.cancel(command))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void listByMerchantCapsLimit() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));
        when(registrationRepository.findByMerchant("mer-1", FlashSaleRegistrationStatus.PENDING, 200))
                .thenReturn(List.of(pending()));

        assertThat(service().listByMerchant("owner-1", FlashSaleRegistrationStatus.PENDING, 9999))
                .hasSize(1);
    }

    @Test
    void listByMerchantRejectsUnknownMerchant() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.empty());

        FlashSaleService service = service();

        assertThatThrownBy(() -> service.listByMerchant("owner-1", null, 10))
                .isInstanceOf(PromotionException.class);
    }

    @Test
    void listByStatusRaisesLimitFloor() {
        when(registrationRepository.findByStatus(FlashSaleRegistrationStatus.PENDING, 1))
                .thenReturn(List.of(pending()));

        assertThat(service().listByStatus(FlashSaleRegistrationStatus.PENDING, -5)).hasSize(1);
    }

    @Test
    void getReturnsRegistration() {
        when(registrationRepository.findById("reg-1")).thenReturn(Optional.of(pending()));

        assertThat(service().get("reg-1").getRegistrationId()).isEqualTo("reg-1");
    }

    @Test
    void getOwnedReturnsRegistrationForOwningMerchant() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(Optional.of("mer-1"));
        when(registrationRepository.findById("reg-1")).thenReturn(Optional.of(pending()));

        assertThat(service().getOwned("reg-1", "owner-1").getRegistrationId()).isEqualTo("reg-1");
    }

    @Test
    void getOwnedRejectsForeignMerchant() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-2")).thenReturn(Optional.of("mer-2"));
        when(registrationRepository.findById("reg-1")).thenReturn(Optional.of(pending()));

        assertThatThrownBy(() -> service().getOwned("reg-1", "owner-2"))
                .isInstanceOf(PromotionException.class)
                .extracting(ex -> ((PromotionException) ex).getErrorCode())
                .isEqualTo(PromotionErrorCode.FLASH_SALE_FORBIDDEN.getCode());
    }

    @Test
    void listActiveReturnsEmptyWhenNoApprovedRegistrations() {
        when(registrationRepository.findAllApprovedRunning(anyInt())).thenReturn(List.of());

        assertThat(service().listActive(5)).isEmpty();
    }

    @Test
    void listActiveGroupsByRunningCampaign() {
        when(registrationRepository.findAllApprovedRunning(anyInt())).thenReturn(List.of(approved()));
        when(campaignRepository.findById("camp-1")).thenReturn(Optional.of(runningFlashCampaign()));

        List<ActiveFlashSaleResult> active = service().listActive(5);

        assertThat(active).hasSize(1);
        assertThat(active.get(0).campaignId()).isEqualTo("camp-1");
        assertThat(active.get(0).items()).hasSize(1);
        assertThat(active.get(0).items().get(0).salePrice()).isEqualByComparingTo("80000");
    }

    @Test
    void listActiveSkipsCampaignsThatAreNotRunning() {
        when(registrationRepository.findAllApprovedRunning(anyInt())).thenReturn(List.of(approved()));
        when(campaignRepository.findById("camp-1")).thenReturn(Optional.of(campaign(CampaignType.FLASH_SALE)));

        assertThat(service().listActive(5)).isEmpty();
    }

    @Test
    void listActiveSkipsMissingCampaigns() {
        when(registrationRepository.findAllApprovedRunning(anyInt())).thenReturn(List.of(approved()));
        when(campaignRepository.findById("camp-1")).thenReturn(Optional.empty());

        assertThat(service().listActive(5)).isEmpty();
    }
}
