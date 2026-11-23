package com.aionn.ordering.application.usecase.returns;

import com.aionn.ordering.application.dto.returns.result.ReturnPageResult;
import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.application.mapper.ReturnResultMapper;
import com.aionn.ordering.application.port.in.returns.ListReturnsInputPort;
import com.aionn.ordering.application.service.OrderReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListReturnsUseCase implements ListReturnsInputPort {

    private final OrderReturnService orderReturnService;
    private final ReturnResultMapper returnResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ReturnResult> execute(String requesterId, String type, int limit) {
        if ("MERCHANT".equalsIgnoreCase(type)) {
            return returnResultMapper.toResults(orderReturnService.listMerchant(requesterId, limit));
        }
        return returnResultMapper.toResults(orderReturnService.listMine(requesterId, limit));
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnPageResult executePaged(
            String requesterId,
            String type,
            String status,
            String reasonType,
            String search,
            int page,
            int limit) {
        if ("MERCHANT".equalsIgnoreCase(type)) {
            OrderReturnService.ReturnPage result = orderReturnService.searchMerchantPage(
                    requesterId, status, reasonType, search, page, limit);
            return new ReturnPageResult(
                    returnResultMapper.toResults(result.content()), result.totalElements());
        }
        List<ReturnResult> content = returnResultMapper.toResults(
                orderReturnService.listMine(requesterId, limit));
        return new ReturnPageResult(content, content.size());
    }
}
