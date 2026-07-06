package com.aionn.catalog.application.usecase.merchant;

import com.aionn.catalog.application.dto.merchant.command.SuspendMerchantCommand;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;
import com.aionn.catalog.application.port.in.merchant.SuspendMerchantInputPort;
import com.aionn.catalog.application.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SuspendMerchantUseCase implements SuspendMerchantInputPort {

    private final MerchantService merchantService;

    @Override
    public MerchantResult execute(SuspendMerchantCommand command) {
        return merchantService.suspend(command);
    }
}
