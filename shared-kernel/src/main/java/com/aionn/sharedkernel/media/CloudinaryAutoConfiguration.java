package com.aionn.sharedkernel.media;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CloudinaryCredentialsProperties.class)
public class CloudinaryAutoConfiguration {
}
