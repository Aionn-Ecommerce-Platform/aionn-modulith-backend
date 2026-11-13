package com.aionn.promotion.application.mapper;

import com.aionn.promotion.application.dto.flashsale.result.FlashSaleRegistrationResult;
import com.aionn.promotion.domain.model.FlashSaleRegistration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FlashSaleResultMapper {

    @Mapping(target = "salePrice", source = "salePrice.amount")
    @Mapping(target = "currency", source = "salePrice.currency")
    FlashSaleRegistrationResult toResult(FlashSaleRegistration registration);

    List<FlashSaleRegistrationResult> toResults(List<FlashSaleRegistration> registrations);
}
