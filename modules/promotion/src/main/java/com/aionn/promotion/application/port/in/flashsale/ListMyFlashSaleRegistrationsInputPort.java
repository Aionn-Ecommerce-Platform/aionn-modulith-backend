package com.aionn.promotion.application.port.in.flashsale;

import com.aionn.promotion.application.dto.flashsale.result.FlashSaleRegistrationResult;
import com.aionn.promotion.domain.valueobject.FlashSaleRegistrationStatus;

import java.util.List;

public interface ListMyFlashSaleRegistrationsInputPort {
    List<FlashSaleRegistrationResult> execute(String ownerId, FlashSaleRegistrationStatus status, int limit);
}
