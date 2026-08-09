package com.aionn.promotion.infrastructure.integration;

import com.aionn.promotion.application.port.out.PromotionCampaignPersistencePort;
import com.aionn.promotion.application.port.out.UserVoucherPersistencePort;
import com.aionn.promotion.application.port.out.VoucherPersistencePort;
import com.aionn.promotion.domain.model.PromotionCampaign;
import com.aionn.promotion.domain.model.UserVoucher;
import com.aionn.promotion.domain.model.Voucher;
import com.aionn.promotion.domain.valueobject.UserVoucherStatus;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.integration.port.promotion.VoucherApplyPort;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Clock;
import java.util.List;

/**
 * Handles discount calculations and applies/consumes the voucher in the
 * database
 * as part of the checkout transaction.
 */
@Slf4j
@Component("promotionVoucherApplyAdapter")
@RequiredArgsConstructor
public class VoucherApplyAdapter implements VoucherApplyPort {

    private final VoucherPersistencePort voucherRepository;
    private final UserVoucherPersistencePort userVoucherRepository;
    private final PromotionCampaignPersistencePort campaignRepository;
    private final Clock clock;

    @Override
    @Transactional
    public Discount apply(String userId, String merchantId, String voucherCode, String orderId,
            BigDecimal lineSubtotal, String currency, List<String> orderCategoryIds) {
        Voucher voucher = voucherRepository.lockByCode(voucherCode).orElse(null);
        if (voucher == null) {
            return new Discount(BigDecimal.ZERO, currency, false, "voucher-not-found");
        }
        if (!voucher.appliesToMerchant(merchantId)) {
            return new Discount(BigDecimal.ZERO, currency, false, "voucher-wrong-shop");
        }

        UserVoucher uv = userVoucherRepository.findByUserAndCode(userId, voucherCode).orElse(null);
        if (uv != null && uv.getStatus() == UserVoucherStatus.APPLIED) {
            if (orderId.equals(uv.getReservedOrderId()) && uv.getAppliedAmount() != null) {
                return new Discount(uv.getAppliedAmount().amount(), uv.getAppliedAmount().currency(), true, null);
            }
            return new Discount(BigDecimal.ZERO, currency, false, "voucher-already-used");
        }

        // Validate campaign conditions if platform voucher
        if (voucher.getCampaignId() != null) {
            PromotionCampaign campaign = campaignRepository.findById(voucher.getCampaignId()).orElse(null);
            if (campaign != null) {
                try {
                    campaign.ensureRunning(clock);
                } catch (Exception e) {
                    return new Discount(BigDecimal.ZERO, currency, false, "campaign-not-running");
                }
                if (!campaign.getCondition().matches(lineSubtotal,
                        orderCategoryIds == null ? List.of() : orderCategoryIds)) {
                    return new Discount(BigDecimal.ZERO, currency, false, "campaign-condition-not-met");
                }
            }
        }

        // Check if user already claimed this voucher
        if (uv != null) {
            if (!voucher.isValidNow(clock.instant())) {
                return new Discount(BigDecimal.ZERO, currency, false, "voucher-not-usable");
            }
            if (uv.getStatus() == UserVoucherStatus.EXPIRED) {
                return new Discount(BigDecimal.ZERO, currency, false, "voucher-expired");
            }
        } else {
            if (!voucher.isUsable(clock.instant())) {
                return new Discount(BigDecimal.ZERO, currency, false, "voucher-not-usable");
            }
            voucher.claimSlot(clock);
            uv = UserVoucher.claim(IdGenerator.ulid(), voucherCode, userId, clock);
            userVoucherRepository.save(uv);
        }

        BigDecimal discount = voucher.getDiscountAmount().amount();
        if (lineSubtotal != null && discount.compareTo(lineSubtotal) > 0) {
            discount = lineSubtotal;
        }
        String discountCurrency = voucher.getDiscountAmount().currency();

        voucher.reserveSlot(clock);
        voucher.commitSlot(clock);

        uv.reserve(orderId, clock.instant().plus(Duration.ofMinutes(15)), clock);
        uv.apply(Money.of(discount, discountCurrency), clock);

        voucherRepository.save(voucher);
        userVoucherRepository.save(uv);

        return new Discount(discount, discountCurrency, true, null);
    }

    @Override
    @Transactional
    public void release(String userId, String orderId, String reason) {
        userVoucherRepository.findByReservedOrderId(orderId)
                .filter(uv -> uv.getUserId().equals(userId))
                .ifPresent(uv -> {
                    if (uv.getStatus() != UserVoucherStatus.APPLIED
                            && uv.getStatus() != UserVoucherStatus.RESERVED) {
                        return;
                    }
                    Voucher voucher = voucherRepository.findByCode(uv.getVoucherCode()).orElse(null);
                    if (voucher != null) {
                        if (uv.getStatus() == UserVoucherStatus.APPLIED) {
                            voucher.releaseCommittedSlot(clock);
                        } else {
                            voucher.releaseSlot(clock);
                        }
                        voucherRepository.save(voucher);
                    }
                    uv.release(reason, clock);
                    userVoucherRepository.save(uv);
                });
    }
}
