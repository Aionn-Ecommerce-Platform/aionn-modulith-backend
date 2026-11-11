package com.aionn.ucp.infrastructure.security;

import com.aionn.ucp.application.port.out.UcpSigningPort;
import com.aionn.ucp.infrastructure.config.UcpProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.EdECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RFC 9421 Ed25519 HTTP Message Signatures plus the RFC 7517 public JWK the
 * discovery profile publishes so platforms can verify them.
 *
 * <p>Both halves of the key pair are configured explicitly: the JDK offers no
 * supported way to derive an Ed25519 public key from a PKCS#8 seed, and
 * hand-rolling the curve arithmetic here would be a security liability. Startup
 * verifies the pair by signing and verifying a probe message.</p>
 */
@Slf4j
@Component
public class Ed25519UcpSigningAdapter implements UcpSigningPort {

    private static final String ALG = "Ed25519";
    private static final String SIGNATURE_LABEL = "sig1";
    private static final int KEY_BYTES = 32;

    private final PrivateKey privateKey;
    private final String keyId;
    private final Clock clock;
    private final List<Map<String, Object>> publicJwks;

    public Ed25519UcpSigningAdapter(UcpProperties properties, Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
        String privatePem = properties == null ? null : properties.signaturePrivateKey();
        String publicPem = properties == null ? null : properties.signaturePublicKey();
        if (privatePem == null || privatePem.isBlank()) {
            if (publicPem != null && !publicPem.isBlank()) {
                throw new IllegalStateException("UCP signature-public-key set without signature-private-key");
            }
            this.privateKey = null;
            this.keyId = null;
            this.publicJwks = List.of();
            return;
        }
        try {
            this.privateKey = parsePrivateKey(privatePem);
            PublicKey publicKey = parsePublicKey(publicPem);
            verifyPair(privateKey, publicKey);
            Map<String, Object> jwk = toPublicJwk((EdECPublicKey) publicKey);
            this.keyId = (String) jwk.get("kid");
            this.publicJwks = List.of(jwk);
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid UCP Ed25519 signing key configuration", ex);
        }
    }

    @Override
    public List<Map<String, Object>> publicJwks() {
        return publicJwks;
    }

    @Override
    public Map<String, String> signRequest(String method, String authority, String path, byte[] body) {
        if (privateKey == null) {
            return Map.of();
        }
        try {
            byte[] payload = body == null ? new byte[0] : body;
            String contentDigest = "sha-256=:" + Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(payload)) + ":";
            String signatureParams = "(\"@method\" \"@authority\" \"@path\" \"content-digest\");"
                    + "created=" + clock.instant().getEpochSecond()
                    + ";keyid=\"" + keyId + "\";alg=\"ed25519\"";
            String signatureBase = "\"@method\": " + method.toLowerCase(Locale.ROOT) + "\n"
                    + "\"@authority\": " + authority + "\n"
                    + "\"@path\": " + path + "\n"
                    + "\"content-digest\": " + contentDigest + "\n"
                    + "\"@signature-params\": " + signatureParams;
            Signature signer = Signature.getInstance(ALG);
            signer.initSign(privateKey);
            signer.update(signatureBase.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getEncoder().encodeToString(signer.sign());

            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Content-Digest", contentDigest);
            headers.put("Signature-Input", SIGNATURE_LABEL + "=" + signatureParams);
            headers.put("Signature", SIGNATURE_LABEL + "=:" + signature + ":");
            return Map.copyOf(headers);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign UCP HTTP request", ex);
        }
    }

    private static PrivateKey parsePrivateKey(String pem) throws Exception {
        byte[] encoded = decodePem(pem);
        return KeyFactory.getInstance(ALG).generatePrivate(new PKCS8EncodedKeySpec(encoded));
    }

    private static PublicKey parsePublicKey(String pem) throws Exception {
        if (pem == null || pem.isBlank()) {
            throw new IllegalArgumentException("UCP signature-public-key is required when signing is enabled");
        }
        byte[] encoded = decodePem(pem);
        return KeyFactory.getInstance(ALG).generatePublic(new X509EncodedKeySpec(encoded));
    }

    private static byte[] decodePem(String pem) {
        String normalized = pem
                .replaceAll("-----BEGIN[^-]*-----", "")
                .replaceAll("-----END[^-]*-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }

    /** Fails fast when the configured halves are not a matching pair. */
    private static void verifyPair(PrivateKey privateKey, PublicKey publicKey) throws Exception {
        byte[] probe = "ucp-key-pair-probe".getBytes(StandardCharsets.UTF_8);
        Signature signer = Signature.getInstance(ALG);
        signer.initSign(privateKey);
        signer.update(probe);
        byte[] signature = signer.sign();
        Signature verifier = Signature.getInstance(ALG);
        verifier.initVerify(publicKey);
        verifier.update(probe);
        if (!verifier.verify(signature)) {
            throw new IllegalArgumentException("UCP signing private/public keys do not match");
        }
    }

    private static Map<String, Object> toPublicJwk(EdECPublicKey publicKey) throws Exception {
        byte[] x = toLittleEndian(publicKey.getPoint().getY(), KEY_BYTES);
        if (publicKey.getPoint().isXOdd()) {
            x[KEY_BYTES - 1] |= (byte) 0x80;
        }
        String xEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(x);
        // RFC 7638 thumbprint: required members only, lexicographic order.
        String thumbprintInput = "{\"crv\":\"Ed25519\",\"kty\":\"OKP\",\"x\":\"" + xEncoded + "\"}";
        String kid = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(thumbprintInput.getBytes(StandardCharsets.UTF_8)));

        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "OKP");
        jwk.put("crv", "Ed25519");
        jwk.put("x", xEncoded);
        jwk.put("use", "sig");
        jwk.put("alg", "EdDSA");
        jwk.put("kid", kid);
        return Map.copyOf(jwk);
    }

    private static byte[] toLittleEndian(BigInteger value, int size) {
        byte[] big = value.toByteArray();
        byte[] little = new byte[size];
        for (int i = 0; i < size; i++) {
            int source = big.length - 1 - i;
            little[i] = source >= 0 ? big[source] : 0;
        }
        return little;
    }
}
