package com.aionn.sharedkernel.infrastructure.web.idempotency;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "security.idempotency")
public class IdempotencyProperties {

    private boolean enabled = true;
    private int processingTtlSeconds = 60;
}
