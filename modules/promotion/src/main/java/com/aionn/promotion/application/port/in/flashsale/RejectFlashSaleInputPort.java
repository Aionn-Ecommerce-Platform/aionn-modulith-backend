package com.aionn.promotion.application.port.in.flashsale;

import com.aionn.promotion.application.dto.flashsale.command.FlashSaleCommands;
import com.aionn.promotion.application.dto.flashsale.result.FlashSaleRegistrationResult;

public interface RejectFlashSaleInputPort {
    FlashSaleRegistrationResult execute(FlashSaleCommands.RejectFlashSale command);
}
