package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.ConsentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class UserConsent {

    private final String id;
    private final String userId;
    private final ConsentType consentType;
    private final String version;
    private final boolean granted;
    private final Instant agreedAt;
    private final Instant revokedAt;
    private final String ipAddress;

    public boolean isActive() {
        return granted && revokedAt == null;
    }
}
