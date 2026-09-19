package com.aionn.ucp.adapter.rest.dto.identity;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Canonical data models for UCP Identity Linking capability
 * (dev.ucp.common.identity_linking).
 */
public final class UcpIdentityModels {

        private UcpIdentityModels() {
        }

        /**
         * Request payload to link an external platform identity to an Aionn merchant
         * customer.
         *
         * @param platformId      Identifier of the calling platform/agent (e.g.
         *                        google_assistant, gemini_agent)
         * @param platformSubject The user identifier within the external platform
         *                        domain
         * @param customerId      The Aionn merchant customer account ID
         * @param scopes          OAuth-compatible permission scopes granted to the
         *                        platform
         * @param metadata        Additional context or platform telemetry
         */
        public record UcpIdentityLinkRequest(
                        @NotBlank(message = "platformId must not be blank") String platformId,

                        @NotBlank(message = "platformSubject must not be blank") String platformSubject,

                        @NotBlank(message = "customerId must not be blank") String customerId,

                        List<String> scopes,

                        Map<String, Object> metadata) {
        }

        /**
         * Response payload representing the active or revoked state of an identity
         * link.
         *
         * @param linkId          Unique internal identifier of this link
         * @param platformId      Identifier of the calling platform/agent
         * @param platformSubject The user identifier within the external platform
         *                        domain
         * @param customerId      The Aionn merchant customer account ID
         * @param status          Status of the link ("ACTIVE", "REVOKED")
         * @param scopes          Authorized permission scopes
         * @param linkedAt        Timestamp when the link was established
         * @param expiresAt       Optional expiration timestamp
         * @param metadata        Associated metadata
         */
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
