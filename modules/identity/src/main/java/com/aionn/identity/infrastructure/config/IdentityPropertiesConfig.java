package com.aionn.identity.infrastructure.config;

import com.aionn.identity.infrastructure.config.properties.AccountManagementProperties;
import com.aionn.identity.infrastructure.config.properties.AddressProperties;
import com.aionn.identity.infrastructure.config.properties.AgentProperties;
import com.aionn.identity.infrastructure.config.properties.AuthCookieProperties;
import com.aionn.identity.infrastructure.config.properties.AuthProperties;
import com.aionn.identity.infrastructure.config.properties.AuthSessionProperties;
import com.aionn.identity.infrastructure.config.properties.CloudinaryProperties;
import com.aionn.identity.infrastructure.config.properties.JwtProperties;
import com.aionn.identity.infrastructure.config.properties.KycProperties;
import com.aionn.identity.infrastructure.config.properties.MfaProperties;
import com.aionn.identity.infrastructure.config.properties.RegistrationProperties;
import com.aionn.identity.infrastructure.config.properties.SocialAuthProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
                AccountManagementProperties.class,
                AddressProperties.class,
                AgentProperties.class,
                AuthCookieProperties.class,
                AuthProperties.class,
                AuthSessionProperties.class,
                CloudinaryProperties.class,
                JwtProperties.class,
                KycProperties.class,
                MfaProperties.class,
                RegistrationProperties.class,
                SocialAuthProperties.class
})
public class IdentityPropertiesConfig {
}
