package com.aionn.inventory.adapter.rest.mapper.reservation;

import com.aionn.inventory.adapter.rest.dto.reservation.request.ReleaseReservationRequest;
import com.aionn.inventory.adapter.rest.dto.reservation.request.ReserveStockRequest;
import com.aionn.inventory.adapter.rest.dto.reservation.response.ReservationResponse;
import com.aionn.inventory.application.dto.reservation.command.ReleaseReservationCommand;
import com.aionn.inventory.application.dto.reservation.command.ReserveStockCommand;
import com.aionn.inventory.application.dto.reservation.result.ReservationResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StockReservationDtoMapper {

    ReserveStockCommand toReserveStockCommand(ReserveStockRequest request);

    ReleaseReservationCommand toReleaseReservationCommand(String reservationId, ReleaseReservationRequest request);

    ReservationResponse toResponse(ReservationResult result);
}
