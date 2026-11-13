package com.aionn.promotion.infrastructure.integration;

import com.aionn.promotion.application.port.out.PromotionCampaignPersistencePort;
import com.aionn.promotion.application.port.out.UserVoucherPersistencePort;
import com.aionn.promotion.application.port.out.VoucherPersistencePort;
import com.aionn.promotion.application.policy.VoucherReservationPolicy;
import com.aionn.promotion.domain.model.PromotionCampaign;
import com.aionn.promotion.domain.model.UserVoucher;
import com.aionn.promotion.domain.model.Voucher;
import com.aionn.promotion.domain.valueobject.UserVoucherStatus;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.contracts.port.promotion.VoucherApplyPort;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    private final VoucherPersistencePort voucherPersistencePort;
    private final UserVoucherPersistencePort userVoucherPersistencePort;
    private final PromotionCampaignPersistencePort promotionCampaignPersistencePort;
    private final VoucherReservationPolicy reservationPolicy;
    private final Clock clock;

    @Override
    @Transactional
    public Discount apply(String userId, String merchantId, String voucherCode, String orderId,
            BigDecimal lineSubtotal, String currency, List<String> orderCategoryIds) {
        Voucher voucher = voucherPersistencePort.lockByCode(voucherCode).orElse(null);
        if (voucher == null) {
            return new Discount(BigDecimal.ZERO, currency, false, "voucher-not-found");
        }
        if (!voucher.appliesToMerchant(merchantId)) {
            return new Discount(BigDecimal.ZERO, currency, false, "voucher-wrong-shop");
        }

        UserVoucher uv = userVoucherPersistencePort.findByUserAndCodeForUpdate(userId, voucherCode).orElse(null);
        if (uv != null && uv.getStatus() == UserVoucherStatus.APPLIED) {
            if (orderId.equals(uv.getReservedOrderId()) && uv.getAppliedAmount() != null) {
                return new Discount(uv.getAppliedAmount().amount(), uv.getAppliedAmount().currency(), true, null,
                        voucher.isFreeShipping());
            }
            return new Discount(BigDecimal.ZERO, currency, false, "voucher-already-used");
        }

        // Validate campaign conditions if platform voucher
        PromotionCampaign campaign = null;
        if (voucher.getCampaignId() != null) {
            campaign = promotionCampaignPersistencePort.findByIdForUpdate(voucher.getCampaignId()).orElse(null);
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
            if (campaign != null && campaign.getCondition().maxClaimsPerUser() != null
                    && userVoucherPersistencePort.countByUserAndCampaign(userId, campaign.getCampaignId())
                            >= campaign.getCondition().maxClaimsPerUser()) {
                return new Discount(BigDecimal.ZERO, currency, false, "claim-limit-reached");
            }
            voucher.claimSlot(clock);
            uv = UserVoucher.claim(IdGenerator.ulid(), voucherCode, userId, clock);
            userVoucherPersistencePort.save(uv);
        }

        if (uv.getStatus() == UserVoucherStatus.RESERVED) {
            if (!orderId.equals(uv.getReservedOrderId())) {
                return new Discount(BigDecimal.ZERO, currency, false, "voucher-reserved-by-other");
            }
        }

        String voucherCurrency = voucher.getDiscountAmount().currency();
        if (currency != null && !currency.isBlank() && !currency.equalsIgnoreCase(voucherCurrency)) {
            return new Discount(BigDecimal.ZERO, currency, false, "voucher-currency-mismatch");
        }
        BigDecimal discount = voucher.getDiscountAmount().amount();
        // Goods vouchers cannot exceed the goods they discount. A free-shipping
        // voucher is capped against the shipping fee by the caller instead, so it
        // must not be clamped to the (usually larger) line subtotal here.
        if (!voucher.isFreeShipping() && lineSubtotal != null && discount.compareTo(lineSubtotal) > 0) {
            discount = lineSubtotal;
        }

        if (uv.getStatus() != UserVoucherStatus.RESERVED) {
            voucher.reserveSlot(clock);
            uv.reserve(orderId, reservationPolicy.expiresFrom(clock.instant()), clock);
        }
        voucher.commitSlot(clock);
        uv.apply(Money.of(discount, voucherCurrency), clock);

        // Consume the campaign budget the same way VoucherService.apply does —
        // ordering goes through this adapter, so without it a platform campaign's
        // budget would never be decremented at checkout.
        if (campaign != null) {
            campaign.consumeBudget(Money.of(discount, voucherCurrency), clock);
            promotionCampaignPersistencePort.save(campaign);
        }

        voucherPersistencePort.save(voucher);
        userVoucherPersistencePort.save(uv);

        return new Discount(discount, voucherCurrency, true, null, voucher.isFreeShipping());
    }

    @Override
    @Transactional
    public void release(String userId, String orderId, String reason) {
        userVoucherPersistencePort.findByReservedOrderIdForUpdate(orderId)
                .filter(uv -> uv.getUserId().equals(userId))
                .ifPresent(uv -> {
                    if (uv.getStatus() != UserVoucherStatus.APPLIED
                            && uv.getStatus() != UserVoucherStatus.RESERVED) {
                        return;
                    }
                    Voucher voucher = voucherPersistencePort.lockByCode(uv.getVoucherCode()).orElse(null);
                    if (voucher != null) {
                        if (uv.getStatus() == UserVoucherStatus.APPLIED) {
                            voucher.releaseCommittedSlot(clock);
                        } else {
                            voucher.releaseSlot(clock);
                        }
                        voucherPersistencePort.save(voucher);
                    }
                    uv.release(reason, clock);
                    userVoucherPersistencePort.save(uv);
                });
    }
}
