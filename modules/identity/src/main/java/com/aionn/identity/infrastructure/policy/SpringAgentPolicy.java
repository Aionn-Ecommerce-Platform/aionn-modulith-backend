package com.aionn.identity.infrastructure.policy;

import com.aionn.identity.application.policy.AgentPolicy;
import com.aionn.identity.infrastructure.config.properties.AgentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringAgentPolicy implements AgentPolicy {

    private final AgentProperties properties;

    @Override
    public int getKeyExpiryYears() {
        return properties.keyExpiryYears();
    }
}
