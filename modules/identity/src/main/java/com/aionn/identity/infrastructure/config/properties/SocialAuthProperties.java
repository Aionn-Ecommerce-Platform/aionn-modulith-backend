package com.aionn.identity.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "identity.auth.social")
public record SocialAuthProperties(
        @DefaultValue Google google) {

    public record Google(
            @DefaultValue("remote") String provider,
            String clientId,
            @DefaultValue("https://oauth2.googleapis.com/tokeninfo") String tokenInfoUrl) {
        public boolean isRemote() {
            return "remote".equalsIgnoreCase(provider);
        }
    }
}
