package com.aionn.ordering.application.port.in.returns;

import com.aionn.ordering.application.dto.returns.result.ReturnResult;

import java.math.BigDecimal;

public interface AdminApproveReturnInputPort {
    ReturnResult execute(String returnId, BigDecimal refundAmount, String currency, String returnWarehouseId);
}
