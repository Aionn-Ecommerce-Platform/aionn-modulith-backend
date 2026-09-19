package com.aionn.ucp.adapter.rest.controller;

import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkRequest;
import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkResponse;
import com.aionn.ucp.application.identity.UcpIdentityApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing canonical UCP Identity Linking endpoints
 * (dev.ucp.common.identity_linking).
 * Allows platforms/agents to establish, query, and revoke identity bindings
 * using composite keys (platformId / platformSubject).
 */
@RestController
@RequestMapping("/ucp/v1/identity/links")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class UcpIdentityController {

    private final UcpIdentityApplicationService identityService;

    /**
     * Creates or updates an identity link between an external platform subject and
     * an Aionn merchant customer.
     *
     * @param request        link request details
     * @param authentication caller's security context
     * @return 201 Created with the active identity link details
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UcpIdentityLinkResponse> linkIdentity(
            @Valid @RequestBody UcpIdentityLinkRequest request,
            Authentication authentication) {
        String userId = getAuthenticatedUserId(authentication);
        UcpIdentityLinkResponse response = identityService.linkIdentity(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Queries an identity link by platform identifier and platform subject
     * identifier.
     *
     * @param platformId      platform ID
     * @param platformSubject platform subject ID
     * @param authentication  caller's security context
     * @return 200 OK with identity link details
     */
    @GetMapping(value = "/{platformId}/{platformSubject}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UcpIdentityLinkResponse> getLink(
            @PathVariable("platformId") String platformId,
            @PathVariable("platformSubject") String platformSubject,
            Authentication authentication) {
        String userId = getAuthenticatedUserId(authentication);
        UcpIdentityLinkResponse response = identityService.getLink(platformId, platformSubject, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Revokes an existing identity link.
     *
     * @param platformId      platform ID
     * @param platformSubject platform subject ID
     * @param authentication  caller's security context
     */
    @DeleteMapping("/{platformId}/{platformSubject}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeLink(
            @PathVariable("platformId") String platformId,
            @PathVariable("platformSubject") String platformSubject,
            Authentication authentication) {
        String userId = getAuthenticatedUserId(authentication);
        identityService.revokeLink(platformId, platformSubject, userId);
    }

    private String getAuthenticatedUserId(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return null;
    }
}
