package com.aionn.ucp.infrastructure.security;

import com.aionn.ucp.infrastructure.config.UcpProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.NamedParameterSpec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Ed25519UcpSigningAdapterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-08T00:00:00Z"), ZoneOffset.UTC);
    private static final String VERSION = "2026-08-25";

    private static KeyPair pair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
        generator.initialize(NamedParameterSpec.ED25519);
        return generator.generateKeyPair();
    }

    private static UcpProperties properties(KeyPair pair) {
        return new UcpProperties(VERSION, "https://store.example/ucp/v1", "Store",
                Set.of("https://ucp.dev/schemas/"), true,
                new UcpProperties.Capabilities(true, true, true, true, true), "", "VND", "",
                Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()),
                Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
    }

    @Test
    void publishesPublicOnlyJwkAndProducesVerifiableRfc9421Headers() throws Exception {
        KeyPair pair = pair();
        Ed25519UcpSigningAdapter adapter = new Ed25519UcpSigningAdapter(properties(pair), CLOCK);
        byte[] body = "{\"event\":\"order.created\"}".getBytes(StandardCharsets.UTF_8);

        Map<String, String> headers = adapter.signRequest("POST", "hooks.example", "/events", body);
        assertThat(headers).containsKeys("Content-Digest", "Signature-Input", "Signature");
        assertThat(headers.get("Signature-Input")).contains("@method", "@authority", "@path", "content-digest");

        String signatureInput = headers.get("Signature-Input");
        String params = signatureInput.substring(signatureInput.indexOf('=') + 1);
        String signatureB64 = headers.get("Signature").substring(headers.get("Signature").indexOf(":") + 1,
                headers.get("Signature").lastIndexOf(":"));
        String digest = headers.get("Content-Digest");
        String created = params.replaceAll(".*created=(\\d+).*", "$1");
        String kid = params.replaceAll(".*keyid=\\\"([^\\\"]+)\\\".*", "$1");
        // The created parameter comes from the injected clock, not wall time.
        assertThat(created).isEqualTo(String.valueOf(CLOCK.instant().getEpochSecond()));
        String base = "\"@method\": post\n"
                + "\"@authority\": hooks.example\n"
                + "\"@path\": /events\n"
                + "\"content-digest\": " + digest + "\n"
                + "\"@signature-params\": " + params;
        Signature verifier = Signature.getInstance("Ed25519");
        verifier.initVerify(pair.getPublic());
        verifier.update(base.getBytes(StandardCharsets.UTF_8));
        assertThat(verifier.verify(Base64.getDecoder().decode(signatureB64))).isTrue();

        var keys = adapter.publicJwks();
        assertThat(keys).hasSize(1);
        assertThat(keys.get(0)).containsEntry("kty", "OKP").containsEntry("crv", "Ed25519")
                .containsEntry("alg", "EdDSA").containsEntry("kid", kid)
                .doesNotContainKeys("d", "private", "seed");
    }

    @Test
    void leavesSigningDisabledWhenNoPrivateKeyIsConfigured() {
        UcpProperties noKeys = new UcpProperties(VERSION, "https://store.example/ucp/v1", "Store",
                Set.of("https://ucp.dev/schemas/"), true,
                new UcpProperties.Capabilities(true, true, true, true, true), "", "VND", "", "", "");
        Ed25519UcpSigningAdapter adapter = new Ed25519UcpSigningAdapter(noKeys, CLOCK);

        assertThat(adapter.publicJwks()).isEmpty();
        assertThat(adapter.signRequest("POST", "hooks.example", "/events", new byte[0])).isEmpty();
    }

    @Test
    void rejectsMismatchedSigningKeyPair() throws Exception {
        KeyPair signing = pair();
        KeyPair differentPublic = pair();
        UcpProperties invalid = new UcpProperties(VERSION, "https://store.example/ucp/v1", "Store",
                Set.of("https://ucp.dev/schemas/"), true,
                new UcpProperties.Capabilities(true, true, true, true, true), "", "VND", "",
                Base64.getEncoder().encodeToString(signing.getPrivate().getEncoded()),
                Base64.getEncoder().encodeToString(differentPublic.getPublic().getEncoded()));

        assertThatThrownBy(() -> new Ed25519UcpSigningAdapter(invalid, CLOCK))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("key configuration");
    }
}
