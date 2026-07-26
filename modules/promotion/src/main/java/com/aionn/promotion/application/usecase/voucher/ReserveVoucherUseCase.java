package com.aionn.promotion.application.usecase.voucher;

import com.aionn.promotion.application.dto.voucher.command.VoucherCommands;
import com.aionn.promotion.application.dto.voucher.result.UserVoucherResult;
import com.aionn.promotion.application.port.in.voucher.ReserveVoucherInputPort;
import com.aionn.promotion.application.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReserveVoucherUseCase implements ReserveVoucherInputPort {

    private final VoucherService voucherService;

    @Override
    @Transactional
    public UserVoucherResult execute(VoucherCommands.ReserveVoucher command) {
        return voucherService.reserve(command);
    }
}
