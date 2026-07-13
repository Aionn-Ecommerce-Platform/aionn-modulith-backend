package com.aionn.inventory.application.port.in.transfer;

import com.aionn.inventory.application.dto.transfer.result.StockTransferResult;

public interface GetTransferInputPort {
    StockTransferResult execute(String transferId);
}
