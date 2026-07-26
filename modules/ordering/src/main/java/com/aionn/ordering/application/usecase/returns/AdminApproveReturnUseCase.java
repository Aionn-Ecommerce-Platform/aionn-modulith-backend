package com.aionn.ordering.application.usecase.returns;

import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.application.mapper.ReturnResultMapper;
import com.aionn.ordering.application.port.in.returns.AdminApproveReturnInputPort;
import com.aionn.ordering.application.service.OrderReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AdminApproveReturnUseCase implements AdminApproveReturnInputPort {

    private final OrderReturnService orderReturnService;
    private final ReturnResultMapper returnResultMapper;

    @Override
    public ReturnResult execute(String returnId, BigDecimal refundAmount, String currency,
            String returnWarehouseId) {
        return returnResultMapper.toResult(
                orderReturnService.adminApprove(returnId, refundAmount, currency, returnWarehouseId));
    }
}
