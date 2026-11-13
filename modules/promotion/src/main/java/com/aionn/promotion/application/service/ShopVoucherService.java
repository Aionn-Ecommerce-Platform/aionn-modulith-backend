package com.aionn.promotion.application.service;

import com.aionn.promotion.domain.valueobject.VoucherType;

import com.aionn.promotion.application.dto.voucher.command.VoucherCommands;
import com.aionn.promotion.application.policy.PromotionDefaultsPolicy;
import com.aionn.promotion.application.policy.PromotionLimitPolicy;
import com.aionn.promotion.application.port.out.VoucherPersistencePort;
import com.aionn.promotion.domain.exception.PromotionErrorCode;
import com.aionn.promotion.domain.exception.PromotionException;
import com.aionn.promotion.domain.model.Voucher;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.contracts.port.catalog.MerchantQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ShopVoucherService {

    private final VoucherPersistencePort voucherPersistencePort;
    private final MerchantQueryPort merchantQueryPort;
    private final EventPublisher eventPublisher;
    private final PromotionDefaultsPolicy defaultsPolicy;
    private final PromotionLimitPolicy limitPolicy;
    private final Clock clock;

    public Voucher issue(VoucherCommands.IssueShopVoucher command) {
        String merchantId = merchantIdFor(command.ownerId());
        String voucherCode = command.voucherCode().trim().toUpperCase();
        if (voucherPersistencePort.findByCode(voucherCode).isPresent()) {
            throw new PromotionException(PromotionErrorCode.VOUCHER_DUPLICATE_CODE);
        }
        Voucher voucher = Voucher.issueForShop(
                voucherCode,
                merchantId,
                command.type() == null
                        ? VoucherType.ORDER_DISCOUNT
                        : command.type(),
                Money.of(command.discountAmount(), resolveCurrency(command.currency())),
                command.usageLimit(), command.validFrom(), command.validUntil(), clock);
        Voucher saved;
        try {
            saved = voucherPersistencePort.save(voucher);
        } catch (DataIntegrityViolationException ex) {
            throw new PromotionException(PromotionErrorCode.VOUCHER_DUPLICATE_CODE);
        }
        eventPublisher.publish(voucher.pullEvents());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Voucher> listMine(String ownerId, int limit) {
        return voucherPersistencePort.findByMerchantId(merchantIdFor(ownerId), safeLimit(limit));
    }

    @Transactional(readOnly = true)
    public List<Voucher> listByMerchant(String merchantId, int limit) {
        return voucherPersistencePort.findByMerchantId(merchantId, safeLimit(limit)).stream()
                .filter(voucher -> voucher.isValidNow(clock.instant()))
                .toList();
    }

    private int safeLimit(int limit) {
        return Math.min(Math.max(limit, 1), limitPolicy.maxListLimit());
    }

    private String resolveCurrency(String currency) {
        return currency == null || currency.isBlank() ? defaultsPolicy.defaultCurrency() : currency;
    }

    private String merchantIdFor(String ownerId) {
        return merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.MERCHANT_NOT_FOUND));
    }
}
