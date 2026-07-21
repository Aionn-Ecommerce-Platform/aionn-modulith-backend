package com.aionn.payment.application.mapper;

import com.aionn.payment.application.dto.method.result.PaymentMethodResult;
import com.aionn.payment.domain.model.PaymentMethod;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMethodResultMapper {

    @Mapping(target = "status", expression = "java(method.getStatus().name())")
    PaymentMethodResult toResult(PaymentMethod method);
}
