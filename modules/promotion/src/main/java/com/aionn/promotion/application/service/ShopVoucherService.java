package com.aionn.promotion.application.service;

import com.aionn.promotion.application.dto.voucher.command.VoucherCommands;
import com.aionn.promotion.application.port.out.VoucherPersistencePort;
import com.aionn.promotion.domain.exception.PromotionErrorCode;
import com.aionn.promotion.domain.exception.PromotionException;
import com.aionn.promotion.domain.model.Voucher;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ShopVoucherService {

    private static final int MAX_LIMIT = 100;

    private final VoucherPersistencePort voucherRepository;
    private final MerchantQueryPort merchantQueryPort;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public Voucher issue(VoucherCommands.IssueShopVoucher command) {
        String merchantId = merchantIdFor(command.ownerId());
        String voucherCode = command.voucherCode().trim().toUpperCase();
        if (voucherRepository.findByCode(voucherCode).isPresent()) {
            throw new PromotionException(PromotionErrorCode.VOUCHER_DUPLICATE_CODE);
        }
        Voucher voucher = Voucher.issueForShop(
                voucherCode,
                merchantId,
                Money.of(command.discountAmount(), command.currency() == null ? "VND" : command.currency()),
                command.usageLimit(), command.validFrom(), command.validUntil(), clock);
        Voucher saved = voucherRepository.save(voucher);
        eventPublisher.publish(voucher.pullEvents());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Voucher> listMine(String ownerId, int limit) {
        return voucherRepository.findByMerchantId(merchantIdFor(ownerId), safeLimit(limit));
    }

    @Transactional(readOnly = true)
    public List<Voucher> listByMerchant(String merchantId, int limit) {
        return voucherRepository.findByMerchantId(merchantId, safeLimit(limit)).stream()
                .filter(voucher -> voucher.isValidNow(clock.instant()))
                .toList();
    }

    private static int safeLimit(int limit) {
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }

    private String merchantIdFor(String ownerId) {
        return merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.MERCHANT_NOT_FOUND));
    }
}
