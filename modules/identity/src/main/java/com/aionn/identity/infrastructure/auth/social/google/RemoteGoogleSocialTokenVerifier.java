package com.aionn.identity.infrastructure.auth.social.google;

import com.aionn.identity.application.port.out.social.SocialUserProfile;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.infrastructure.config.properties.SocialAuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;

@Slf4j
@Component
public class RemoteGoogleSocialTokenVerifier implements GoogleSocialTokenVerifier {

    private static final Set<String> VALID_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final SocialAuthProperties socialAuthProperties;
    private final RestClient restClient;
    private final Clock clock;

    public RemoteGoogleSocialTokenVerifier(SocialAuthProperties socialAuthProperties, Clock clock) {
        this.socialAuthProperties = socialAuthProperties;
        this.clock = clock;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
        factory.setReadTimeout((int) READ_TIMEOUT.toMillis());
        SocialAuthProperties.Google config = socialAuthProperties.google();
        String baseUrl = config != null ? config.tokenInfoUrl() : null;
        RestClient.Builder builder = RestClient.builder().requestFactory(factory);
        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
        }
        this.restClient = builder.build();
    }

    @Override
    public SocialUserProfile verify(String providerToken) {
        requireNotBlank(providerToken);

        SocialAuthProperties.Google config = socialAuthProperties.google();
        if (config == null || config.clientId() == null || config.clientId().isBlank()) {
            throw new IdentityException(IdentityErrorCode.PROVIDER_TOKEN_INVALID,
                    "Google client id is not configured");
        }

        try {
            GoogleTokenInfoResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.queryParam("id_token", providerToken).build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(GoogleTokenInfoResponse.class);

            if (response == null) {
                throw new IdentityException(IdentityErrorCode.PROVIDER_TOKEN_INVALID,
                        "Google tokeninfo returned no payload");
            }

            if (!config.clientId().equals(response.aud())) {
                throw new IdentityException(IdentityErrorCode.PROVIDER_TOKEN_INVALID,
                        "Google token audience mismatch");
            }
            if (!VALID_ISSUERS.contains(response.iss())) {
                throw new IdentityException(IdentityErrorCode.PROVIDER_TOKEN_INVALID,
                        "Google token issuer mismatch");
            }
            if (response.sub() == null || response.sub().isBlank()) {
                throw new IdentityException(IdentityErrorCode.PROVIDER_TOKEN_INVALID,
                        "Google token subject is missing");
            }
            if (response.exp() != null && response.exp() <= Instant.now(clock).getEpochSecond()) {
                throw new IdentityException(IdentityErrorCode.PROVIDER_TOKEN_INVALID,
                        "Google token has expired");
            }

            String email = response.email_verified() != null && response.email_verified() ? response.email() : null;
            String displayName = response.name();
            if (displayName == null || displayName.isBlank()) {
                String given = response.given_name();
                String family = response.family_name();
                if (given != null || family != null) {
                    displayName = ((given == null ? "" : given) + " " + (family == null ? "" : family)).trim();
                }
            }
            return new SocialUserProfile(response.sub(), email, displayName);
        } catch (IdentityException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to verify Google token", ex);
            throw new IdentityException(IdentityErrorCode.PROVIDER_TOKEN_INVALID,
                    "Google token verification failed");
        }
    }

    private record GoogleTokenInfoResponse(
            String aud,
            String iss,
            String sub,
            Long exp,
            String email,
            Boolean email_verified,
            String name,
            String given_name,
            String family_name,
            String picture) {
    }
}
