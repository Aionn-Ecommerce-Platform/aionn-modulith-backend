package com.aionn.promotion.application.usecase.voucher;

import com.aionn.promotion.application.dto.voucher.command.VoucherCommands;
import com.aionn.promotion.application.dto.voucher.result.VoucherResult;
import com.aionn.promotion.application.mapper.VoucherResultMapper;
import com.aionn.promotion.application.port.in.voucher.IssueShopVoucherInputPort;
import com.aionn.promotion.application.service.ShopVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IssueShopVoucherUseCase implements IssueShopVoucherInputPort {

    private final ShopVoucherService shopVoucherService;
    private final VoucherResultMapper voucherResultMapper;

    @Override
    @Transactional
    public VoucherResult execute(VoucherCommands.IssueShopVoucher command) {
        return voucherResultMapper.toResult(shopVoucherService.issue(command));
    }
}
