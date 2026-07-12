package com.aionn.identity.infrastructure.kyc;

import com.aionn.identity.application.port.out.kyc.ExternalKycVerificationPort.ExternalKycApplicant;
import com.aionn.identity.application.port.out.kyc.ExternalKycVerificationPort.ExternalKycSession;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.domain.valueobject.KycProvider;
import com.aionn.identity.infrastructure.config.properties.KycProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SumsubKycVerificationAdapterTest {

    private static final String BASE_URL = "https://api.sumsub.com";
    private static final String APP_TOKEN = "app-token";
    private static final String SECRET_KEY = "secret-key";
    private static final String LEVEL_NAME = "basic-kyc-level";
    private static final String WEBHOOK_SECRET = "webhook-secret";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private KycProperties.Sumsub sumsub(String appToken, String secretKey, String levelName, String webhookSecret) {
        return new KycProperties.Sumsub(BASE_URL, appToken, secretKey, levelName, webhookSecret, 600, true);
    }

    private KycProperties kycProperties(KycProperties.Sumsub sumsub) {
        return new KycProperties(KycProvider.SUMSUB, sumsub, null);
    }

    private SumsubKycVerificationAdapter adapter(KycProperties properties) {
        return new SumsubKycVerificationAdapter(properties, objectMapper, java.time.Clock.systemUTC());
    }

    private MockRestServiceServer bindMockServer(SumsubKycVerificationAdapter adapter) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        setField(adapter, "clientBuilder", builder);
        return server;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private IdentityUser user() {
        return IdentityUser.createNew("user-1", "u@example.com", "0900000000", "username");
    }

    private static String hmacHex(String algorithm, String secret, byte[] payload) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            byte[] hash = mac.doFinal(payload);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    void createApplicantParsesIdAndReturnsApplicant() {
        SumsubKycVerificationAdapter adapter = adapter(
                kycProperties(sumsub(APP_TOKEN, SECRET_KEY, LEVEL_NAME, WEBHOOK_SECRET)));
        MockRestServiceServer server = bindMockServer(adapter);
        server.expect(requestTo(containsString("/resources/applicants")))
                .andRespond(withSuccess(
                        "{\"id\":\"appl-123\",\"reviewStatus\":\"init\",\"correlationId\":\"corr-1\"}",
                        MediaType.APPLICATION_JSON));

        ExternalKycApplicant applicant = adapter.createApplicant(user(), "kyc-1", "PASSPORT");

        assertThat(applicant.provider()).isEqualTo("sumsub");
        assertThat(applicant.applicantId()).isEqualTo("appl-123");
        assertThat(applicant.levelName()).isEqualTo(LEVEL_NAME);
        assertThat(applicant.reviewStatus()).isEqualTo("init");
        assertThat(applicant.correlationId()).isEqualTo("corr-1");
        server.verify();
    }

    @Test
    void createApplicantFallsBackToApplicantIdField() {
        SumsubKycVerificationAdapter adapter = adapter(
                kycProperties(sumsub(APP_TOKEN, SECRET_KEY, LEVEL_NAME, WEBHOOK_SECRET)));
        MockRestServiceServer server = bindMockServer(adapter);
        server.expect(requestTo(containsString("/resources/applicants")))
                .andRespond(withSuccess("{\"applicantId\":\"appl-456\"}", MediaType.APPLICATION_JSON));

        ExternalKycApplicant applicant = adapter.createApplicant(user(), "kyc-1", "PASSPORT");

        assertThat(applicant.applicantId()).isEqualTo("appl-456");
    }

    @Test
    void createApplicantThrowsWhenIdMissing() {
        SumsubKycVerificationAdapter adapter = adapter(
                kycProperties(sumsub(APP_TOKEN, SECRET_KEY, LEVEL_NAME, WEBHOOK_SECRET)));
        MockRestServiceServer server = bindMockServer(adapter);
        server.expect(requestTo(containsString("/resources/applicants")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.createApplicant(user(), "kyc-1", "PASSPORT"))
                .isInstanceOf(IdentityException.class);
    }

    @Test
    void createApplicantThrowsOnHttpError() {
        SumsubKycVerificationAdapter adapter = adapter(
                kycProperties(sumsub(APP_TOKEN, SECRET_KEY, LEVEL_NAME, WEBHOOK_SECRET)));
        MockRestServiceServer server = bindMockServer(adapter);
        server.expect(requestTo(containsString("/resources/applicants")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.createApplicant(user(), "kyc-1", "PASSPORT"))
                .isInstanceOf(IdentityException.class);
    }

    @Test
    void createApplicantThrowsWhenProviderNotConfigured() {
        SumsubKycVerificationAdapter adapter = adapter(kycProperties(null));
        bindMockServer(adapter);

        assertThatThrownBy(() -> adapter.createApplicant(user(), "kyc-1", "PASSPORT"))
                .isInstanceOf(IdentityException.class);
    }

    @Test
    void generateVerificationSessionParsesToken() {
        SumsubKycVerificationAdapter adapter = adapter(
                kycProperties(sumsub(APP_TOKEN, SECRET_KEY, LEVEL_NAME, WEBHOOK_SECRET)));
        MockRestServiceServer server = bindMockServer(adapter);
        server.expect(requestTo(containsString("/resources/accessTokens/sdk")))
                .andRespond(withSuccess("{\"token\":\"sdk-token\"}", MediaType.APPLICATION_JSON));

        ExternalKycSession session = adapter.generateVerificationSession(user(), "kyc-1", "appl-123");

        assertThat(session.provider()).isEqualTo("sumsub");
        assertThat(session.applicantId()).isEqualTo("appl-123");
        assertThat(session.accessToken()).isEqualTo("sdk-token");
        assertThat(session.levelName()).isEqualTo(LEVEL_NAME);
        assertThat(session.expiresInSeconds()).isEqualTo(600);
        assertThat(session.sandbox()).isTrue();
        server.verify();
    }

    @Test
    void generateVerificationSessionThrowsWhenTokenMissing() {
        SumsubKycVerificationAdapter adapter = adapter(
                kycProperties(sumsub(APP_TOKEN, SECRET_KEY, LEVEL_NAME, WEBHOOK_SECRET)));
        MockRestServiceServer server = bindMockServer(adapter);
        server.expect(requestTo(containsString("/resources/accessTokens/sdk")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.generateVerificationSession(user(), "kyc-1", "appl-123"))
                .isInstanceOf(IdentityException.class);
    }

    @Test
    void verifyWebhookSignatureAcceptsValidDefaultAlgorithmDigest() {
        SumsubKycVerificationAdapter adapter = adapter(
                kycProperties(sumsub(APP_TOKEN, SECRET_KEY, LEVEL_NAME, WEBHOOK_SECRET)));
        byte[] payload = "{\"event\":\"applicantReviewed\"}".getBytes(StandardCharsets.UTF_8);
        String digest = hmacHex("HmacSHA256", WEBHOOK_SECRET, payload);

        assertThatCode(() -> adapter.verifyWebhookSignature(payload, digest, null)).doesNotThrowAnyException();
    }

    @Test
    void verifyWebhookSignatureAcceptsExplicitSha256() {
        SumsubKycVerificationAdapter adapter = adapter(
                kycProperties(sumsub(APP_TOKEN, SECRET_KEY, LEVEL_NAME, WEBHOOK_SECRET)));
        byte[] payload = "payload-256".getBytes(StandardCharsets.UTF_8);
        String digest = hmacHex("HmacSHA256", WEBHOOK_SECRET, payload);

        assertThatCode(() -> adapter.verifyWebhookSignature(payload, digest, "HMAC_SHA256_HEX"))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyWebhookSignatureAcceptsSha1() {
        SumsubKycVerificationAdapter adapter = adapter(
                kycProperties(sumsub(APP_TOKEN, SECRET_KEY, LEVEL_NAME, WEBHOOK_SECRET)));
        byte[] payload = "payload-1".getBytes(StandardCharsets.UTF_8);
        String digest = hmacHex("HmacSHA1", WEBHOOK_SECRET, payload);

        assertThatCode(() -> adapter.verifyWebhookSignature(payload, digest, "HMAC_SHA1_HEX"))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyWebhookSignatureAcceptsSha512() {
        SumsubKycVerificationAdapter adapter = adapter(
                kycProperties(sumsub(APP_TOKEN, SECRET_KEY, LEVEL_NAME, WEBHOOK_SECRET)));
        byte[] payload = "payload-512".getBytes(StandardCharsets.UTF_8);
        String digest = hmacHex("HmacSHA512", WEBHOOK_SECRET, payload);

        assertThatCode(() -> adapter.verifyWebhookSignature(payload, digest, "HMAC_SHA512_HEX"))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyWebhookSignatureRejectsTamperedDigest() {
        SumsubKycVerificationAdapter adapter = adapter(
                kycProperties(sumsub(APP_TOKEN, SECRET_KEY, LEVEL_NAME, WEBHOOK_SECRET)));
        byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);
        String tampered = hmacHex("HmacSHA256", "wrong-secret", payload);

        assertThatThrownBy(() -> adapter.verifyWebhookSignature(payload, tampered, "HMAC_SHA256_HEX"))
                .isInstanceOf(IdentityException.class);
    }

    @Test
    void verifyWebhookSignatureRejectsUnsupportedAlgorithm() {
        SumsubKycVerificationAdapter adapter = adapter(
                kycProperties(sumsub(APP_TOKEN, SECRET_KEY, LEVEL_NAME, WEBHOOK_SECRET)));
        byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> adapter.verifyWebhookSignature(payload, "abc", "HMAC_MD5_HEX"))
                .isInstanceOf(IdentityException.class);
    }

    @Test
    void verifyWebhookSignatureRejectsBlankDigest() {
        SumsubKycVerificationAdapter adapter = adapter(
                kycProperties(sumsub(APP_TOKEN, SECRET_KEY, LEVEL_NAME, WEBHOOK_SECRET)));
        byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> adapter.verifyWebhookSignature(payload, "  ", "HMAC_SHA256_HEX"))
                .isInstanceOf(IdentityException.class);
    }

    @Test
    void verifyWebhookSignatureThrowsWhenWebhookSecretMissing() {
        SumsubKycVerificationAdapter adapter = adapter(kycProperties(sumsub(APP_TOKEN, SECRET_KEY, LEVEL_NAME, "")));
        byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> adapter.verifyWebhookSignature(payload, "abc", "HMAC_SHA256_HEX"))
                .isInstanceOf(IdentityException.class);
    }
}
