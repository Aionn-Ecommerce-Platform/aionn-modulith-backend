package com.aionn.promotion.application.usecase.flashsale;

import com.aionn.promotion.application.dto.flashsale.result.FlashSaleRegistrationResult;
import com.aionn.promotion.application.mapper.FlashSaleResultMapper;
import com.aionn.promotion.application.port.in.flashsale.ListFlashSaleRegistrationsByStatusInputPort;
import com.aionn.promotion.application.service.FlashSaleService;
import com.aionn.promotion.domain.valueobject.FlashSaleRegistrationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListFlashSaleRegistrationsByStatusUseCase implements ListFlashSaleRegistrationsByStatusInputPort {

    private final FlashSaleService flashSaleService;
    private final FlashSaleResultMapper flashSaleResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<FlashSaleRegistrationResult> execute(FlashSaleRegistrationStatus status, int limit) {
        return flashSaleResultMapper.toResults(flashSaleService.listByStatus(status, limit));
    }
}
