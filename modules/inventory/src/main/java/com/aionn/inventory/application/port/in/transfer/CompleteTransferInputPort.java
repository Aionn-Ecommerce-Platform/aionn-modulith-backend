package com.aionn.inventory.application.port.in.transfer;

import com.aionn.inventory.application.dto.transfer.command.CompleteTransferCommand;
import com.aionn.inventory.application.dto.transfer.result.StockTransferResult;

public interface CompleteTransferInputPort {
    StockTransferResult execute(CompleteTransferCommand command);
}
