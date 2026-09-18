package com.aionn.ucp.adapter.rest.controller;

import com.aionn.ucp.adapter.rest.dto.checkout.UcpCheckoutRequest;
import com.aionn.ucp.adapter.rest.dto.checkout.UcpCheckoutResponse;
import com.aionn.ucp.application.checkout.UcpCheckoutApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing canonical UCP Checkout REST endpoints.
 * Handles create, read, update, complete, and cancel operations for checkout sessions.
 */
@RestController
@RequestMapping("/ucp/v1/checkout-sessions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UcpCheckoutController {

    private final UcpCheckoutApplicationService checkoutService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UcpCheckoutResponse> createCheckout(
            @Valid @RequestBody(required = false) UcpCheckoutRequest request,
            Authentication authentication) {
        String userId = getAuthenticatedUserId(authentication);
        UcpCheckoutResponse response = checkoutService.createCheckout(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UcpCheckoutResponse> getCheckout(
            @PathVariable("id") String checkoutId,
            Authentication authentication) {
        String userId = getAuthenticatedUserId(authentication);
        UcpCheckoutResponse response = checkoutService.getCheckout(checkoutId, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UcpCheckoutResponse> updateCheckout(
            @PathVariable("id") String checkoutId,
            @Valid @RequestBody UcpCheckoutRequest request,
            Authentication authentication) {
        String userId = getAuthenticatedUserId(authentication);
        UcpCheckoutResponse response = checkoutService.updateCheckout(checkoutId, request, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{id}/complete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UcpCheckoutResponse> completeCheckout(
            @PathVariable("id") String checkoutId,
            Authentication authentication) {
        String userId = getAuthenticatedUserId(authentication);
        UcpCheckoutResponse response = checkoutService.completeCheckout(checkoutId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{id}/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UcpCheckoutResponse> cancelCheckout(
            @PathVariable("id") String checkoutId,
            Authentication authentication) {
        String userId = getAuthenticatedUserId(authentication);
        UcpCheckoutResponse response = checkoutService.cancelCheckout(checkoutId, userId);
        return ResponseEntity.ok(response);
    }

    private String getAuthenticatedUserId(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return null;
    }
}
