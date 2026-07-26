package com.aionn.promotion.application.usecase.flashsale;

import com.aionn.promotion.application.dto.flashsale.result.ActiveFlashSaleResult;
import com.aionn.promotion.application.port.in.flashsale.ListActiveFlashSalesInputPort;
import com.aionn.promotion.application.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListActiveFlashSalesUseCase implements ListActiveFlashSalesInputPort {

    private final FlashSaleService flashSaleService;

    @Override
    @Transactional(readOnly = true)
    public List<ActiveFlashSaleResult> execute(int limit) {
        return flashSaleService.listActive(limit);
    }
}
