package com.aionn.ucp.infrastructure.schema;

import com.aionn.ucp.application.port.out.UcpSchemaValidationPort;
import com.aionn.ucp.infrastructure.config.UcpProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.resource.InputStreamSource;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PinnedUcpSchemaValidator implements UcpSchemaValidationPort {

    private static final String DEFAULT_PREFIX = "https://ucp.dev/schemas/";
    private static final String ERROR_RESPONSE_SCHEMA_URI = DEFAULT_PREFIX + "common/types/error_response.json";

    private final Set<String> allowedPrefixes;
    private final JsonSchemaFactory factory;
    private final Map<String, JsonSchema> schemaCache = new ConcurrentHashMap<>();

    public PinnedUcpSchemaValidator() {
        this(null);
    }

    @Autowired
    public PinnedUcpSchemaValidator(@Autowired(required = false) UcpProperties properties) {
        this.allowedPrefixes = (properties != null && properties.allowedSchemaPrefixes() != null
                && !properties.allowedSchemaPrefixes().isEmpty())
                        ? Set.copyOf(properties.allowedSchemaPrefixes())
                        : Set.of(DEFAULT_PREFIX);

        this.factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaLoaders(loaders -> loaders.add(iri -> loadSchema(iri.toString()))));
    }

    InputStreamSource loadSchema(String uri) {
        if (uri == null || uri.contains("..")) {
            throw new IllegalArgumentException("Schema URI contains invalid path elements: " + uri);
        }

        boolean matched = false;
        String matchedPrefix = null;
        for (String prefix : allowedPrefixes) {
            if (uri.startsWith(prefix)) {
                matched = true;
                matchedPrefix = prefix;
                break;
            }
        }

        if (!matched) {
            throw new IllegalArgumentException("Schema is not in the pinned contract registry: " + uri);
        }

        String subpath = uri.substring(matchedPrefix.length());
        int hashIdx = subpath.indexOf('#');
        if (hashIdx >= 0) {
            subpath = subpath.substring(0, hashIdx);
        }

        String mainResource = "/ucp/2026-08-25/" + subpath;
        InputStream is = getClass().getResourceAsStream(mainResource);
        String finalResource = mainResource;

        if (is == null) {
            String testResource = "/ucp-contract/2026-08-25/schemas/" + subpath;
            is = getClass().getResourceAsStream(testResource);
            finalResource = testResource;
        }

        if (is == null) {
            throw new IllegalArgumentException("Schema is not in the pinned contract registry: " + uri);
        }

        try {
            is.close();
        } catch (Exception ignored) {
            // Resource presence probe stream closed; ignore secondary close exception
        }

        final String path = finalResource;
        return () -> {
            InputStream stream = getClass().getResourceAsStream(path);
            if (stream == null) {
                throw new IllegalStateException("Missing pinned schema resource: " + path);
            }
            return stream;
        };
    }

    @Override
    public void validate(String schemaUri, JsonNode payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload cannot be null for schema validation");
        }

        JsonSchema jsonSchema = schemaCache.computeIfAbsent(schemaUri, uri -> factory.getSchema(URI.create(uri)));
        Set<ValidationMessage> errors = jsonSchema.validate(
                payload,
                context -> context.getExecutionConfig().setFormatAssertionsEnabled(true));

        if (!errors.isEmpty()) {
            throw new IllegalStateException(
                    "UCP payload failed validation against schema: " + schemaUri + ", errors: " + errors);
        }
    }

    @Override
    public void validateErrorResponse(JsonNode errorResponse) {
        validate(ERROR_RESPONSE_SCHEMA_URI, errorResponse);
    }
}
