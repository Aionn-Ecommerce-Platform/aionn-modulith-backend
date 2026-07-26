package com.aionn.promotion.application.usecase.flashsale;

import com.aionn.promotion.application.dto.flashsale.command.FlashSaleCommands;
import com.aionn.promotion.application.dto.flashsale.result.FlashSaleRegistrationResult;
import com.aionn.promotion.application.mapper.FlashSaleResultMapper;
import com.aionn.promotion.application.port.in.flashsale.RegisterFlashSaleInputPort;
import com.aionn.promotion.application.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterFlashSaleUseCase implements RegisterFlashSaleInputPort {

    private final FlashSaleService flashSaleService;
    private final FlashSaleResultMapper flashSaleResultMapper;

    @Override
    @Transactional
    public FlashSaleRegistrationResult execute(FlashSaleCommands.RegisterFlashSale command) {
        return flashSaleResultMapper.toResult(flashSaleService.register(command));
    }
}
