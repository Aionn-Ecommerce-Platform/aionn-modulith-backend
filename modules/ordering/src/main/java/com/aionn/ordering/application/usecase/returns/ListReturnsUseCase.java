package com.aionn.ordering.application.usecase.returns;

import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.application.port.in.returns.ListReturnsInputPort;
import com.aionn.ordering.application.service.OrderReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListReturnsUseCase implements ListReturnsInputPort {

    private final OrderReturnService orderReturnService;

    @Override
    public List<ReturnResult> execute(String requesterId, String type, int limit) {
        if ("MERCHANT".equalsIgnoreCase(type)) {
            return orderReturnService.listMerchant(requesterId, limit);
        } else {
            return orderReturnService.listMine(requesterId, limit);
        }
    }
}