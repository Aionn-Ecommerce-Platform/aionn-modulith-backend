package com.aionn.ucp.application.identity;

import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkRequest;
import com.aionn.ucp.adapter.rest.dto.identity.UcpIdentityModels.UcpIdentityLinkResponse;
import com.aionn.ucp.application.port.out.UcpIdentityLinkPort;
import com.aionn.ucp.domain.exception.UcpProtocolException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** UCP identity links: fail-closed ownership checks and composite platform keys. */
@Slf4j
@Service
@RequiredArgsConstructor
public class UcpIdentityApplicationService {

        private final UcpIdentityLinkPort identityLinkPort;

        /** Caller must own the customer, and an existing binding must not be hijacked. */
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

        /** Maps unauthorized access to 404 so subject existence isn't leaked. */
        public UcpIdentityLinkResponse getLink(String platformId, String platformSubject, String callerUserId) {
                return identityLinkPort.findByPlatformAndSubject(platformId, platformSubject)
                                .filter(l -> callerUserId != null && callerUserId.equals(l.customerId()))
                                .orElseThrow(() -> new UcpProtocolException(404, "link_not_found",
                                                "Identity link not found for platform: " + platformId + ", subject: "
                                                                + platformSubject,
                                                "error"));
        }

        public void revokeLink(String platformId, String platformSubject, String callerUserId) {
                identityLinkPort.findByPlatformAndSubject(platformId, platformSubject)
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
