package com.aionn.ordering.application.usecase.returns;

import com.aionn.ordering.application.dto.returns.command.RejectReturnCommand;
import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.application.port.in.returns.RejectReturnInputPort;
import com.aionn.ordering.application.service.OrderReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RejectReturnUseCase implements RejectReturnInputPort {

    private final OrderReturnService orderReturnService;

    @Override
    public ReturnResult execute(RejectReturnCommand command) {
        return orderReturnService.reject(command);
    }
}