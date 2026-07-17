package com.aionn.inventory.application.usecase.transfer;

import com.aionn.inventory.application.dto.transfer.command.CancelTransferCommand;
import com.aionn.inventory.application.dto.transfer.result.StockTransferResult;
import com.aionn.inventory.application.port.in.transfer.CancelTransferInputPort;
import com.aionn.inventory.application.service.StockTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelTransferUseCase implements CancelTransferInputPort {

    private final StockTransferService stockTransferService;

    @Override
    @Transactional
    public StockTransferResult execute(CancelTransferCommand command) {
        return stockTransferService.cancel(command);
    }
}
