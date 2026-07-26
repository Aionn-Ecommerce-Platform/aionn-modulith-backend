package com.aionn.shipping.adapter.rest.controller;

import com.aionn.shipping.adapter.rest.dto.rate.ConfigureRateRequest;
import com.aionn.shipping.adapter.rest.dto.rate.UpdateRateRequest;
import com.aionn.shipping.adapter.rest.dto.rate.response.ShippingRateResponse;
import com.aionn.shipping.adapter.rest.mapper.rate.ShippingRateDtoMapper;
import com.aionn.shipping.application.dto.rate.command.ConfigureRateCommand;
import com.aionn.shipping.application.dto.rate.command.UpdateRateCommand;
import com.aionn.shipping.application.dto.rate.result.ShippingRateResult;
import com.aionn.shipping.application.port.in.rate.ConfigureRateInputPort;
import com.aionn.shipping.application.port.in.rate.UpdateRateInputPort;
import com.aionn.shipping.application.port.in.rate.GetRateInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shipping/rates")
@RequiredArgsConstructor
@Tag(name = "Shipping - Rate", description = "System Admin shipping rate configuration")
public class ShippingRateController {

    private final ConfigureRateInputPort configureRateInputPort;
    private final UpdateRateInputPort updateRateInputPort;
    private final GetRateInputPort getRateInputPort;
    private final ShippingRateDtoMapper shippingRateDtoMapper;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Configure rate")
    public ResponseEntity<ApiResponse<ShippingRateResponse>> configure(
            @Valid @RequestBody ConfigureRateRequest request) {
        ConfigureRateCommand command = shippingRateDtoMapper.toCommand(request);
        ShippingRateResult result = configureRateInputPort.execute(command);
        return ApiResponse.createdResponse("Shipping rate configured", shippingRateDtoMapper.toResponse(result));
    }

    @PutMapping("/{rateId}")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Update rate")
    public ResponseEntity<ApiResponse<ShippingRateResponse>> update(
            @PathVariable String rateId,
            @Valid @RequestBody UpdateRateRequest request) {
        UpdateRateCommand command = shippingRateDtoMapper.toCommand(request, rateId);
        ShippingRateResult result = updateRateInputPort.execute(command);
        return ResponseEntity.ok(ApiResponse.success(shippingRateDtoMapper.toResponse(result), "Shipping rate updated"));
    }

    @GetMapping("/{rateId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get rate")
    public ResponseEntity<ApiResponse<ShippingRateResponse>> get(@PathVariable String rateId) {
        ShippingRateResult result = getRateInputPort.execute(rateId);
        return ResponseEntity.ok(ApiResponse.success(shippingRateDtoMapper.toResponse(result), "Rate fetched"));
    }
}
