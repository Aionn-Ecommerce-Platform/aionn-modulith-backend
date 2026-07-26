package com.aionn.inventory.application.usecase.transfer;

import com.aionn.inventory.application.dto.transfer.result.StockTransferResult;
import com.aionn.inventory.application.mapper.StockTransferResultMapper;
import com.aionn.inventory.application.port.in.transfer.GetTransferInputPort;
import com.aionn.inventory.application.service.StockTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetTransferUseCase implements GetTransferInputPort {

    private final StockTransferService stockTransferService;
    private final StockTransferResultMapper stockTransferResultMapper;

    @Override
    @Transactional(readOnly = true)
    public StockTransferResult execute(String transferId) {
        return stockTransferResultMapper.toResult(stockTransferService.get(transferId));
    }
}
