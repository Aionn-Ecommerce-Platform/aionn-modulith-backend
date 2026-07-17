package com.aionn.inventory.application.usecase.transfer;

import com.aionn.inventory.application.dto.transfer.command.InitiateTransferCommand;
import com.aionn.inventory.application.dto.transfer.result.StockTransferResult;
import com.aionn.inventory.application.port.in.transfer.InitiateTransferInputPort;
import com.aionn.inventory.application.service.StockTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InitiateTransferUseCase implements InitiateTransferInputPort {

    private final StockTransferService stockTransferService;

    @Override
    @Transactional
    public StockTransferResult execute(InitiateTransferCommand command) {
        return stockTransferService.initiate(command);
    }
}
