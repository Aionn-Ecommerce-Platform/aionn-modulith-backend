package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.adapter.rest.dto.analytics.response.AdminPaymentAnalyticsResponse;
import com.aionn.payment.adapter.rest.mapper.analytics.PaymentAnalyticsDtoMapper;
import com.aionn.payment.application.dto.analytics.query.GetAdminPaymentAnalyticsQuery;
import com.aionn.payment.application.port.in.analytics.GetAdminPaymentAnalyticsInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/admin/analytics")
@RequiredArgsConstructor
@Tag(name = "Payment - Admin Analytics", description = "Platform payment and payout analytics")
public class AdminPaymentAnalyticsController {
    private final GetAdminPaymentAnalyticsInputPort analyticsInputPort;
    private final PaymentAnalyticsDtoMapper dtoMapper;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Payment and payout analytics for administrators")
    public ResponseEntity<ApiResponse<AdminPaymentAnalyticsResponse>> get(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "VND") String currency) {
        var result = analyticsInputPort.execute(new GetAdminPaymentAnalyticsQuery(from, to, currency));
        return ResponseEntity.ok(ApiResponse.success(dtoMapper.toResponse(result), "Payment analytics fetched"));
    }
}
