package com.aionn.ordering.application.usecase.returns;

import com.aionn.ordering.application.dto.returns.command.ConfirmItemReceivedCommand;
import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.application.port.in.returns.ConfirmItemReceivedInputPort;
import com.aionn.ordering.application.service.OrderReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfirmItemReceivedUseCase implements ConfirmItemReceivedInputPort {

    private final OrderReturnService orderReturnService;

    @Override
    public ReturnResult execute(ConfirmItemReceivedCommand command) {
        return orderReturnService.confirmItemReceived(command);
    }
}
