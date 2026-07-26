package com.aionn.promotion.application.service;

import com.aionn.promotion.application.dto.campaign.command.CampaignCommands;
import com.aionn.promotion.application.port.out.PromotionCampaignPersistencePort;
import com.aionn.promotion.application.port.out.VoucherPersistencePort;
import com.aionn.promotion.domain.exception.PromotionErrorCode;
import com.aionn.promotion.domain.exception.PromotionException;
import com.aionn.promotion.domain.model.PromotionCampaign;
import com.aionn.promotion.domain.model.Voucher;
import com.aionn.promotion.domain.valueobject.CampaignStatus;
import com.aionn.promotion.domain.valueobject.CampaignType;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.Money;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionCampaignServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private PromotionCampaignPersistencePort campaignRepository;
    @Mock
    private VoucherPersistencePort voucherRepository;
    @Mock
    private EventPublisher eventPublisher;

    private PromotionCampaignService service() {
        return new PromotionCampaignService(campaignRepository, voucherRepository, eventPublisher, CLOCK);
    }

    private static PromotionCampaign draft() {
        return PromotionCampaign.create("camp-1", "Summer", CampaignType.DISCOUNT,
                Money.of(new BigDecimal("1000000"), "VND"),
                NOW.minusSeconds(60), NOW.plusSeconds(86400), "admin-1", CLOCK);
    }

    private static PromotionCampaign running() {
        PromotionCampaign campaign = draft();
        campaign.activate(CLOCK);
        campaign.pullEvents();
        return campaign;
    }

    private void saveEchoes() {
        when(campaignRepository.save(any(PromotionCampaign.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createBuildsCampaignWithClockAndPublishes() {
        saveEchoes();

        PromotionCampaign created = service().create(new CampaignCommands.CreateCampaign(
                "Summer", CampaignType.DISCOUNT, new BigDecimal("1000000"), "VND",
                NOW.minusSeconds(60), NOW.plusSeconds(86400), "admin-1"));

        assertThat(created.getName()).isEqualTo("Summer");
        assertThat(created.getStatus()).isEqualTo(CampaignStatus.SCHEDULED);
        assertThat(created.getCreatedAt()).isEqualTo(NOW);
        verify(eventPublisher).publish(any(java.util.Collection.class));
    }

    @Test
    void createDefaultsCurrencyToVnd() {
        saveEchoes();

        PromotionCampaign created = service().create(new CampaignCommands.CreateCampaign(
                "Summer", CampaignType.DISCOUNT, new BigDecimal("1000"), null,
                NOW.minusSeconds(60), NOW.plusSeconds(86400), "admin-1"));

        assertThat(created.getBudget().currency()).isEqualTo("VND");
    }

    @Test
    void activateMovesCampaignToRunning() {
        when(campaignRepository.findById("camp-1")).thenReturn(Optional.of(draft()));
        saveEchoes();

        PromotionCampaign activated = service().activate(new CampaignCommands.ActivateCampaign("camp-1"));

        assertThat(activated.getStatus()).isEqualTo(CampaignStatus.RUNNING);
    }

    @Test
    void activateThrowsWhenCampaignMissing() {
        when(campaignRepository.findById("nope")).thenReturn(Optional.empty());

        PromotionCampaignService service = service();
        CampaignCommands.ActivateCampaign command = new CampaignCommands.ActivateCampaign("nope");

        assertThatThrownBy(() -> service.activate(command))
                .isInstanceOf(PromotionException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        PromotionErrorCode.CAMPAIGN_NOT_FOUND.getCode());
    }

    @Test
    void endMovesCampaignToEnded() {
        when(campaignRepository.findById("camp-1")).thenReturn(Optional.of(running()));
        saveEchoes();

        PromotionCampaign ended = service().end(new CampaignCommands.EndCampaign("camp-1"));

        assertThat(ended.getStatus()).isEqualTo(CampaignStatus.ENDED);
    }

    @Test
    void cancelStoresReason() {
        when(campaignRepository.findById("camp-1")).thenReturn(Optional.of(draft()));
        saveEchoes();

        PromotionCampaign cancelled = service().cancel(
                new CampaignCommands.CancelCampaign("camp-1", "wrong dates"));

        assertThat(cancelled.getStatus()).isEqualTo(CampaignStatus.CANCELLED);
    }

    @Test
    void configureConditionStoresCondition() {
        when(campaignRepository.findById("camp-1")).thenReturn(Optional.of(draft()));
        saveEchoes();

        PromotionCampaign configured = service().configureCondition(
                new CampaignCommands.ConfigureCondition("camp-1", new BigDecimal("100000"),
                        List.of("cat-1"), 1, 2));

        assertThat(configured.getCondition().minOrderValue()).isEqualByComparingTo("100000");
        assertThat(configured.getCondition().applicableCategoryIds()).containsExactly("cat-1");
    }

    @Test
    void issueVoucherUsesCampaignCurrencyWhenAbsent() {
        when(campaignRepository.findById("camp-1")).thenReturn(Optional.of(draft()));
        when(voucherRepository.findByCode("V-1")).thenReturn(Optional.empty());
        when(voucherRepository.save(any(Voucher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Voucher voucher = service().issueVoucher(new CampaignCommands.IssueVoucher(
                "camp-1", "V-1", new BigDecimal("50000"), null, 100, null, null));

        assertThat(voucher.getDiscountAmount().currency()).isEqualTo("VND");
        assertThat(voucher.getCampaignId()).isEqualTo("camp-1");
    }

    @Test
    void issueVoucherRejectsDuplicateCode() {
        when(campaignRepository.findById("camp-1")).thenReturn(Optional.of(draft()));
        when(voucherRepository.findByCode("V-1")).thenReturn(Optional.of(
                Voucher.issue("V-1", "camp-1", Money.of(new BigDecimal("1000"), "VND"),
                        5, null, null, CLOCK)));

        PromotionCampaignService service = service();
        CampaignCommands.IssueVoucher command = new CampaignCommands.IssueVoucher(
                "camp-1", "V-1", new BigDecimal("50000"), "VND", 100, null, null);

        assertThatThrownBy(() -> service.issueVoucher(command))
                .isInstanceOf(PromotionException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        PromotionErrorCode.VOUCHER_DUPLICATE_CODE.getCode());
        verify(voucherRepository, never()).save(any());
    }

    @Test
    void processScheduledTransitionsCountsBothDirections() {
        when(campaignRepository.findToActivate(eq(NOW), anyInt())).thenReturn(List.of(draft()));
        when(campaignRepository.findToEnd(eq(NOW), anyInt())).thenReturn(List.of(running()));
        saveEchoes();

        assertThat(service().processScheduledTransitions(NOW, 50)).isEqualTo(2);
    }

    @Test
    void processScheduledTransitionsSkipsInvalidTransitions() {
        when(campaignRepository.findToActivate(eq(NOW), anyInt())).thenReturn(List.of(running()));
        when(campaignRepository.findToEnd(eq(NOW), anyInt())).thenReturn(List.of(draft()));

        assertThat(service().processScheduledTransitions(NOW, 50)).isZero();
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void getReturnsCampaign() {
        when(campaignRepository.findById("camp-1")).thenReturn(Optional.of(draft()));

        assertThat(service().get("camp-1").getCampaignId()).isEqualTo("camp-1");
    }

    @Test
    void listByStatusDelegatesToRepository() {
        when(campaignRepository.listByStatus("RUNNING", 50)).thenReturn(List.of(running()));

        assertThat(service().listByStatus("RUNNING", 50)).hasSize(1);
    }

    @Test
    void listVouchersByCampaignIdDelegatesToRepository() {
        when(voucherRepository.findByCampaignId("camp-1", 50)).thenReturn(List.of(
                Voucher.issue("V-1", "camp-1", Money.of(new BigDecimal("1000"), "VND"),
                        5, null, null, CLOCK)));

        assertThat(service().listVouchersByCampaignId("camp-1", 50)).hasSize(1);
    }
}
