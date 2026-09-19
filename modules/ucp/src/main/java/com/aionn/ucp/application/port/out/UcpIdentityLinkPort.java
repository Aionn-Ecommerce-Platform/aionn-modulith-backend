package com.aionn.ucp.application.port.out;

import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkRequest;
import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkResponse;

import java.util.Optional;

/**
 * Outbound port for persisting and querying UCP identity links.
 */
public interface UcpIdentityLinkPort {

    /**
     * Persists or updates an identity link record.
     *
     * @param request the identity link request
     * @return the resulting link response
     */
    UcpIdentityLinkResponse saveLink(UcpIdentityLinkRequest request);

    /**
     * Finds an identity link by platform identifier and platform subject.
     *
     * @param platformId      the platform identifier
     * @param platformSubject the platform user identifier
     * @return optional link response if present
     */
    Optional<UcpIdentityLinkResponse> findByPlatformAndSubject(String platformId, String platformSubject);

    /**
     * Finds an identity link by customer identifier.
     *
     * @param customerId the Aionn customer identifier
     * @return optional link response if present
     */
    Optional<UcpIdentityLinkResponse> findByCustomerId(String customerId);

    /**
     * Revokes an existing link for the given platform and subject.
     *
     * @param platformId      the platform identifier
     * @param platformSubject the platform user identifier
     * @return true if a link was found and revoked, false otherwise
     */
    boolean revokeLink(String platformId, String platformSubject);
}
