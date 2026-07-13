package com.aionn.inventory.adapter.rest.mapper.reservation;

import com.aionn.inventory.adapter.rest.dto.reservation.ReleaseReservationRequest;
import com.aionn.inventory.adapter.rest.dto.reservation.ReserveStockRequest;
import com.aionn.inventory.application.dto.reservation.command.ReleaseReservationCommand;
import com.aionn.inventory.application.dto.reservation.command.ReserveStockCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StockReservationDtoMapper {

    ReserveStockCommand toReserveStockCommand(ReserveStockRequest request);

    ReleaseReservationCommand toReleaseReservationCommand(String reservationId, ReleaseReservationRequest request);
}
