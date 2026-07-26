package com.aionn.promotion.application.usecase.voucher;

import com.aionn.promotion.application.dto.voucher.command.VoucherCommands;
import com.aionn.promotion.application.dto.voucher.result.UserVoucherResult;
import com.aionn.promotion.application.port.in.voucher.ReleaseVoucherInputPort;
import com.aionn.promotion.application.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReleaseVoucherUseCase implements ReleaseVoucherInputPort {

    private final VoucherService voucherService;

    @Override
    @Transactional
    public UserVoucherResult execute(VoucherCommands.ReleaseVoucher command) {
        return voucherService.release(command);
    }
}
