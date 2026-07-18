package com.aionn.ordering.application.usecase.returns;

import com.aionn.ordering.application.dto.returns.command.RequestReturnCommand;
import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.application.port.in.returns.RequestReturnInputPort;
import com.aionn.ordering.application.service.OrderReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RequestReturnUseCase implements RequestReturnInputPort {

    private final OrderReturnService orderReturnService;

    @Override
    public ReturnResult execute(RequestReturnCommand command) {
        return orderReturnService.requestReturn(command);
    }
}
