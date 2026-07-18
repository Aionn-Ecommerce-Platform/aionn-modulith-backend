package com.aionn.ordering.application.usecase.returns;

import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.application.port.in.returns.GetReturnInputPort;
import com.aionn.ordering.application.service.OrderReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetReturnUseCase implements GetReturnInputPort {

    private final OrderReturnService orderReturnService;

    @Override
    public ReturnResult execute(String returnId, String userId) {
        return orderReturnService.getForRequester(returnId, userId);
    }
}