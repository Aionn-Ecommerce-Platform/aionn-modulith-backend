package com.aionn.inventory.application.port.in.transfer;

import com.aionn.inventory.application.dto.transfer.command.InitiateTransferCommand;
import com.aionn.inventory.application.dto.transfer.result.StockTransferResult;

public interface InitiateTransferInputPort {
    StockTransferResult execute(InitiateTransferCommand command);
}
