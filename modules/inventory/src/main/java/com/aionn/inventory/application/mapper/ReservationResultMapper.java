package com.aionn.inventory.application.mapper;

import com.aionn.inventory.application.dto.reservation.result.ReservationResult;
import com.aionn.inventory.domain.model.StockReservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReservationResultMapper {

    @Mapping(target = "status", expression = "java(reservation.getStatus().name())")
    ReservationResult toResult(StockReservation reservation);
}
