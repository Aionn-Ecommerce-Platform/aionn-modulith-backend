package com.aionn.ucp.application.identity;

import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkRequest;
import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkResponse;
import com.aionn.ucp.application.port.out.UcpIdentityLinkPort;
import com.aionn.ucp.domain.exception.UcpProtocolException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Application service managing UCP identity links with fail-closed IDOR
 * security.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UcpIdentityApplicationService {

        private final UcpIdentityLinkPort identityLinkPort;

        /**
         * Creates or updates an identity link. Verifies that the authenticated caller
         * matches the customerId,
         * and ensures an existing platform binding cannot be hijacked by a different
         * customer.
         *
         * @param request      the identity link request
         * @param callerUserId authenticated caller user id
         * @return created identity link response
         */
        public UcpIdentityLinkResponse linkIdentity(UcpIdentityLinkRequest request, String callerUserId) {
                if (callerUserId == null || !callerUserId.equals(request.customerId())) {
                        log.warn("Access denied: Caller '{}' cannot link identity for customer '{}'", callerUserId,
                                        request.customerId());
                        throw new UcpProtocolException(403, "access_denied",
                                        "Caller is not authorized to link identity for customer: "
                                                        + request.customerId(),
                                        "error");
                }

                Optional<UcpIdentityLinkResponse> existing = identityLinkPort.findByPlatformAndSubject(
                                request.platformId(), request.platformSubject());
                if (existing.isPresent() && !existing.get().customerId().equals(request.customerId())) {
                        log.warn("Conflict: Platform subject '{}::{}' is already bound to another customer",
                                        request.platformId(), request.platformSubject());
                        throw new UcpProtocolException(409, "identity_conflict",
                                        "Platform identity is already bound to another customer account", "error");
                }

                log.info("Linking platform '{}' subject '{}' to customer '{}'",
                                request.platformId(), request.platformSubject(), request.customerId());
                return identityLinkPort.saveLink(request);
        }

        /**
         * Retrieves an identity link for the given platform and subject.
         * Maps unauthorized caller access to 404 to avoid leaking subject existence.
         *
         * @param platformId      the platform identifier
         * @param platformSubject the platform subject identifier
         * @param callerUserId    authenticated caller user id
         * @return link details
         */
        public UcpIdentityLinkResponse getLink(String platformId, String platformSubject, String callerUserId) {
                UcpIdentityLinkResponse link = identityLinkPort.findByPlatformAndSubject(platformId, platformSubject)
                                .filter(l -> callerUserId != null && callerUserId.equals(l.customerId()))
                                .orElseThrow(() -> new UcpProtocolException(404, "link_not_found",
                                                "Identity link not found for platform: " + platformId + ", subject: "
                                                                + platformSubject,
                                                "error"));

                return link;
        }

        /**
         * Revokes an existing identity link.
         *
         * @param platformId      the platform identifier
         * @param platformSubject the platform subject identifier
         * @param callerUserId    authenticated caller user id
         */
        public void revokeLink(String platformId, String platformSubject, String callerUserId) {
                UcpIdentityLinkResponse link = identityLinkPort.findByPlatformAndSubject(platformId, platformSubject)
                                .filter(l -> callerUserId != null && callerUserId.equals(l.customerId()))
                                .orElseThrow(() -> new UcpProtocolException(404, "link_not_found",
                                                "Identity link not found for platform: " + platformId + ", subject: "
                                                                + platformSubject,
                                                "error"));

                identityLinkPort.revokeLink(platformId, platformSubject);
                log.info("Successfully revoked identity link for platform '{}' subject '{}'", platformId,
                                platformSubject);
        }
}
