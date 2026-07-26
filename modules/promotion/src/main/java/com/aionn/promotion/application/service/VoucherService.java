package com.aionn.promotion.application.service;

import com.aionn.promotion.application.dto.voucher.command.VoucherCommands;
import com.aionn.promotion.application.dto.voucher.result.UserVoucherResult;
import com.aionn.promotion.application.mapper.PromotionResultMapper;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.promotion.application.port.out.PromotionCampaignPersistencePort;
import com.aionn.promotion.application.port.out.UserVoucherPersistencePort;
import com.aionn.promotion.application.port.out.VoucherPersistencePort;
import com.aionn.promotion.domain.exception.PromotionErrorCode;
import com.aionn.promotion.domain.exception.PromotionException;
import com.aionn.promotion.domain.model.PromotionCampaign;
import com.aionn.promotion.domain.model.UserVoucher;
import com.aionn.promotion.domain.model.Voucher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VoucherService {

    private static final Duration DEFAULT_RESERVATION_TTL = Duration.ofMinutes(15);

    private final VoucherPersistencePort voucherRepository;
    private final UserVoucherPersistencePort userVoucherRepository;
    private final PromotionCampaignPersistencePort campaignRepository;
    private final PromotionResultMapper mapper;
    private final EventPublisher eventPublisher;

    public UserVoucherResult claim(VoucherCommands.ClaimVoucher command) {
        Voucher voucher = voucherRepository.lockByCode(command.voucherCode())
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.VOUCHER_NOT_FOUND));
        runningCampaignOrNull(voucher);

        userVoucherRepository.findByUserAndCode(command.userId(), command.voucherCode())
                .ifPresent(existing -> {
                    throw new PromotionException(PromotionErrorCode.USER_VOUCHER_ALREADY_CLAIMED);
                });

        voucher.claimSlot();
        UserVoucher uv = UserVoucher.claim(IdGenerator.ulid(), command.voucherCode(), command.userId());
        voucherRepository.save(voucher);
        UserVoucher saved = userVoucherRepository.save(uv);
        eventPublisher.publish(uv.pullEvents());
        return toResult(saved);
    }

    public UserVoucherResult reserve(VoucherCommands.ReserveVoucher command) {
        Voucher voucher = voucherRepository.lockByCode(command.voucherCode())
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.VOUCHER_NOT_FOUND));
        PromotionCampaign campaign = runningCampaignOrNull(voucher);

        if (campaign != null && !campaign.getCondition().matches(command.orderValue(), command.orderCategoryIds())) {
            throw new PromotionException(PromotionErrorCode.CONDITION_NOT_MET);
        }

        UserVoucher uv = userVoucherRepository.findByUserAndCode(command.userId(), command.voucherCode())
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.USER_VOUCHER_NOT_FOUND));
        Instant expiresAt = command.expiresAt() != null ? command.expiresAt()
                : Instant.now().plus(DEFAULT_RESERVATION_TTL);
        voucher.reserveSlot();
        uv.reserve(command.orderId(), expiresAt);

        voucherRepository.save(voucher);
        UserVoucher saved = userVoucherRepository.save(uv);
        eventPublisher.publish(uv.pullEvents());
        return toResult(saved);
    }

    public UserVoucherResult apply(VoucherCommands.ApplyVoucher command) {
        Voucher voucher = voucherRepository.lockByCode(command.voucherCode())
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.VOUCHER_NOT_FOUND));
        PromotionCampaign campaign = runningCampaignOrNull(voucher);
        UserVoucher uv = userVoucherRepository.findByUserAndCode(command.userId(), command.voucherCode())
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.USER_VOUCHER_NOT_FOUND));
        if (uv.getReservedOrderId() == null || !uv.getReservedOrderId().equals(command.orderId())) {
            throw new PromotionException(PromotionErrorCode.USER_VOUCHER_RESERVED_BY_OTHER);
        }
        Money applied = Money.of(command.appliedAmount(),
                command.currency() == null ? voucher.getDiscountAmount().currency() : command.currency());
        voucher.commitSlot();
        if (campaign != null) {
            campaign.consumeBudget(applied);
        }
        uv.apply(applied);

        voucherRepository.save(voucher);
        if (campaign != null) {
            campaignRepository.save(campaign);
        }
        UserVoucher saved = userVoucherRepository.save(uv);
        eventPublisher.publish(uv.pullEvents());
        return toResult(saved);
    }

    public UserVoucherResult release(VoucherCommands.ReleaseVoucher command) {
        Voucher voucher = voucherRepository.lockByCode(command.voucherCode())
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.VOUCHER_NOT_FOUND));
        UserVoucher uv = userVoucherRepository.findByUserAndCode(command.userId(), command.voucherCode())
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.USER_VOUCHER_NOT_FOUND));
        if (uv.getReservedOrderId() == null || !uv.getReservedOrderId().equals(command.orderId())) {
            throw new PromotionException(PromotionErrorCode.USER_VOUCHER_RESERVED_BY_OTHER);
        }
        voucher.releaseSlot();
        uv.release(command.reason());

        voucherRepository.save(voucher);
        UserVoucher saved = userVoucherRepository.save(uv);
        eventPublisher.publish(uv.pullEvents());
        return toResult(saved);
    }

    /**
     * Release any voucher reserved for the given orderId (e.g. when the order
     * is cancelled). Returns the number of vouchers released — 0 means no
     * reservation was found.
     */
    public int releaseByOrder(String orderId, String reason) {
        UserVoucher uv = userVoucherRepository.findByReservedOrderId(orderId).orElse(null);
        if (uv == null) {
            return 0;
        }
        Voucher voucher = voucherRepository.lockByCode(uv.getVoucherCode()).orElse(null);
        if (voucher != null) {
            voucher.releaseSlot();
            voucherRepository.save(voucher);
        }
        uv.release(reason);
        userVoucherRepository.save(uv);
        eventPublisher.publish(uv.pullEvents());
        return 1;
    }

    public int releaseExpiredReservations(Instant now, int batchSize) {
        // Kept for backwards compatibility; the auto-release scheduler now drives
        // expiry directly through VoucherAutoReleaseWorker so each release runs
        // in its own transaction and a single failure doesn't poison the batch.
        List<UserVoucher> expired = userVoucherRepository.findExpiredReservations(now, batchSize);
        int released = 0;
        for (UserVoucher uv : expired) {
            try {
                Voucher voucher = voucherRepository.lockByCode(uv.getVoucherCode())
                        .orElse(null);
                if (voucher != null) {
                    voucher.releaseSlot();
                    voucherRepository.save(voucher);
                }
                uv.release("expired");
                userVoucherRepository.save(uv);
                eventPublisher.publish(uv.pullEvents());
                released++;
            } catch (RuntimeException ex) {
                log.warn("Skip release for {}: {}", uv.getUserVoucherId(), ex.getMessage());
            }
        }
        return released;
    }

    @Transactional(readOnly = true)
    public UserVoucherResult getMine(String userId, String voucherCode) {
        return toResult(userVoucherRepository.findByUserAndCode(userId, voucherCode)
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.USER_VOUCHER_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public List<UserVoucherResult> listMine(String userId, int limit) {
        return userVoucherRepository.findByUser(userId, limit).stream().map(this::toResult).toList();
    }

    private UserVoucherResult toResult(UserVoucher userVoucher) {
        UserVoucherResult base = mapper.toResult(userVoucher);
        Voucher voucher = voucherRepository.findByCode(userVoucher.getVoucherCode()).orElse(null);
        if (voucher == null) {
            return base;
        }

        BigDecimal minOrderValue = null;
        if (voucher.getCampaignId() != null) {
            minOrderValue = campaignRepository.findById(voucher.getCampaignId())
                    .map(campaign -> campaign.getCondition().minOrderValue())
                    .orElse(null);
        }

        return new UserVoucherResult(
                base.userVoucherId(), base.voucherCode(), base.userId(), base.status(),
                base.reservedOrderId(), base.appliedAmount(), base.currency(), base.claimedAt(),
                base.reservedAt(), base.reservedExpiresAt(), base.appliedAt(), base.releasedAt(),
                base.updatedAt(), voucher.getDiscountAmount().amount(), voucher.getDiscountAmount().currency(),
                voucher.getScope().name(), voucher.getValidUntil(), minOrderValue,
                voucher.getUsageLimit(), voucher.getUsedCount());
    }

    private PromotionCampaign runningCampaignOrNull(Voucher voucher) {
        if (voucher.getCampaignId() == null) {
            return null;
        }
        PromotionCampaign campaign = campaignRepository.findById(voucher.getCampaignId())
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.CAMPAIGN_NOT_FOUND));
        campaign.ensureRunning();
        return campaign;
    }
}
