package com.aionn.ucp.infrastructure.adapter;

import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkRequest;
import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkResponse;
import com.aionn.ucp.application.port.out.UcpIdentityLinkPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory thread-safe adapter implementation for UcpIdentityLinkPort.
 */
@Component
public class InMemoryUcpIdentityLinkAdapter implements UcpIdentityLinkPort {

    private final Map<String, UcpIdentityLinkResponse> linksBySubject = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public InMemoryUcpIdentityLinkAdapter(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    public InMemoryUcpIdentityLinkAdapter() {
        this(Clock.systemUTC());
    }

    @Override
    public UcpIdentityLinkResponse saveLink(UcpIdentityLinkRequest request) {
        String linkId = "idlink_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        UcpIdentityLinkResponse response = new UcpIdentityLinkResponse(
                linkId,
                request.platformId(),
                request.platformSubject(),
                request.customerId(),
                "ACTIVE",
                request.scopes() != null ? request.scopes() : Collections.emptyList(),
                clock.instant(),
                null,
                request.metadata() != null ? request.metadata() : Collections.emptyMap());
        linksBySubject.put(request.platformSubject(), response);
        return response;
    }

    @Override
    public Optional<UcpIdentityLinkResponse> findByPlatformSubject(String platformSubject) {
        if (platformSubject == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(linksBySubject.get(platformSubject));
    }

    @Override
    public Optional<UcpIdentityLinkResponse> findByCustomerId(String customerId) {
        if (customerId == null) {
            return Optional.empty();
        }
        return linksBySubject.values().stream()
                .filter(link -> customerId.equals(link.customerId()))
                .findFirst();
    }

    @Override
    public boolean revokeLink(String platformSubject) {
        if (platformSubject == null) {
            return false;
        }
        UcpIdentityLinkResponse existing = linksBySubject.get(platformSubject);
        if (existing == null) {
            return false;
        }
        UcpIdentityLinkResponse revoked = new UcpIdentityLinkResponse(
                existing.linkId(),
                existing.platformId(),
                existing.platformSubject(),
                existing.customerId(),
                "REVOKED",
                existing.scopes(),
                existing.linkedAt(),
                clock.instant(),
                existing.metadata());
        linksBySubject.put(platformSubject, revoked);
        return true;
    }
}
