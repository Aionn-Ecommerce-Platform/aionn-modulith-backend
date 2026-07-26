package com.aionn.ordering.application.usecase.returns;

import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.application.mapper.ReturnResultMapper;
import com.aionn.ordering.application.port.in.returns.AdminGetReturnInputPort;
import com.aionn.ordering.application.service.OrderReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminGetReturnUseCase implements AdminGetReturnInputPort {

    private final OrderReturnService orderReturnService;
    private final ReturnResultMapper returnResultMapper;

    @Override
    @Transactional(readOnly = true)
    public ReturnResult execute(String returnId) {
        return returnResultMapper.toResult(orderReturnService.adminGet(returnId));
    }
}
