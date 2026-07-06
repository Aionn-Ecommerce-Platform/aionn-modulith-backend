package com.aionn.catalog.application.usecase.merchant;

import com.aionn.catalog.application.dto.merchant.command.RegisterMerchantCommand;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;
import com.aionn.catalog.application.port.in.merchant.RegisterMerchantInputPort;
import com.aionn.catalog.application.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterMerchantUseCase implements RegisterMerchantInputPort {

    private final MerchantService merchantService;

    @Override
    public MerchantResult execute(RegisterMerchantCommand command) {
        return merchantService.register(command);
    }
}
