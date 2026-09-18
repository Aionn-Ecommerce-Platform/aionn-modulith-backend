package com.aionn.ucp.adapter.rest.controller;

import com.aionn.ucp.adapter.rest.dto.cart.UcpCartRequest;
import com.aionn.ucp.adapter.rest.dto.cart.UcpCartResponse;
import com.aionn.ucp.application.cart.UcpCartApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/ucp/v1/carts", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("isAuthenticated()")
public class UcpCartController {

    private final UcpCartApplicationService cartService;

    public UcpCartController(UcpCartApplicationService cartService) {
        this.cartService = cartService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UcpCartResponse createCart(
            @Valid @RequestBody UcpCartRequest request,
            HttpServletRequest servletRequest) {
        String userId = resolveUserId(servletRequest);
        return cartService.createCart(request, userId);
    }

    @GetMapping("/{id}")
    public UcpCartResponse getCart(
            @PathVariable("id") String id,
            HttpServletRequest servletRequest) {
        String userId = resolveUserId(servletRequest);
        return cartService.getCart(id, userId);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UcpCartResponse updateCart(
            @PathVariable("id") String id,
            @Valid @RequestBody UcpCartRequest request,
            HttpServletRequest servletRequest) {
        String userId = resolveUserId(servletRequest);
        return cartService.updateCart(id, request, userId);
    }

    @PostMapping("/{id}/cancel")
    public UcpCartResponse cancelCart(
            @PathVariable("id") String id,
            HttpServletRequest servletRequest) {
        String userId = resolveUserId(servletRequest);
        return cartService.cancelCart(id, userId);
    }

    private String resolveUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Principal principal = request.getUserPrincipal();
        return principal != null ? principal.getName() : null;
    }
}
