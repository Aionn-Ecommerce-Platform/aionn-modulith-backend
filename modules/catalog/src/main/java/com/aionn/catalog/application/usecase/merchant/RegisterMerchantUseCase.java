package com.aionn.catalog.application.usecase.merchant;

import com.aionn.catalog.application.dto.merchant.command.RegisterMerchantCommand;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;
import com.aionn.catalog.application.mapper.MerchantResultMapper;
import com.aionn.catalog.application.port.in.merchant.RegisterMerchantInputPort;
import com.aionn.catalog.application.service.MerchantService;
import com.aionn.catalog.domain.model.Merchant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterMerchantUseCase implements RegisterMerchantInputPort {

    private final MerchantService merchantService;
    private final MerchantResultMapper merchantResultMapper;

    @Override
    @Transactional
    public MerchantResult execute(RegisterMerchantCommand command) {
        Merchant merchant = merchantService.register(command);
        return merchantResultMapper.toResult(merchant);
    }
}
