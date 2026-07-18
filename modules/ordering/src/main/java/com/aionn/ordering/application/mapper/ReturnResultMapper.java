package com.aionn.ordering.application.mapper;

import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.domain.model.OrderReturn;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReturnResultMapper {

    @Mapping(target = "refundAmount", expression = "java(orderReturn.getRefundAmount() == null ? null : orderReturn.getRefundAmount().amount())")
    @Mapping(target = "currency", expression = "java(orderReturn.getRefundAmount() == null ? null : orderReturn.getRefundAmount().currency())")
    @Mapping(target = "status", expression = "java(orderReturn.getStatus().name())")
    ReturnResult toResult(OrderReturn orderReturn);
}
