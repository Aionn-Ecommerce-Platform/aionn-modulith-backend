package com.aionn.promotion.application.usecase.voucher;

import com.aionn.promotion.application.dto.voucher.command.VoucherCommands;
import com.aionn.promotion.application.dto.voucher.result.UserVoucherResult;
import com.aionn.promotion.application.port.in.voucher.ApplyVoucherInputPort;
import com.aionn.promotion.application.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("promotionApplyVoucherUseCase")
@RequiredArgsConstructor
public class ApplyVoucherUseCase implements ApplyVoucherInputPort {

    private final VoucherService voucherService;

    @Override
    @Transactional
    public UserVoucherResult execute(VoucherCommands.ApplyVoucher command) {
        return voucherService.apply(command);
    }
}
