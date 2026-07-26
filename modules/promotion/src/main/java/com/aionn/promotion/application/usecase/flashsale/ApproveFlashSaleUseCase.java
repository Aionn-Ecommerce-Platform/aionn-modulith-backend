package com.aionn.promotion.application.usecase.flashsale;

import com.aionn.promotion.application.dto.flashsale.command.FlashSaleCommands;
import com.aionn.promotion.application.dto.flashsale.result.FlashSaleRegistrationResult;
import com.aionn.promotion.application.mapper.FlashSaleResultMapper;
import com.aionn.promotion.application.port.in.flashsale.ApproveFlashSaleInputPort;
import com.aionn.promotion.application.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApproveFlashSaleUseCase implements ApproveFlashSaleInputPort {

    private final FlashSaleService flashSaleService;
    private final FlashSaleResultMapper flashSaleResultMapper;

    @Override
    @Transactional
    public FlashSaleRegistrationResult execute(FlashSaleCommands.ApproveFlashSale command) {
        return flashSaleResultMapper.toResult(flashSaleService.approve(command));
    }
}
