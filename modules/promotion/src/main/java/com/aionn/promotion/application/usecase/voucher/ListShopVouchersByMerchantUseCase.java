package com.aionn.promotion.application.usecase.voucher;

import com.aionn.promotion.application.dto.voucher.result.VoucherResult;
import com.aionn.promotion.application.mapper.VoucherResultMapper;
import com.aionn.promotion.application.port.in.voucher.ListShopVouchersByMerchantInputPort;
import com.aionn.promotion.application.service.ShopVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListShopVouchersByMerchantUseCase implements ListShopVouchersByMerchantInputPort {

    private final ShopVoucherService shopVoucherService;
    private final VoucherResultMapper voucherResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResult> execute(String merchantId, int limit) {
        return voucherResultMapper.toResults(shopVoucherService.listByMerchant(merchantId, limit));
    }
}
