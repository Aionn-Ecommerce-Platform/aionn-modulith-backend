package com.aionn.promotion.application.port.in.flashsale;

import com.aionn.promotion.application.dto.flashsale.result.FlashSaleRegistrationResult;

public interface GetFlashSaleRegistrationInputPort {
    FlashSaleRegistrationResult execute(String registrationId);
}
