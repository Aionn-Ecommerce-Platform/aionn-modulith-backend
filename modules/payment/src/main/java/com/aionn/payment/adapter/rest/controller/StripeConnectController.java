package com.aionn.payment.adapter.rest.controller;

import com.aionn.payment.adapter.rest.support.session.CurrentUserId;
import com.aionn.payment.application.port.in.stripeconnect.CreateStripeConnectOnboardingLinkInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/stripe-connect")
@RequiredArgsConstructor
@Tag(name = "Payment - Stripe Connect", description = "Merchant onboarding to Stripe Connect")
public class StripeConnectController {

    private final CreateStripeConnectOnboardingLinkInputPort createStripeConnectOnboardingLinkInputPort;

    @PostMapping("/onboarding-link")
    @PreAuthorize("hasAuthority('ROLE_MERCHANT')")
    @Operation(summary = "Get Stripe Connect onboarding URL")
    public ResponseEntity<ApiResponse<Map<String, String>>> onboardingLink(@CurrentUserId String ownerId) {
        String url = createStripeConnectOnboardingLinkInputPort.execute(ownerId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url),
                "Stripe Connect onboarding link"));
    }
}
