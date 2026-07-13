package com.aionn.inventory.application.port.in.inventory;

import com.aionn.inventory.application.dto.analytics.result.LowStockAlertResult;
import java.util.List;

public interface GetMerchantLowStockInputPort {
    List<LowStockAlertResult> execute(String ownerId);
}
