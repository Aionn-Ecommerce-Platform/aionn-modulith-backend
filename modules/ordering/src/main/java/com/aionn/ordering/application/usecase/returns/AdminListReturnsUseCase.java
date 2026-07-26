package com.aionn.ordering.application.usecase.returns;

import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.application.mapper.ReturnResultMapper;
import com.aionn.ordering.application.port.in.returns.AdminListReturnsInputPort;
import com.aionn.ordering.application.service.OrderReturnService;
import com.aionn.ordering.domain.valueobject.ReturnStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminListReturnsUseCase implements AdminListReturnsInputPort {

    private final OrderReturnService orderReturnService;
    private final ReturnResultMapper returnResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ReturnResult> execute(ReturnStatus status, int limit) {
        return returnResultMapper.toResults(orderReturnService.adminListByStatus(status, limit));
    }
}
