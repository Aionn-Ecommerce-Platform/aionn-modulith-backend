package com.aionn.ucp.adapter.rest.controller;

import com.aionn.ucp.adapter.rest.dto.order.UcpOrderModels.UcpOrderResponse;
import com.aionn.ucp.application.order.UcpOrderApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** UCP Order REST endpoint (dev.ucp.shopping.order), release 2026-08-25. */
@RestController
@RequestMapping("/ucp/v1/orders")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class UcpOrderController {

    private final UcpOrderApplicationService orderService;

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UcpOrderResponse> getOrder(
            @PathVariable("id") String orderId,
            Authentication authentication) {
        String userId = getAuthenticatedUserId(authentication);
        UcpOrderResponse response = orderService.getOrder(orderId, userId);
        return ResponseEntity.ok(response);
    }

    private String getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        String name = authentication.getName();
        // Platform credentials own guest orders directly; buyer JWTs carry the buyer id.
        return name != null && !name.isBlank() ? name : null;
    }
}
