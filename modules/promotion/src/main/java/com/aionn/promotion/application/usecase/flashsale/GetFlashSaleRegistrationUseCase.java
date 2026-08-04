package com.aionn.promotion.application.usecase.flashsale;

import com.aionn.promotion.application.dto.flashsale.result.FlashSaleRegistrationResult;
import com.aionn.promotion.application.mapper.FlashSaleResultMapper;
import com.aionn.promotion.application.port.in.flashsale.GetFlashSaleRegistrationInputPort;
import com.aionn.promotion.application.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetFlashSaleRegistrationUseCase implements GetFlashSaleRegistrationInputPort {

    private final FlashSaleService flashSaleService;
    private final FlashSaleResultMapper flashSaleResultMapper;

    @Override
    @Transactional(readOnly = true)
    public FlashSaleRegistrationResult execute(String registrationId, String ownerId) {
        return flashSaleResultMapper.toResult(flashSaleService.getOwned(registrationId, ownerId));
    }
}
