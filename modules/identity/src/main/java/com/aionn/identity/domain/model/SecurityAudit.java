package com.aionn.identity.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class SecurityAudit {

    private final String id;
    private final String userId;
    private final String eventType;
    private final String description;
    private final String ipAddress;
    private final String deviceId;
    private final LocalDateTime timestamp;
}
