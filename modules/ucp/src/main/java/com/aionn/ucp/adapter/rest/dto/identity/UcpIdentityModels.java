package com.aionn.ucp.adapter.rest.dto.identity;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Canonical models for UCP Identity Linking (dev.ucp.common.identity_linking). */
public final class UcpIdentityModels {

        private UcpIdentityModels() {
        }

        public record UcpIdentityLinkRequest(
                        @NotBlank(message = "platformId must not be blank") String platformId,

                        @NotBlank(message = "platformSubject must not be blank") String platformSubject,

                        @NotBlank(message = "customerId must not be blank") String customerId,

                        List<String> scopes,

                        Map<String, Object> metadata) {
        }

        public record UcpIdentityLinkResponse(
                        String linkId,
                        String platformId,
                        String platformSubject,
                        String customerId,
                        String status,
                        List<String> scopes,
                        Instant linkedAt,
                        Instant expiresAt,
                        Map<String, Object> metadata) {
        }
}
