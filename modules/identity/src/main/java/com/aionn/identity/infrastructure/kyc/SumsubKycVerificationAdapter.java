package com.aionn.identity.infrastructure.kyc;

import com.aionn.identity.application.port.out.kyc.ExternalKycVerificationPort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.infrastructure.config.properties.KycProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "identity.kyc", name = "provider", havingValue = "sumsub")
public class SumsubKycVerificationAdapter implements ExternalKycVerificationPort {

    private static final String PROVIDER = "sumsub";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

    private final KycProperties kycProperties;
    private final ObjectMapper objectMapper;
    // Shared client with explicit timeouts so a stalled Sumsub upstream can't
    // block identity threads indefinitely; baseUrl comes from config each call.
    private final RestClient.Builder clientBuilder;
    private final Clock clock;

    public SumsubKycVerificationAdapter(KycProperties kycProperties, ObjectMapper objectMapper, Clock clock) {
        this.kycProperties = kycProperties;
        this.objectMapper = objectMapper;
        this.clock = clock;
        var settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(CONNECT_TIMEOUT)
                .withReadTimeout(READ_TIMEOUT);
        this.clientBuilder = RestClient.builder()
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings));
    }

    @Override
    public ExternalKycApplicant createApplicant(IdentityUser user, String kycId, String docType) {
        KycProperties.Sumsub config = requireConfig();
        String encodedLevel = urlEncode(config.levelName());
        String pathWithQuery = "/resources/applicants?levelName=" + encodedLevel;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("externalUserId", user.getUserId());
        body.put("type", "individual");
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            body.put("email", user.getEmail());
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            body.put("phone", user.getPhone());
        }

        JsonNode response = executeSignedJson("POST", pathWithQuery, body);
        String applicantId = text(response, "id");
        if (applicantId == null || applicantId.isBlank()) {
            applicantId = text(response, "applicantId");
        }
        if (applicantId == null || applicantId.isBlank()) {
            throw new IdentityException(IdentityErrorCode.KYC_PROVIDER_ERROR, "Sumsub applicant id is missing");
        }

        return new ExternalKycApplicant(
                PROVIDER,
                applicantId,
                config.levelName(),
                text(response, "reviewStatus"),
                text(response, "correlationId"));
    }

    @Override
    public ExternalKycSession generateVerificationSession(IdentityUser user, String kycId, String providerApplicantId) {
        KycProperties.Sumsub config = requireConfig();
        String path = "/resources/accessTokens/sdk";

        Map<String, Object> applicantIdentifiers = new LinkedHashMap<>();
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            applicantIdentifiers.put("email", user.getEmail());
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            applicantIdentifiers.put("phone", user.getPhone());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", user.getUserId());
        body.put("levelName", config.levelName());
        body.put("ttlInSecs", config.sdkTokenTtlSeconds());
        if (!applicantIdentifiers.isEmpty()) {
            body.put("applicantIdentifiers", applicantIdentifiers);
        }

        JsonNode response = executeSignedJson("POST", path, body);
        String token = text(response, "token");
        if (token == null || token.isBlank()) {
            throw new IdentityException(IdentityErrorCode.KYC_PROVIDER_ERROR, "Sumsub SDK token is missing");
        }

        return new ExternalKycSession(
                PROVIDER,
                providerApplicantId,
                config.levelName(),
                token,
                config.sdkTokenTtlSeconds(),
                config.sandbox());
    }

    @Override
    public void verifyWebhookSignature(byte[] payload, String digest, String digestAlgorithm) {
        KycProperties.Sumsub config = requireConfig();
        if (config.webhookSecret() == null || config.webhookSecret().isBlank()) {
            throw new IdentityException(IdentityErrorCode.KYC_PROVIDER_NOT_CONFIGURED,
                    "Sumsub webhook secret is not configured");
        }
        if (digest == null || digest.isBlank()) {
            throw new IdentityException(IdentityErrorCode.KYC_WEBHOOK_SIGNATURE_INVALID);
        }

        String algorithm = switch (digestAlgorithm == null ? "" : digestAlgorithm.trim().toUpperCase()) {
            case "", "HMAC_SHA256_HEX" -> "HmacSHA256";
            case "HMAC_SHA1_HEX" -> "HmacSHA1";
            case "HMAC_SHA512_HEX" -> "HmacSHA512";
            default -> throw new IdentityException(
                    IdentityErrorCode.KYC_WEBHOOK_SIGNATURE_INVALID,
                    "Unsupported Sumsub webhook digest algorithm: " + digestAlgorithm);
        };

        String calculated = hmacHex(algorithm, config.webhookSecret(), payload);
        // Constant-time comparison to avoid leaking the HMAC digest via timing.
        byte[] expected = calculated.toLowerCase().getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] actual = digest.toLowerCase().getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        if (!java.security.MessageDigest.isEqual(expected, actual)) {
            throw new IdentityException(IdentityErrorCode.KYC_WEBHOOK_SIGNATURE_INVALID);
        }
    }

    private JsonNode executeSignedJson(String method, String pathWithQuery, Map<String, Object> body) {
        KycProperties.Sumsub config = requireConfig();
        String bodyJson = toJson(body);
        String timestamp = String.valueOf(Instant.now(clock).getEpochSecond());
        String signature = sign(timestamp, method, pathWithQuery, bodyJson, config.secretKey());

        RestClient client = clientBuilder
                .clone()
                .baseUrl(config.baseUrl())
                .build();

        try {
            String response = client.method(org.springframework.http.HttpMethod.valueOf(method))
                    .uri(pathWithQuery)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-App-Token", config.appToken())
                    .header("X-App-Access-Ts", timestamp)
                    .header("X-App-Access-Sig", signature)
                    .body(bodyJson)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(response == null ? "{}" : response);
        } catch (Exception ex) {
            log.error("Sumsub request failed for path {}", pathWithQuery, ex);
            throw new IdentityException(IdentityErrorCode.KYC_PROVIDER_ERROR,
                    "Failed to call Sumsub for " + pathWithQuery);
        }
    }

    private KycProperties.Sumsub requireConfig() {
        KycProperties.Sumsub config = kycProperties.sumsub();
        if (config == null
                || isBlank(config.appToken())
                || isBlank(config.secretKey())
                || isBlank(config.levelName())) {
            throw new IdentityException(IdentityErrorCode.KYC_PROVIDER_NOT_CONFIGURED,
                    "Sumsub app token, secret key, or level name is missing");
        }
        return config;
    }

    private String sign(String timestamp, String method, String pathWithQuery, String bodyJson, String secretKey) {
        String payload = timestamp + method.toUpperCase() + pathWithQuery + bodyJson;
        return hmacHex("HmacSHA256", secretKey, payload.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacHex(String algorithm, String secretKey, byte[] payload) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), algorithm));
            byte[] hash = mac.doFinal(payload);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IdentityException(IdentityErrorCode.KYC_PROVIDER_ERROR, "Failed to sign Sumsub request");
        }
    }

    private String toJson(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception ex) {
            throw new IdentityException(IdentityErrorCode.KYC_PROVIDER_ERROR, "Failed to serialize Sumsub request");
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
