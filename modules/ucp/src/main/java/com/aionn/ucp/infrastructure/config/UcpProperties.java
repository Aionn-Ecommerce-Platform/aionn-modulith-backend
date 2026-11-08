package com.aionn.ucp.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "aionn.ucp")
public record UcpProperties(
                @NotBlank @Pattern(regexp = "2026-08-25", message = "UCP version must be 2026-08-25") @DefaultValue("2026-08-25") String version,
                // No default: unset means capabilities aren't advertised rather than pointing at localhost.
                String restEndpoint,
                @NotBlank @DefaultValue("Aionn Commerce") String businessName,
                @NotEmpty @DefaultValue("https://ucp.dev/schemas/") Set<String> allowedSchemaPrefixes,
                @DefaultValue("true") boolean requireHttps,
                @NotNull @DefaultValue Capabilities capabilities,
                @DefaultValue("") String platformApiKeys,
                // Must match the currency catalog pricing emits; the store is VND-priced.
                @NotBlank @DefaultValue("VND") String marketCurrency,
                // Empty disables webhook signing (X-UCP-Signature / X-UCP-Timestamp); dev only.
                @DefaultValue("") String webhookSecret) {

        public record Capabilities(
                        @DefaultValue("false") boolean cart,
                        @DefaultValue("false") boolean checkout,
                        @DefaultValue("false") boolean catalog,
                        @DefaultValue("false") boolean order,
                        @DefaultValue("false") boolean identityLinking) {
        }

        /** Parses {@code "platA=secretA,platB=secretB"} into apiKey → platformId; empty disables auth. */
        public Map<String, String> parsePlatformApiKeys() {
                if (platformApiKeys == null || platformApiKeys.isBlank()) {
                        return Map.of();
                }
                Map<String, String> map = new LinkedHashMap<>();
                for (String pair : platformApiKeys.split(",")) {
                        int eq = pair.indexOf('=');
                        if (eq <= 0) {
                                continue;
                        }
                        String platformId = pair.substring(0, eq).trim();
                        String key = pair.substring(eq + 1).trim();
                        if (!platformId.isEmpty() && !key.isEmpty()) {
                                map.put(key, platformId);
                        }
                }
                return map;
        }
}
