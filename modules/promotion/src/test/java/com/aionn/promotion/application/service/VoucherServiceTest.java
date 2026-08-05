package com.aionn.promotion.application.service;

import com.aionn.promotion.application.dto.voucher.command.VoucherCommands;
import com.aionn.promotion.application.dto.voucher.result.UserVoucherResult;
import com.aionn.promotion.application.mapper.UserVoucherResultMapper;
import com.aionn.promotion.application.port.out.PromotionCampaignPersistencePort;
import com.aionn.promotion.application.port.out.UserVoucherPersistencePort;
import com.aionn.promotion.application.port.out.VoucherPersistencePort;
import com.aionn.promotion.domain.exception.PromotionErrorCode;
import com.aionn.promotion.domain.exception.PromotionException;
import com.aionn.promotion.domain.model.PromotionCampaign;
import com.aionn.promotion.domain.model.UserVoucher;
import com.aionn.promotion.domain.model.Voucher;
import com.aionn.promotion.domain.valueobject.CampaignType;
import com.aionn.promotion.domain.valueobject.PromotionCondition;
import com.aionn.promotion.domain.valueobject.UserVoucherStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

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
@MockitoSettings(strictness = Strictness.LENIENT)
class VoucherServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private VoucherPersistencePort voucherRepository;
    @Mock
    private UserVoucherPersistencePort userVoucherRepository;
    @Mock
    private PromotionCampaignPersistencePort campaignRepository;
    @Mock
    private UserVoucherResultMapper mapper;
    @Mock
    private EventPublisher eventPublisher;

    private VoucherService service() {
        return new VoucherService(voucherRepository, userVoucherRepository, campaignRepository,
                mapper, eventPublisher, CLOCK);
    }

    private static Voucher platformVoucher(String campaignId) {
        return Voucher.issue("V-1", campaignId, Money.of(new BigDecimal("50000"), "VND"),
                100, NOW.minusSeconds(60), NOW.plusSeconds(86400), CLOCK);
    }

    private static Voucher reservedVoucher(String campaignId) {
        Voucher voucher = platformVoucher(campaignId);
        voucher.reserveSlot(CLOCK);
        return voucher;
    }

    private static PromotionCampaign runningCampaign(PromotionCondition condition) {
        PromotionCampaign campaign = PromotionCampaign.create("camp-1", "Summer",
                CampaignType.DISCOUNT, Money.of(new BigDecimal("1000000"), "VND"),
                NOW.minusSeconds(60), NOW.plusSeconds(86400), "admin-1", CLOCK);
        campaign.activate(CLOCK);
        if (condition != null) {
            campaign.configureCondition(condition, CLOCK);
        }
        campaign.pullEvents();
        return campaign;
    }

    private static UserVoucher claimed() {
        return UserVoucher.claim("uv-1", "V-1", "user-1", CLOCK);
    }

    private static UserVoucher reserved(String orderId) {
        UserVoucher uv = claimed();
        uv.reserve(orderId, NOW.plusSeconds(900), CLOCK);
        uv.pullEvents();
        return uv;
    }

    private void mapperEchoes() {
        when(mapper.toResult(any(UserVoucher.class))).thenAnswer(invocation -> {
            UserVoucher uv = invocation.getArgument(0);
            return new UserVoucherResult(uv.getUserVoucherId(), uv.getVoucherCode(), uv.getUserId(),
                    uv.getStatus().name(), uv.getReservedOrderId(),
                    uv.getAppliedAmount() == null ? null : uv.getAppliedAmount().amount(),
                    uv.getAppliedAmount() == null ? null : uv.getAppliedAmount().currency(),
                    uv.getClaimedAt(), uv.getReservedAt(), uv.getReservedExpiresAt(),
                    uv.getAppliedAt(), uv.getReleasedAt(), uv.getUpdatedAt(),
                    null, null, null, null, null, 0, 0);
        });
    }

    @Test
    void claimCreatesUserVoucherAndEnrichesResult() {
        mapperEchoes();
        when(voucherRepository.lockByCode("V-1")).thenReturn(Optional.of(platformVoucher("camp-1")));
        when(voucherRepository.findByCode("V-1")).thenReturn(Optional.of(platformVoucher("camp-1")));
        when(campaignRepository.findById("camp-1")).thenReturn(Optional.of(
                runningCampaign(new PromotionCondition(new BigDecimal("100000"), List.of(), null, null))));
        when(userVoucherRepository.findByUserAndCode("user-1", "V-1")).thenReturn(Optional.empty());
        when(userVoucherRepository.save(any(UserVoucher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserVoucherResult result = service().claim(new VoucherCommands.ClaimVoucher("user-1", "V-1"));

        assertThat(result.status()).isEqualTo(UserVoucherStatus.CLAIMED.name());
        assertThat(result.voucherDiscountAmount()).isEqualByComparingTo("50000");
        assertThat(result.voucherCurrency()).isEqualTo("VND");
        assertThat(result.voucherScope()).isEqualTo("PLATFORM");
        assertThat(result.minOrderValue()).isEqualByComparingTo("100000");
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    void claimRejectsUnknownVoucher() {
        when(voucherRepository.lockByCode("V-1")).thenReturn(Optional.empty());

        VoucherService service = service();
        VoucherCommands.ClaimVoucher command = new VoucherCommands.ClaimVoucher("user-1", "V-1");

        assertThatThrownBy(() -> service.claim(command))
                .isInstanceOf(PromotionException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        PromotionErrorCode.VOUCHER_NOT_FOUND.getCode());
    }

    @Test
    void claimRejectsSecondClaimBySameUser() {
        when(voucherRepository.lockByCode("V-1")).thenReturn(Optional.of(platformVoucher(null)));
        when(userVoucherRepository.findByUserAndCode("user-1", "V-1"))
                .thenReturn(Optional.of(claimed()));

        VoucherService service = service();
        VoucherCommands.ClaimVoucher command = new VoucherCommands.ClaimVoucher("user-1", "V-1");

        assertThatThrownBy(() -> service.claim(command))
                .isInstanceOf(PromotionException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        PromotionErrorCode.USER_VOUCHER_ALREADY_CLAIMED.getCode());
    }

    @Test
    void claimRejectsMissingCampaign() {
        when(voucherRepository.lockByCode("V-1")).thenReturn(Optional.of(platformVoucher("camp-1")));
        when(campaignRepository.findById("camp-1")).thenReturn(Optional.empty());

        VoucherService service = service();
        VoucherCommands.ClaimVoucher command = new VoucherCommands.ClaimVoucher("user-1", "V-1");

        assertThatThrownBy(() -> service.claim(command))
                .isInstanceOf(PromotionException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        PromotionErrorCode.CAMPAIGN_NOT_FOUND.getCode());
    }

    @Test
    void reserveUsesDefaultTtlWhenNotProvided() {
        mapperEchoes();
        when(voucherRepository.lockByCode("V-1")).thenReturn(Optional.of(platformVoucher(null)));
        when(voucherRepository.findByCode("V-1")).thenReturn(Optional.of(platformVoucher(null)));
        when(userVoucherRepository.findByUserAndCode("user-1", "V-1")).thenReturn(Optional.of(claimed()));
        when(userVoucherRepository.save(any(UserVoucher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserVoucherResult result = service().reserve(new VoucherCommands.ReserveVoucher(
                "user-1", "V-1", "order-1", new BigDecimal("200000"), "VND", null, null));

        assertThat(result.status()).isEqualTo(UserVoucherStatus.RESERVED.name());
        assertThat(result.reservedExpiresAt()).isEqualTo(NOW.plusSeconds(900));
    }

    @Test
    void reserveRejectsWhenCampaignConditionFails() {
        when(voucherRepository.lockByCode("V-1")).thenReturn(Optional.of(platformVoucher("camp-1")));
        when(campaignRepository.findById("camp-1")).thenReturn(Optional.of(
                runningCampaign(new PromotionCondition(new BigDecimal("500000"), List.of(), null, null))));

        VoucherService service = service();
        VoucherCommands.ReserveVoucher command = new VoucherCommands.ReserveVoucher(
                "user-1", "V-1", "order-1", new BigDecimal("1000"), "VND", null, null);

        assertThatThrownBy(() -> service.reserve(command))
                .isInstanceOf(PromotionException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        PromotionErrorCode.CONDITION_NOT_MET.getCode());
    }

    @Test
    void reserveRejectsWhenUserHasNotClaimed() {
        when(voucherRepository.lockByCode("V-1")).thenReturn(Optional.of(platformVoucher(null)));
        when(userVoucherRepository.findByUserAndCode("user-1", "V-1")).thenReturn(Optional.empty());

        VoucherService service = service();
        VoucherCommands.ReserveVoucher command = new VoucherCommands.ReserveVoucher(
                "user-1", "V-1", "order-1", new BigDecimal("1000"), "VND", null, null);

        assertThatThrownBy(() -> service.reserve(command))
                .isInstanceOf(PromotionException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        PromotionErrorCode.USER_VOUCHER_NOT_FOUND.getCode());
    }

    @Test
    void applyConsumesCampaignBudget() {
        mapperEchoes();
        PromotionCampaign campaign = runningCampaign(null);
        when(voucherRepository.lockByCode("V-1")).thenReturn(Optional.of(reservedVoucher("camp-1")));
        when(voucherRepository.findByCode("V-1")).thenReturn(Optional.of(platformVoucher("camp-1")));
        when(campaignRepository.findById("camp-1")).thenReturn(Optional.of(campaign));
        when(userVoucherRepository.findByUserAndCode("user-1", "V-1"))
                .thenReturn(Optional.of(reserved("order-1")));
        when(userVoucherRepository.save(any(UserVoucher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserVoucherResult result = service().apply(new VoucherCommands.ApplyVoucher(
                "user-1", "V-1", "order-1", new BigDecimal("50000"), "VND"));

        assertThat(result.status()).isEqualTo(UserVoucherStatus.APPLIED.name());
        assertThat(campaign.getBudgetRemaining().amount()).isEqualByComparingTo("950000");
        verify(campaignRepository).save(campaign);
    }

    @Test
    void applyFallsBackToVoucherCurrency() {
        mapperEchoes();
        when(voucherRepository.lockByCode("V-1")).thenReturn(Optional.of(reservedVoucher(null)));
        when(voucherRepository.findByCode("V-1")).thenReturn(Optional.of(platformVoucher(null)));
        when(userVoucherRepository.findByUserAndCode("user-1", "V-1"))
                .thenReturn(Optional.of(reserved("order-1")));
        when(userVoucherRepository.save(any(UserVoucher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserVoucherResult result = service().apply(new VoucherCommands.ApplyVoucher(
                "user-1", "V-1", "order-1", new BigDecimal("50000"), null));

        assertThat(result.currency()).isEqualTo("VND");
    }

    @Test
    void applyRejectsWhenReservedByAnotherOrder() {
        when(voucherRepository.lockByCode("V-1")).thenReturn(Optional.of(reservedVoucher(null)));
        when(userVoucherRepository.findByUserAndCode("user-1", "V-1"))
                .thenReturn(Optional.of(reserved("order-other")));

        VoucherService service = service();
        VoucherCommands.ApplyVoucher command = new VoucherCommands.ApplyVoucher(
                "user-1", "V-1", "order-1", new BigDecimal("50000"), "VND");

        assertThatThrownBy(() -> service.apply(command))
                .isInstanceOf(PromotionException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        PromotionErrorCode.USER_VOUCHER_RESERVED_BY_OTHER.getCode());
    }

    @Test
    void releaseClearsReservation() {
        mapperEchoes();
        when(voucherRepository.lockByCode("V-1")).thenReturn(Optional.of(reservedVoucher(null)));
        when(voucherRepository.findByCode("V-1")).thenReturn(Optional.of(platformVoucher(null)));
        when(userVoucherRepository.findByUserAndCode("user-1", "V-1"))
                .thenReturn(Optional.of(reserved("order-1")));
        when(userVoucherRepository.save(any(UserVoucher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserVoucherResult result = service().release(new VoucherCommands.ReleaseVoucher(
                "user-1", "V-1", "order-1", "buyer cancelled"));

        assertThat(result.status()).isEqualTo(UserVoucherStatus.RELEASED.name());
        assertThat(result.reservedOrderId()).isNull();
    }

    @Test
    void releaseRejectsWhenOrderDoesNotMatch() {
        when(voucherRepository.lockByCode("V-1")).thenReturn(Optional.of(reservedVoucher(null)));
        when(userVoucherRepository.findByUserAndCode("user-1", "V-1"))
                .thenReturn(Optional.of(claimed()));

        VoucherService service = service();
        VoucherCommands.ReleaseVoucher command = new VoucherCommands.ReleaseVoucher(
                "user-1", "V-1", "order-1", "nope");

        assertThatThrownBy(() -> service.release(command))
                .isInstanceOf(PromotionException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        PromotionErrorCode.USER_VOUCHER_RESERVED_BY_OTHER.getCode());
    }

    @Test
    void releaseByOrderReturnsZeroWhenNoReservation() {
        when(userVoucherRepository.findByReservedOrderId("order-1")).thenReturn(Optional.empty());

        assertThat(service().releaseByOrder("order-1", "cancelled")).isZero();
        verify(userVoucherRepository, never()).save(any());
    }

    @Test
    void releaseByOrderReleasesReservation() {
        when(userVoucherRepository.findByReservedOrderId("order-1"))
                .thenReturn(Optional.of(reserved("order-1")));
        when(voucherRepository.lockByCode("V-1")).thenReturn(Optional.of(reservedVoucher(null)));

        assertThat(service().releaseByOrder("order-1", "cancelled")).isEqualTo(1);
        verify(voucherRepository).save(any(Voucher.class));
        verify(userVoucherRepository).save(any(UserVoucher.class));
    }

    @Test
    void releaseByOrderToleratesMissingVoucher() {
        when(userVoucherRepository.findByReservedOrderId("order-1"))
                .thenReturn(Optional.of(reserved("order-1")));
        when(voucherRepository.lockByCode("V-1")).thenReturn(Optional.empty());

        assertThat(service().releaseByOrder("order-1", "cancelled")).isEqualTo(1);
        verify(voucherRepository, never()).save(any());
    }

    @Test
    void releaseExpiredReservationsSkipsFailures() {
        when(userVoucherRepository.findExpiredReservations(eq(NOW), anyInt()))
                .thenReturn(List.of(reserved("order-1"), claimed()));
        when(voucherRepository.lockByCode("V-1")).thenReturn(Optional.of(reservedVoucher(null)));

        assertThat(service().releaseExpiredReservations(NOW, 10)).isEqualTo(1);
    }

    @Test
    void getMineThrowsWhenMissing() {
        when(userVoucherRepository.findByUserAndCode("user-1", "V-9")).thenReturn(Optional.empty());

        VoucherService service = service();

        assertThatThrownBy(() -> service.getMine("user-1", "V-9"))
                .isInstanceOf(PromotionException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        PromotionErrorCode.USER_VOUCHER_NOT_FOUND.getCode());
    }

    @Test
    void getMineReturnsBaseResultWhenVoucherRowMissing() {
        mapperEchoes();
        when(userVoucherRepository.findByUserAndCode("user-1", "V-1"))
                .thenReturn(Optional.of(claimed()));
        when(voucherRepository.findByCode("V-1")).thenReturn(Optional.empty());

        UserVoucherResult result = service().getMine("user-1", "V-1");

        assertThat(result.voucherDiscountAmount()).isNull();
        assertThat(result.voucherScope()).isNull();
    }

    @Test
    void listMineMapsEveryUserVoucher() {
        mapperEchoes();
        when(userVoucherRepository.findByUser("user-1", 50))
                .thenReturn(List.of(claimed(), reserved("order-1")));
        when(voucherRepository.findByCode("V-1")).thenReturn(Optional.of(platformVoucher(null)));

        assertThat(service().listMine("user-1", 50)).hasSize(2);
    }
}
