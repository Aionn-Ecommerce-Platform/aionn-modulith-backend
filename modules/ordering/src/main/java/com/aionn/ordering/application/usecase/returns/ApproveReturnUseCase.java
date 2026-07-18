package com.aionn.ordering.application.usecase.returns;

import com.aionn.ordering.application.dto.returns.command.ApproveReturnCommand;
import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.application.port.in.returns.ApproveReturnInputPort;
import com.aionn.ordering.application.service.OrderReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApproveReturnUseCase implements ApproveReturnInputPort {

    private final OrderReturnService orderReturnService;

    @Override
    public ReturnResult execute(ApproveReturnCommand command) {
        return orderReturnService.approve(command);
    }
}