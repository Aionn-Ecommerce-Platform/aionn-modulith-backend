package com.aionn.ucp.adapter.rest.controller;

import com.aionn.sharedkernel.adapter.web.support.idempotency.IdempotentRequest;
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

/** UCP Identity Linking endpoints (dev.ucp.common.identity_linking), keyed by platformId/platformSubject. */
@RestController
@RequestMapping("/ucp/v1/identity/links")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class UcpIdentityController {

    private final UcpIdentityApplicationService identityService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @IdempotentRequest(ttlSeconds = 3600)
    public ResponseEntity<UcpIdentityLinkResponse> linkIdentity(
            @Valid @RequestBody UcpIdentityLinkRequest request,
            Authentication authentication) {
        String userId = getAuthenticatedUserId(authentication);
        UcpIdentityLinkResponse response = identityService.linkIdentity(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(value = "/{platformId}/{platformSubject}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UcpIdentityLinkResponse> getLink(
            @PathVariable("platformId") String platformId,
            @PathVariable("platformSubject") String platformSubject,
            Authentication authentication) {
        String userId = getAuthenticatedUserId(authentication);
        UcpIdentityLinkResponse response = identityService.getLink(platformId, platformSubject, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{platformId}/{platformSubject}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @IdempotentRequest(ttlSeconds = 3600)
    public void revokeLink(
            @PathVariable("platformId") String platformId,
            @PathVariable("platformSubject") String platformSubject,
            Authentication authentication) {
        String userId = getAuthenticatedUserId(authentication);
        identityService.revokeLink(platformId, platformSubject, userId);
    }

    private String getAuthenticatedUserId(Authentication authentication) {
        // Anonymous and platform-principal callers have no buyer identity here.
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        String name = authentication.getName();
        if (name != null && name.startsWith("ucp:platform:")) {
            return null;
        }
        return name;
    }
}
