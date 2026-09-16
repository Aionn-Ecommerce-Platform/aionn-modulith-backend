package com.aionn.ucp.infrastructure.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.aionn.ucp.application.port.out.BusinessProfileValidationPort;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.resource.InputStreamSource;
import java.net.URI;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PinnedBusinessProfileValidator implements BusinessProfileValidationPort {

    private static final String PREFIX = "https://ucp.dev/schemas/";
    private static final Set<String> RESOURCES = Set.of(
            "profile.json", "ucp.json", "service.json", "capability.json", "payment_handler.json",
            "common/types/available_payment_instrument.json", "common/types/constraint_expression.json",
            "common/types/request_constraints.json", "common/types/reverse_domain_name.json",
            "transports/embedded_config.json");

    private final JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012,
            builder -> builder.schemaLoaders(loaders -> loaders.add(
                    iri -> loadSchema(iri.toString()))))
            .getSchema(URI.create(PREFIX + "profile.json#/$defs/business_schema"));

    static InputStreamSource loadSchema(String uri) {
        if (!uri.startsWith(PREFIX) || !RESOURCES.contains(uri.substring(PREFIX.length()))) {
            // Throw rather than return null: null allows the library's network fallback.
            throw new IllegalArgumentException("Schema is not in the pinned discovery registry");
        }
        String resource = "/ucp/2026-08-25/" + uri.substring(PREFIX.length());
        return () -> {
            var stream = PinnedBusinessProfileValidator.class.getResourceAsStream(resource);
            if (stream == null) {
                throw new IllegalStateException("Missing pinned discovery schema resource");
            }
            return stream;
        };
    }

    @Override
    public void validate(JsonNode profile) {
        if (profile == null || !schema.validate(profile).isEmpty()) {
            // Validation messages may contain values; never put the profile in an exception.
            throw new IllegalStateException("UCP business profile does not match the pinned schema");
        }
    }
}
