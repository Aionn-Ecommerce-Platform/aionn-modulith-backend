package com.aionn.promotion.application.service;

import com.aionn.promotion.adapter.rest.dto.voucher.IssueVoucherRequest;
import com.aionn.promotion.application.dto.voucher.result.VoucherResult;
import com.aionn.promotion.application.mapper.PromotionResultMapper;
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ShopVoucherService {

    private final VoucherPersistencePort voucherRepository;
    private final MerchantQueryPort merchantQueryPort;
    private final PromotionResultMapper mapper;
    private final EventPublisher eventPublisher;

    public VoucherResult issue(String ownerId, IssueVoucherRequest request) {
        String merchantId = merchantIdFor(ownerId);
        String voucherCode = request.voucherCode().trim().toUpperCase();
        if (voucherRepository.findByCode(voucherCode).isPresent()) {
            throw new PromotionException(PromotionErrorCode.VOUCHER_DUPLICATE_CODE);
        }
        Voucher voucher = Voucher.issueForShop(
                voucherCode,
                merchantId,
                Money.of(request.discountAmount(), request.currency() == null ? "VND" : request.currency()),
                request.usageLimit(), request.validFrom(), request.validUntil());
        Voucher saved = voucherRepository.save(voucher);
        eventPublisher.publish(voucher.pullEvents());
        return mapper.toResult(saved);
    }

    @Transactional(readOnly = true)
    public List<VoucherResult> listMine(String ownerId, int limit) {
        return voucherRepository.findByMerchantId(
                        merchantIdFor(ownerId), Math.min(Math.max(limit, 1), 100)).stream()
                .map(mapper::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VoucherResult> listByMerchant(String merchantId, int limit) {
        return voucherRepository.findByMerchantId(merchantId, Math.min(Math.max(limit, 1), 100)).stream()
                .filter(voucher -> voucher.isValidNow(java.time.Instant.now()))
                .map(mapper::toResult)
                .toList();
    }

    private String merchantIdFor(String ownerId) {
        return merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.MERCHANT_NOT_FOUND));
    }
}
