package com.aionn.ucp.adapter.rest.controller;

import com.aionn.platform.web.support.idempotency.IdempotentRequest;
import com.aionn.ucp.adapter.rest.filter.UcpAgentAuthenticationFilter;
import com.aionn.ucp.application.UcpProtocolConstants;
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
    @IdempotentRequest(ttlSeconds = UcpProtocolConstants.IDEMPOTENCY_TTL_SECONDS, required = true)
    public ResponseEntity<UcpIdentityLinkResponse> linkIdentity(
            @Valid @RequestBody UcpIdentityLinkRequest request,
            Authentication authentication) {
        Caller caller = resolveCaller(authentication);
        UcpIdentityLinkResponse response = caller.platformId() != null
                ? identityService.linkIdentity(request, null, caller.platformId())
                : identityService.linkIdentity(request, caller.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(value = "/{platformId}/{platformSubject}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UcpIdentityLinkResponse> getLink(
            @PathVariable("platformId") String platformId,
            @PathVariable("platformSubject") String platformSubject,
            Authentication authentication) {
        Caller caller = resolveCaller(authentication);
        UcpIdentityLinkResponse response = caller.platformId() != null
                ? identityService.getLink(platformId, platformSubject, null, caller.platformId())
                : identityService.getLink(platformId, platformSubject, caller.userId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{platformId}/{platformSubject}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @IdempotentRequest(ttlSeconds = UcpProtocolConstants.IDEMPOTENCY_TTL_SECONDS, required = true)
    public void revokeLink(
            @PathVariable("platformId") String platformId,
            @PathVariable("platformSubject") String platformSubject,
            Authentication authentication) {
        Caller caller = resolveCaller(authentication);
        if (caller.platformId() != null) {
            identityService.revokeLink(platformId, platformSubject, null, caller.platformId());
        } else {
            identityService.revokeLink(platformId, platformSubject, caller.userId());
        }
    }

    private Caller resolveCaller(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return new Caller(null, null);
        }
        String name = authentication.getName();
        if (name == null) {
            return new Caller(null, null);
        }
        if (name.startsWith(UcpAgentAuthenticationFilter.PLATFORM_PRINCIPAL_PREFIX)) {
            return new Caller(null, name.substring(UcpAgentAuthenticationFilter.PLATFORM_PRINCIPAL_PREFIX.length()));
        }
        return new Caller(name, null);
    }

    private record Caller(String userId, String platformId) {
    }
}
