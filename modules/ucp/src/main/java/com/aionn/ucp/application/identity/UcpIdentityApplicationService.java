package com.aionn.ucp.application.identity;

import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkRequest;
import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkResponse;
import com.aionn.ucp.application.port.out.UcpIdentityLinkPort;
import com.aionn.ucp.domain.exception.UcpProtocolException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
         * matches the customerId.
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

                log.info("Linking platform '{}' subject '{}' to customer '{}'",
                                request.platformId(), request.platformSubject(), request.customerId());
                return identityLinkPort.saveLink(request);
        }

        /**
         * Retrieves an identity link for the given platform subject.
         *
         * @param platformSubject the platform subject identifier
         * @param callerUserId    authenticated caller user id
         * @return link details
         */
        public UcpIdentityLinkResponse getLink(String platformSubject, String callerUserId) {
                UcpIdentityLinkResponse link = identityLinkPort.findByPlatformSubject(platformSubject)
                                .orElseThrow(() -> new UcpProtocolException(404, "link_not_found",
                                                "Identity link not found for platform subject: " + platformSubject,
                                                "error"));

                if (callerUserId == null || !callerUserId.equals(link.customerId())) {
                        log.warn("Access denied: Caller '{}' cannot view link for customer '{}'", callerUserId,
                                        link.customerId());
                        throw new UcpProtocolException(403, "access_denied",
                                        "Caller is not authorized to access this identity link", "error");
                }

                return link;
        }

        /**
         * Revokes an existing identity link.
         *
         * @param platformSubject the platform subject identifier
         * @param callerUserId    authenticated caller user id
         */
        public void revokeLink(String platformSubject, String callerUserId) {
                UcpIdentityLinkResponse link = identityLinkPort.findByPlatformSubject(platformSubject)
                                .orElseThrow(() -> new UcpProtocolException(404, "link_not_found",
                                                "Identity link not found for platform subject: " + platformSubject,
                                                "error"));

                if (callerUserId == null || !callerUserId.equals(link.customerId())) {
                        log.warn("Access denied: Caller '{}' cannot revoke link for customer '{}'", callerUserId,
                                        link.customerId());
                        throw new UcpProtocolException(403, "access_denied",
                                        "Caller is not authorized to revoke this identity link", "error");
                }

                identityLinkPort.revokeLink(platformSubject);
                log.info("Successfully revoked identity link for platform subject '{}'", platformSubject);
        }
}
