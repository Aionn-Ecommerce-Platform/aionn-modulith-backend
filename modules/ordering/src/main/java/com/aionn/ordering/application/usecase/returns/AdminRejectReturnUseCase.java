package com.aionn.ordering.application.usecase.returns;

import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.application.mapper.ReturnResultMapper;
import com.aionn.ordering.application.port.in.returns.AdminRejectReturnInputPort;
import com.aionn.ordering.application.service.OrderReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminRejectReturnUseCase implements AdminRejectReturnInputPort {

    private final OrderReturnService orderReturnService;
    private final ReturnResultMapper returnResultMapper;

    @Override
    @Transactional
    public ReturnResult execute(String returnId, String reason) {
        return returnResultMapper.toResult(orderReturnService.adminReject(returnId, reason));
    }
}
