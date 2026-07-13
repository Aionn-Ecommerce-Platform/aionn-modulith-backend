package com.aionn.inventory.application.usecase.transfer;

import com.aionn.inventory.application.dto.transfer.command.CancelTransferCommand;
import com.aionn.inventory.application.dto.transfer.command.CompleteTransferCommand;
import com.aionn.inventory.application.dto.transfer.command.InitiateTransferCommand;
import com.aionn.inventory.application.dto.transfer.result.StockTransferResult;
import com.aionn.inventory.application.port.in.transfer.CancelTransferInputPort;
import com.aionn.inventory.application.port.in.transfer.CompleteTransferInputPort;
import com.aionn.inventory.application.port.in.transfer.GetTransferInputPort;
import com.aionn.inventory.application.port.in.transfer.InitiateTransferInputPort;
import com.aionn.inventory.application.service.StockTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockTransferUseCase implements
        InitiateTransferInputPort,
        CompleteTransferInputPort,
        CancelTransferInputPort,
        GetTransferInputPort {

    private final StockTransferService service;

    @Override
    public StockTransferResult execute(InitiateTransferCommand command) {
        return service.initiate(command);
    }

    @Override
    public StockTransferResult execute(CompleteTransferCommand command) {
        return service.complete(command);
    }

    @Override
    public StockTransferResult execute(CancelTransferCommand command) {
        return service.cancel(command);
    }

    @Override
    public StockTransferResult execute(String transferId) {
        return service.get(transferId);
    }
}
