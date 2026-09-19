package com.aionn.ucp.infrastructure.adapter;

import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkRequest;
import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkResponse;
import com.aionn.ucp.application.port.out.UcpIdentityLinkPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-memory thread-safe adapter implementation for UcpIdentityLinkPort.
 * Keys identity links by a collision-free structured key (platformId,
 * platformSubject).
 */
@Component
public class InMemoryUcpIdentityLinkAdapter implements UcpIdentityLinkPort {

    private record PlatformSubjectKey(String platformId, String platformSubject) {
        PlatformSubjectKey {
            platformId = platformId != null ? platformId : "";
            platformSubject = platformSubject != null ? platformSubject : "";
        }
    }

    private final Map<PlatformSubjectKey, UcpIdentityLinkResponse> linksByKey = new ConcurrentHashMap<>();
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
        PlatformSubjectKey key = new PlatformSubjectKey(request.platformId(), request.platformSubject());
        return linksByKey.compute(key, (k, existing) -> {
            String linkId = existing != null ? existing.linkId()
                    : "idlink_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            return new UcpIdentityLinkResponse(
                    linkId,
                    request.platformId(),
                    request.platformSubject(),
                    request.customerId(),
                    "ACTIVE",
                    request.scopes() != null ? request.scopes() : Collections.emptyList(),
                    existing != null ? existing.linkedAt() : clock.instant(),
                    null,
                    request.metadata() != null ? request.metadata() : Collections.emptyMap());
        });
    }

    @Override
    public Optional<UcpIdentityLinkResponse> findByPlatformAndSubject(String platformId, String platformSubject) {
        if (platformId == null || platformSubject == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(linksByKey.get(new PlatformSubjectKey(platformId, platformSubject)));
    }

    @Override
    public Optional<UcpIdentityLinkResponse> findByCustomerId(String customerId) {
        if (customerId == null) {
            return Optional.empty();
        }
        return linksByKey.values().stream()
                .filter(link -> customerId.equals(link.customerId()))
                .findFirst();
    }

    @Override
    public boolean revokeLink(String platformId, String platformSubject) {
        if (platformId == null || platformSubject == null) {
            return false;
        }
        PlatformSubjectKey key = new PlatformSubjectKey(platformId, platformSubject);
        AtomicBoolean wasRevoked = new AtomicBoolean(false);
        linksByKey.computeIfPresent(key, (k, existing) -> {
            wasRevoked.set(true);
            return new UcpIdentityLinkResponse(
                    existing.linkId(),
                    existing.platformId(),
                    existing.platformSubject(),
                    existing.customerId(),
                    "REVOKED",
                    existing.scopes(),
                    existing.linkedAt(),
                    clock.instant(),
                    existing.metadata());
        });
        return wasRevoked.get();
    }
}
