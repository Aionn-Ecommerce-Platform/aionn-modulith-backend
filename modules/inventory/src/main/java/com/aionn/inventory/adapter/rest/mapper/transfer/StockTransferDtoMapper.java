package com.aionn.inventory.adapter.rest.mapper.transfer;

import com.aionn.inventory.adapter.rest.dto.transfer.request.CancelTransferRequest;
import com.aionn.inventory.adapter.rest.dto.transfer.request.CompleteTransferRequest;
import com.aionn.inventory.adapter.rest.dto.transfer.request.InitiateTransferRequest;
import com.aionn.inventory.adapter.rest.dto.transfer.response.StockTransferResponse;
import com.aionn.inventory.application.dto.transfer.command.CancelTransferCommand;
import com.aionn.inventory.application.dto.transfer.command.CompleteTransferCommand;
import com.aionn.inventory.application.dto.transfer.command.InitiateTransferCommand;
import com.aionn.inventory.application.dto.transfer.result.StockTransferResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StockTransferDtoMapper {

    InitiateTransferCommand toInitiateTransferCommand(String ownerId, InitiateTransferRequest request);

    CompleteTransferCommand toCompleteTransferCommand(String ownerId, String transferId, CompleteTransferRequest request);

    CancelTransferCommand toCancelTransferCommand(String ownerId, String transferId, CancelTransferRequest request);

    StockTransferResponse toResponse(StockTransferResult result);
}
