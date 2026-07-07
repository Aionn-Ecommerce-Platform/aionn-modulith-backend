package com.aionn.identity.infrastructure.persistence.adapter.security;

import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.SecurityAudit;
import com.aionn.identity.domain.valueobject.SecurityAuditEventType;
import com.aionn.identity.infrastructure.persistence.entity.SecurityAuditEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.mapper.SecurityAuditDomainMapper;
import com.aionn.identity.infrastructure.persistence.repository.security.SecurityAuditRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAuditAdapterTest {

    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityAuditRepository securityAuditRepository;
    @Mock
    private SecurityAuditDomainMapper securityAuditDomainMapper;

    @InjectMocks
    private SecurityAuditAdapter adapter;

    @Test
    void saveAuditLogPersistsEventFromEventType() {
        UserEntity user = UserEntity.builder().userId(USER_ID).build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        adapter.saveAuditLog(USER_ID, SecurityAuditEventType.MFA_ENABLED, "127.0.0.1");

        ArgumentCaptor<SecurityAuditEntity> captor = ArgumentCaptor.forClass(SecurityAuditEntity.class);
        verify(securityAuditRepository).save(captor.capture());
        SecurityAuditEntity saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo(SecurityAuditEventType.MFA_ENABLED.eventType());
        assertThat(saved.getDescription()).isEqualTo(SecurityAuditEventType.MFA_ENABLED.description());
        assertThat(saved.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(saved.getDeviceId()).isNull();
        assertThat(saved.getUser()).isSameAs(user);
    }

    @Test
    void saveAuditLogThrowsWhenUserMissing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.saveAuditLog(USER_ID, SecurityAuditEventType.MFA_ENABLED, "127.0.0.1"))
                .isInstanceOfSatisfying(IdentityException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.USER_NOT_FOUND.getCode()));
        verify(securityAuditRepository, never()).save(any());
    }

    @Test
    void getAuditLogsMapsResults() {
        SecurityAuditEntity entity = SecurityAuditEntity.builder().auditId("01HZAUD0000000000000000001").build();
        SecurityAudit domain = SecurityAudit.builder()
                .id("01HZAUD0000000000000000001")
                .userId(USER_ID)
                .eventType(SecurityAuditEventType.MFA_ENABLED)
                .build();
        when(securityAuditRepository.findTop100ByUser_UserIdOrderByTimestampDesc(USER_ID))
                .thenReturn(List.of(entity));
        when(securityAuditDomainMapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.getAuditLogs(USER_ID)).containsExactly(domain);
    }
}
