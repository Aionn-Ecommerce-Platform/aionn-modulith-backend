package com.aionn.ordering.application.usecase.returns;

import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.application.mapper.ReturnResultMapper;
import com.aionn.ordering.application.port.in.returns.AdminConfirmItemReceivedInputPort;
import com.aionn.ordering.application.service.OrderReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminConfirmItemReceivedUseCase implements AdminConfirmItemReceivedInputPort {

    private final OrderReturnService orderReturnService;
    private final ReturnResultMapper returnResultMapper;

    @Override
    @Transactional
    public ReturnResult execute(String returnId, String itemCondition) {
        return returnResultMapper.toResult(
                orderReturnService.adminConfirmItemReceived(returnId, itemCondition));
    }
}
