package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.SecurityAuditEventType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAuditTest {

    @Test
    void builderUsesTypedAuditEvent() {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());

        SecurityAudit audit = SecurityAudit.builder()
                .id("audit-1")
                .userId("user-1")
                .eventType(SecurityAuditEventType.PASSWORD_CHANGED)
                .description("login ok")
                .ipAddress("127.0.0.1")
                .deviceId("device-1")
                .timestamp(now)
                .build();

        assertThat(audit.getEventType()).isEqualTo(SecurityAuditEventType.PASSWORD_CHANGED);
        assertThat(audit.getTimestamp()).isEqualTo(now);
    }
}
