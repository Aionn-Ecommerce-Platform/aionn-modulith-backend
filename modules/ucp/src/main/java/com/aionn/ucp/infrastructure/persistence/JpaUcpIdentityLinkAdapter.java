package com.aionn.ucp.infrastructure.persistence;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkRequest;
import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkResponse;
import com.aionn.ucp.application.port.out.UcpIdentityLinkPort;
import com.aionn.ucp.infrastructure.persistence.entity.UcpIdentityLinkEntity;
import com.aionn.ucp.infrastructure.persistence.repository.UcpIdentityLinkRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/** JPA-backed {@link UcpIdentityLinkPort}; links are a trust boundary, so they must be durable. */
@Component
@Primary
public class JpaUcpIdentityLinkAdapter implements UcpIdentityLinkPort {

    private final UcpIdentityLinkRepository repository;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public JpaUcpIdentityLinkAdapter(UcpIdentityLinkRepository repository, Clock clock,
            PlatformTransactionManager txManager) {
        this.repository = repository;
        this.clock = clock != null ? clock : Clock.systemUTC();
        this.transactionTemplate = new TransactionTemplate(txManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Upsert in its own committed transaction. If a concurrent insert wins the gap, the unique
     * constraint rolls this attempt back and the retry sees the winner's row; retrying in the same
     * transaction would fail as rollback-only.
     */
    @Override
    public UcpIdentityLinkResponse saveLink(UcpIdentityLinkRequest request) {
        try {
            return transactionTemplate.execute(status -> doUpsert(request));
        } catch (DataIntegrityViolationException conflict) {
            return transactionTemplate.execute(status -> doUpsert(request));
        }
    }

    private UcpIdentityLinkResponse doUpsert(UcpIdentityLinkRequest request) {
        UcpIdentityLinkEntity entity = repository
                .findByPlatformIdAndPlatformSubject(request.platformId(), request.platformSubject())
                .map(existing -> {
                    // Re-save keeps the stable linkId and linkedAt; status/metadata/scopes refresh.
                    existing.setCustomerId(request.customerId());
                    existing.setStatus("ACTIVE");
                    existing.setScopes(request.scopes() != null ? request.scopes() : Collections.emptyList());
                    existing.setMetadata(request.metadata() != null ? request.metadata() : Collections.emptyMap());
                    existing.setExpiresAt(null);
                    return existing;
                })
                .orElseGet(() -> UcpIdentityLinkEntity.builder()
                        .linkId("idlink_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                        .platformId(request.platformId())
                        .platformSubject(request.platformSubject())
                        .customerId(request.customerId())
                        .status("ACTIVE")
                        .scopes(request.scopes() != null ? request.scopes() : Collections.emptyList())
                        .linkedAt(clock.instant())
                        .expiresAt(null)
                        .metadata(request.metadata() != null ? request.metadata() : Collections.emptyMap())
                        .build());
        return toResponse(repository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UcpIdentityLinkResponse> findByPlatformAndSubject(String platformId, String platformSubject) {
        if (platformId == null || platformSubject == null) {
            return Optional.empty();
        }
        return repository.findByPlatformIdAndPlatformSubject(platformId, platformSubject)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UcpIdentityLinkResponse> findByCustomerId(String customerId) {
        if (customerId == null) {
            return Optional.empty();
        }
        return repository.findFirstByCustomerId(customerId).map(this::toResponse);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean revokeLink(String platformId, String platformSubject) {
        if (platformId == null || platformSubject == null) {
            return false;
        }
        return repository.revokeByPlatformAndSubject(platformId, platformSubject, clock.instant()) > 0;
    }

    private UcpIdentityLinkResponse toResponse(UcpIdentityLinkEntity e) {
        return new UcpIdentityLinkResponse(
                e.getLinkId(),
                e.getPlatformId(),
                e.getPlatformSubject(),
                e.getCustomerId(),
                e.getStatus(),
                e.getScopes() != null ? e.getScopes() : Collections.emptyList(),
                e.getLinkedAt(),
                e.getExpiresAt(),
                e.getMetadata() != null ? e.getMetadata() : Collections.emptyMap());
    }
}
