package com.aionn.shipping.adapter.rest.mapper.rate;

import com.aionn.shipping.adapter.rest.dto.rate.ConfigureRateRequest;
import com.aionn.shipping.adapter.rest.dto.rate.UpdateRateRequest;
import com.aionn.shipping.adapter.rest.dto.shipment.QuoteShippingRequest;
import com.aionn.shipping.adapter.rest.dto.rate.response.ShippingQuoteResponse;
import com.aionn.shipping.adapter.rest.dto.rate.response.ShippingRateResponse;
import com.aionn.shipping.application.dto.rate.command.ConfigureRateCommand;
import com.aionn.shipping.application.dto.rate.command.UpdateRateCommand;
import com.aionn.shipping.application.dto.shipment.command.QuoteShippingCommand;
import com.aionn.shipping.application.dto.rate.result.ShippingQuoteResult;
import com.aionn.shipping.application.dto.rate.result.ShippingRateResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ShippingRateDtoMapper {
    ShippingQuoteResponse toResponse(ShippingQuoteResult result);
    ShippingRateResponse toResponse(ShippingRateResult result);

    QuoteShippingCommand toCommand(QuoteShippingRequest request);
    ConfigureRateCommand toCommand(ConfigureRateRequest request);
    UpdateRateCommand toCommand(UpdateRateRequest request, String rateId);
}
