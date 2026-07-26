package com.aionn.ordering.application.usecase.returns;

import com.aionn.ordering.application.dto.returns.command.RequestReturnCommand;
import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.application.mapper.ReturnResultMapper;
import com.aionn.ordering.application.port.in.returns.RequestReturnInputPort;
import com.aionn.ordering.application.service.OrderReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequestReturnUseCase implements RequestReturnInputPort {

    private final OrderReturnService orderReturnService;
    private final ReturnResultMapper returnResultMapper;

    @Override
    @Transactional
    public ReturnResult execute(RequestReturnCommand command) {
        return returnResultMapper.toResult(orderReturnService.requestReturn(command));
    }
}
