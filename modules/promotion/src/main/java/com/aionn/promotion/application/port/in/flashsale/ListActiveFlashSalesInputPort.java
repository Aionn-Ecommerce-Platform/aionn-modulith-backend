package com.aionn.promotion.application.port.in.flashsale;

import com.aionn.promotion.application.dto.flashsale.result.ActiveFlashSaleResult;

import java.util.List;

public interface ListActiveFlashSalesInputPort {
    List<ActiveFlashSaleResult> execute(int limit);
}
