package com.aionn.ordering.adapter.rest.mapper;

import com.aionn.ordering.adapter.rest.dto.response.CartResponse;
import com.aionn.ordering.adapter.rest.dto.response.OrderResponse;
import com.aionn.ordering.adapter.rest.dto.response.OrderReturnResponse;
import com.aionn.ordering.application.dto.cart.result.CartResult;
import com.aionn.ordering.application.dto.order.result.OrderResult;
import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderingDtoMapper {

    CartResponse toResponse(CartResult result);

    OrderResponse toResponse(OrderResult result);

    OrderReturnResponse toResponse(ReturnResult result);
}
