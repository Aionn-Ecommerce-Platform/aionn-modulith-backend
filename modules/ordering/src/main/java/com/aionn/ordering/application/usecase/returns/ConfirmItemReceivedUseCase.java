package com.aionn.ordering.application.usecase.returns;

import com.aionn.ordering.application.dto.returns.command.ConfirmItemReceivedCommand;
import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.application.mapper.ReturnResultMapper;
import com.aionn.ordering.application.port.in.returns.ConfirmItemReceivedInputPort;
import com.aionn.ordering.application.service.OrderReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfirmItemReceivedUseCase implements ConfirmItemReceivedInputPort {

    private final OrderReturnService orderReturnService;
    private final ReturnResultMapper returnResultMapper;

    @Override
    @Transactional
    public ReturnResult execute(ConfirmItemReceivedCommand command) {
        return returnResultMapper.toResult(orderReturnService.confirmItemReceived(command));
    }
}
