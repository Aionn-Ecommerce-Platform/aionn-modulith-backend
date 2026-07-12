package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.SecurityAuditEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class SecurityAudit {

    private final String id;
    private final String userId;
    private final SecurityAuditEventType eventType;
    private final String description;
    private final String ipAddress;
    private final String deviceId;
    private final Instant timestamp;
}
