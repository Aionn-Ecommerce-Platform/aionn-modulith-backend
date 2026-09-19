package com.aionn.ucp.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "aionn.ucp")
public record UcpProperties(
                @NotBlank @Pattern(regexp = "2026-08-25", message = "UCP version must be 2026-08-25") @DefaultValue("2026-08-25") String version,
                @NotBlank @DefaultValue("http://localhost:8080/ucp/v1") String restEndpoint,
                @NotBlank @DefaultValue("Aionn Commerce") String businessName,
                @NotEmpty @DefaultValue("https://ucp.dev/schemas/") Set<String> allowedSchemaPrefixes,
                @DefaultValue("false") boolean requireHttps,
                @NotNull @DefaultValue Capabilities capabilities) {

        public record Capabilities(
                        @DefaultValue("false") boolean cart,
                        @DefaultValue("false") boolean checkout,
                        @DefaultValue("false") boolean catalog,
                        @DefaultValue("false") boolean order,
                        @DefaultValue("false") boolean identityLinking) {

                public Capabilities(boolean cart, boolean checkout, boolean catalog, boolean order) {
                        this(cart, checkout, catalog, order, false);
                }
        }
}
