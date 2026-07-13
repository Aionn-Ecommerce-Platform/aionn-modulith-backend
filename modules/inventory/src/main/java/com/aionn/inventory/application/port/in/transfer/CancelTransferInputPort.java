package com.aionn.inventory.application.port.in.transfer;

import com.aionn.inventory.application.dto.transfer.command.CancelTransferCommand;
import com.aionn.inventory.application.dto.transfer.result.StockTransferResult;

public interface CancelTransferInputPort {
    StockTransferResult execute(CancelTransferCommand command);
}
