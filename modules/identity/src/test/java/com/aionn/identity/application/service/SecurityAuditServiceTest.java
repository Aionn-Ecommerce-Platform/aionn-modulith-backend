package com.aionn.identity.application.service;

import com.aionn.identity.application.port.out.security.SecurityAuditPort;
import com.aionn.identity.domain.model.SecurityAudit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

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
                .eventType("LOGIN")
                .build();
        when(securityAuditPort.getAuditLogs("user-1")).thenReturn(List.of(audit));

        List<SecurityAudit> result = securityAuditService.getAuditLogs("user-1");

        assertEquals(List.of(audit), result);
    }
}
