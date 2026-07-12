package com.aionn.identity.application.service;


import com.aionn.identity.application.port.out.security.SecurityAuditPort;
import com.aionn.identity.domain.model.SecurityAudit;
import com.aionn.identity.domain.valueobject.SecurityAuditEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SecurityAuditServiceTest {

    @Mock
    private SecurityAuditPort securityAuditPort;

    private SecurityAuditService securityAuditService;

    @BeforeEach
    void setUp() {
        securityAuditService = new SecurityAuditService(securityAuditPort);
    }

    @Test
    void getAuditLogsDelegatesToPort() {
        var audit = SecurityAudit.builder()
                .id("01HZAUDIT0000000000000000")
                .userId("user-1")
                .eventType(SecurityAuditEventType.PASSWORD_CHANGED)
                .build();
        when(securityAuditPort.getAuditLogs("user-1")).thenReturn(List.of(audit));

        List<SecurityAudit> result = securityAuditService.getAuditLogs("user-1");

        assertThat(result).isEqualTo(List.of(audit));
    }
}
