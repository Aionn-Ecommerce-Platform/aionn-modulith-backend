package com.aionn.ucp.infrastructure.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PinnedUcpContractBundleTest {

    private static final String SCHEMA_BASE_URI = "https://ucp.dev/schemas/";
    private static final String ROOT = "/ucp-contract/2026-08-25/";
    private static final String COMMIT = "cd78fb38e819de77d9b527d110476eccb876f1bd";
    private static final Set<String> CONTRACT_ROOTS = Set.of(
            "schemas/shopping/cart.json",
            "schemas/shopping/checkout.json",
            "schemas/shopping/catalog_lookup.json",
            "schemas/shopping/catalog_search.json",
            "schemas/shopping/order.json",
            "schemas/common/identity_linking.json",
            "schemas/shopping/buyer_consent.json",
            "schemas/shopping/discount.json",
            "schemas/shopping/fulfillment.json");

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void recordsTheApprovedReleaseBaselineAndOfflineScope() throws Exception {
        JsonNode manifest = json("manifest.json");
        JsonNode provenance = json("provenance.json");

        assertThat(manifest.path("protocol_version").asText()).isEqualTo("2026-08-25");
        assertThat(manifest.path("source_tag").asText()).isEqualTo("v2026-08-25");
        assertThat(manifest.path("source_commit").asText()).isEqualTo(COMMIT);
        assertThat(provenance.path("source_commit").asText()).isEqualTo(COMMIT);
        assertThat(manifest.path("scope").asText())
                .contains("not registered with the discovery runtime validator")
                .contains("do not advertise or implement");
    }

    @Test
    void verifiesManifestHashesAndJsonParseability() throws Exception {
        JsonNode manifest = json("manifest.json");
        assertThat(manifest.path("files")).isNotEmpty();

        for (JsonNode entry : manifest.path("files")) {
            String path = entry.path("path").asText();
            byte[] content = resource(path);
            assertThat(sha256(content)).as(path).isEqualTo(entry.path("sha256").asText());
            if (path.endsWith(".json")) {
                assertThatCode(() -> mapper.readTree(content)).as(path).doesNotThrowAnyException();
            }
        }
    }

    @Test
    void recordsEveryDeclaredContractRootAndRestDescription() throws Exception {
        JsonNode manifest = json("manifest.json");
        Set<String> listed = new HashSet<>();
        manifest.path("files").forEach(entry -> listed.add(entry.path("path").asText()));
        Set<String> roots = new HashSet<>();
        manifest.path("contract_roots").forEach(value -> roots.add(value.asText()));

        assertThat(roots).isEqualTo(CONTRACT_ROOTS);
        assertThat(listed).containsAll(CONTRACT_ROOTS)
                .contains("services/common/rest.openapi.json", "services/shopping/rest.openapi.json", "LICENSE");
    }

    @Test
    void mapsCanonicalAndRelativeReferencesToTheSameBundlePath() {
        String source = "schemas/shopping/cart.json";
        assertThat(resolveSchemaReference(source, "https://ucp.dev/schemas/shopping/cart.json"))
                .isEqualTo(source);
        assertThat(resolveSchemaReference(source, "cart.json"))
                .isEqualTo(source);
        assertThat(resolveSchemaReference(source, "#/$defs/example"))
                .isEqualTo(source);
    }

    @Test
    void resolvesAllSchemaReferencesWithinThePinnedBundle() throws Exception {
        JsonNode manifest = json("manifest.json");
        Set<String> files = new HashSet<>();
        manifest.path("files").forEach(entry -> files.add(entry.path("path").asText()));
        Set<String> visited = new HashSet<>();
        ArrayDeque<String> pending = new ArrayDeque<>(CONTRACT_ROOTS);

        while (!pending.isEmpty()) {
            String path = pending.removeFirst();
            if (!visited.add(path)) {
                continue;
            }
            JsonNode document = mapper.readTree(resource(path));
            collectSchemaReferences(document, path, files, pending);
        }
        assertThat(visited).containsAll(CONTRACT_ROOTS);
    }

    private void collectSchemaReferences(JsonNode node, String source, Set<String> files, ArrayDeque<String> pending) {
        if (node.isObject()) {
            node.properties().forEach(entry -> {
                if ("$ref".equals(entry.getKey()) && entry.getValue().isTextual()) {
                    String reference = entry.getValue().asText();
                    String resolved = resolveSchemaReference(source, reference);
                    assertThat(resolved).as("reference %s from %s", reference, source).isNotBlank();
                    assertThat(files).as("reference %s from %s", reference, source).contains(resolved);
                    try {
                        assertFragmentExists(resolved, reference);
                    } catch (IOException exception) {
                        throw new IllegalStateException("cannot read reference target " + resolved, exception);
                    }
                    pending.add(resolved);
                }
                if (!isInstanceDataKeyword(entry.getKey())) {
                    collectSchemaReferences(entry.getValue(), source, files, pending);
                }
            });
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                collectSchemaReferences(child, source, files, pending);
            }
        }
    }

    private boolean isInstanceDataKeyword(String keyword) {
        return Set.of("examples", "example", "default", "const").contains(keyword);
    }

    private String resolveSchemaReference(String source, String reference) {
        try {
            URI referenceUri = URI.create(reference);
            URI documentUri;
            if (referenceUri.isAbsolute()) {
                if (!SCHEMA_BASE_URI.equals(referenceUri.resolve(".").toString())
                        && !reference.startsWith(SCHEMA_BASE_URI)) {
                    throw new IllegalArgumentException("reference is outside the canonical pinned schema authority: " + reference);
                }
                documentUri = referenceUri;
            } else {
                documentUri = URI.create(SCHEMA_BASE_URI + source.substring("schemas/".length())).resolve(referenceUri);
            }
            if (!"https".equals(documentUri.getScheme()) || !"ucp.dev".equals(documentUri.getHost())
                    || !documentUri.getPath().startsWith("/schemas/")) {
                throw new IllegalArgumentException("reference does not resolve within canonical pinned schemas: " + reference);
            }
            String path = documentUri.getPath().substring("/schemas/".length());
            return "schemas/" + URI.create(path).normalize().getPath();
        } catch (IllegalArgumentException exception) {
            throw new AssertionError("invalid schema reference " + reference + " from " + source, exception);
        }
    }

    private void assertFragmentExists(String resolved, String reference) throws IOException {
        int fragmentIndex = reference.indexOf('#');
        if (fragmentIndex < 0 || fragmentIndex == reference.length() - 1) {
            return;
        }
        JsonNode target = mapper.readTree(resource(resolved));
        String fragment = reference.substring(fragmentIndex + 1);
        if (fragment.startsWith("/")) {
            assertThat(target.at("/" + fragment.substring(1)).isMissingNode())
                    .as("fragment %s in %s", fragment, resolved)
                    .isFalse();
            return;
        }
        assertThat(fragment).as("unsupported fragment %s", reference).isEqualTo("");
    }

    private JsonNode json(String path) throws IOException {
        return mapper.readTree(resource(path));
    }

    private byte[] resource(String path) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(ROOT + path)) {
            assertThat(stream).as("missing pinned contract resource %s", path).isNotNull();
            return stream.readAllBytes();
        }
    }

    private String sha256(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            StringBuilder hex = new StringBuilder();
            for (byte value : digest) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }
}
