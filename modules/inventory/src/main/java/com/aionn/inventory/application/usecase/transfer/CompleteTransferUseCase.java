package com.aionn.inventory.application.usecase.transfer;

import com.aionn.inventory.application.dto.transfer.command.CompleteTransferCommand;
import com.aionn.inventory.application.dto.transfer.result.StockTransferResult;
import com.aionn.inventory.application.port.in.transfer.CompleteTransferInputPort;
import com.aionn.inventory.application.service.StockTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompleteTransferUseCase implements CompleteTransferInputPort {

    private final StockTransferService stockTransferService;

    @Override
    @Transactional
    public StockTransferResult execute(CompleteTransferCommand command) {
        return stockTransferService.complete(command);
    }
}
